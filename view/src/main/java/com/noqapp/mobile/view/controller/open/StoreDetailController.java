package com.noqapp.mobile.view.controller.open;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreCategoryEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonStore;
import com.noqapp.domain.json.JsonStoreCategory;
import com.noqapp.domain.json.JsonStoreProduct;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.service.TokenQueueMobileService;
import com.noqapp.search.elastic.domain.BizStoreElasticList;
import com.noqapp.service.BizService;
import com.noqapp.service.StoreCategoryService;
import com.noqapp.service.StoreProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * hitender
 * 3/23/18 1:46 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/open/store")
public class StoreDetailController {
    private static final Logger LOG = LoggerFactory.getLogger(StoreDetailController.class);

    private BizService bizService;
    private TokenQueueMobileService tokenQueueMobileService;
    private StoreProductService storeProductService;
    private StoreCategoryService storeCategoryService;
    private ApiHealthService apiHealthService;

    @Autowired
    public StoreDetailController(
            BizService bizService,
            TokenQueueMobileService tokenQueueMobileService,
            StoreProductService storeProductService,
            StoreCategoryService storeCategoryService,
            ApiHealthService apiHealthService
    ) {
        this.bizService = bizService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.storeProductService = storeProductService;
        this.storeCategoryService = storeCategoryService;
        this.apiHealthService = apiHealthService;
    }

    @GetMapping(
            value = "/{codeQR}",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public String getStoreDetail(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @PathVariable("codeQR")
            ScrubbedInput codeQR,

            HttpServletResponse response
    ) {
        Instant start = Instant.now();
        LOG.info("Store Detail for codeQR={} did={} dt={}", codeQR, did, dt);

        try {
            BizStoreEntity bizStore = bizService.findByCodeQR(codeQR.getText());
            JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(codeQR.getText());
            List<StoreProductEntity> storeProducts = storeProductService.findAll(bizStore.getId());
            List<StoreCategoryEntity> storeCategories = storeCategoryService.findAll(bizStore.getId());

            JsonStore jsonStore = new JsonStore();
            jsonStore.setJsonQueue(jsonQueue);
            for (StoreProductEntity storeProduct : storeProducts) {
                JsonStoreProduct jsonStoreProduct = new JsonStoreProduct()
                        .setProductName(storeProduct.getProductName())
                        .setProductPrice(storeProduct.getProductPrice())
                        .setProductDescription(storeProduct.getProductDescription())
                        .setStoreCategoryId(storeProduct.getStoreCategoryId())
                        .setProductFresh(storeProduct.isProductFresh())
                        .setProductReference(storeProduct.getProductReference());
                jsonStore.addJsonStoreProduct(jsonStoreProduct);
            }

            for (StoreCategoryEntity storeCategory : storeCategories) {
                JsonStoreCategory jsonStoreCategory = new JsonStoreCategory()
                        .setCategoryId(storeCategory.getId())
                        .setCategoryName(storeCategory.getCategoryName());
                jsonStore.addJsonStoreCategory(jsonStoreCategory);
            }

            return jsonStore.asJson();
        } catch (Exception e) {
            LOG.error("Failed processing search reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/search",
                    "search",
                    SearchBusinessStoreController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return new BizStoreElasticList().asJson();
        } finally {
            apiHealthService.insert(
                    "/search",
                    "search",
                    SearchBusinessStoreController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
}
