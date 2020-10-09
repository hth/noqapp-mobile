package com.noqapp.mobile.service;

import static com.noqapp.common.utils.AbstractDomain.ISO8601_FMT;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreCategoryEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.annotation.Mobile;
import com.noqapp.domain.json.JsonHour;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonStore;
import com.noqapp.domain.json.JsonStoreCategory;
import com.noqapp.domain.json.JsonStoreProduct;
import com.noqapp.domain.json.JsonStoreProductList;
import com.noqapp.domain.types.medical.PharmacyCategoryEnum;
import com.noqapp.service.BizService;
import com.noqapp.service.StoreCategoryService;
import com.noqapp.service.StoreProductService;

import org.apache.commons.lang3.time.DateFormatUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * hitender
 * 6/4/18 5:38 PM
 */
@Service
public class StoreDetailService {

    private BizService bizService;
    private TokenQueueMobileService tokenQueueMobileService;
    private StoreProductService storeProductService;
    private StoreCategoryService storeCategoryService;

    @Autowired
    public StoreDetailService(
        BizService bizService,
        TokenQueueMobileService tokenQueueMobileService,
        StoreProductService storeProductService,
        StoreCategoryService storeCategoryService
    ) {
        this.bizService = bizService;
        this.tokenQueueMobileService = tokenQueueMobileService;
        this.storeProductService = storeProductService;
        this.storeCategoryService = storeCategoryService;
    }

    public String populateStoreDetail(String codeQR) {
        return storeDetail(codeQR).asJson();
    }

    public JsonStoreProductList displayCaseStoreProducts(String codeQR) {
        String bizNameId = bizService.findByCodeQR(codeQR).getBizName().getId();
        List<BizStoreEntity> bizStores = bizService.getAllBizStores(bizNameId);

        JsonStoreProductList jsonStoreProductList = new JsonStoreProductList();
        for (BizStoreEntity bizStore : bizStores) {
            List<StoreProductEntity> storeProducts = storeProductService.findAllDisplayCase(bizStore.getId());
            for (StoreProductEntity storeProduct : storeProducts) {
                JsonStoreProduct jsonStoreProduct = getJsonStoreProduct(storeProduct);
                jsonStoreProductList.addJsonStoreProduct(jsonStoreProduct);
            }
        }

        return jsonStoreProductList;
    }

    public JsonStoreProduct getJsonStoreProduct(StoreProductEntity storeProduct) {
        return new JsonStoreProduct()
            .setProductId(storeProduct.getId())
            .setBarCode(storeProduct.getBarCode())
            .setProductName(storeProduct.getProductName())
            .setProductPrice(storeProduct.getProductPrice())
            .setTax(storeProduct.getTax())
            .setProductDiscount(storeProduct.getProductDiscount())
            .setProductInfo(storeProduct.getProductInfo())
            .setProductImage(null == storeProduct.getProductImage() ? null : storeProduct.getProductImage())
            .setStoreCategoryId(storeProduct.getStoreCategoryId())
            .setProductType(storeProduct.getProductType())
            .setUnitValue(storeProduct.getUnitValue())
            .setPackageSize(storeProduct.getPackageSize())
            .setInventoryCurrent(storeProduct.getInventoryCurrent())
            .setInventoryLimit(storeProduct.getInventoryLimit())
            .setUnitOfMeasurement(storeProduct.getUnitOfMeasurement())
            .setProductReference(storeProduct.getProductReference())
            .setAvailableDate(
                storeProduct.getAvailableDate() == null
                    ? DateFormatUtils.format(new Date(), ISO8601_FMT, TimeZone.getTimeZone("UTC"))
                    : DateFormatUtils.format(storeProduct.getAvailableDate(), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
            .setAvailableNow(storeProduct.getAvailableDate() == null || Instant.now().isAfter(storeProduct.getAvailableDate().toInstant()))
            .setDisplayCaseTurnedOn(storeProduct.isDisplayCaseTurnedOn())
            .setBizStoreId(storeProduct.getBizStoreId())
            .setActive(storeProduct.isActive());
    }

    @Mobile
    public JsonStore storeDetail(String codeQR) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        JsonStore jsonStore = new JsonStore();

        JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(codeQR);
        jsonStore.setJsonQueue(jsonQueue);

        List<StoreProductEntity> storeProducts = storeProductService.findAll(bizStore.getId());
        for (StoreProductEntity storeProduct : storeProducts) {
            JsonStoreProduct jsonStoreProduct = getJsonStoreProduct(storeProduct);
            jsonStore.addJsonStoreProduct(jsonStoreProduct);
        }

        switch (bizStore.getBusinessType()) {
            case PH:
                Map<String, String> map = PharmacyCategoryEnum.asMapWithNameAsKey();
                for (String key : map.keySet()) {
                    JsonStoreCategory jsonStoreCategory = new JsonStoreCategory()
                        .setCategoryId(key)
                        .setCategoryName(map.get(key));
                    jsonStore.addJsonStoreCategory(jsonStoreCategory);
                }
                break;
            default:
                List<StoreCategoryEntity> storeCategories = storeCategoryService.findAll(bizStore.getId());
                for (StoreCategoryEntity storeCategory : storeCategories) {
                    JsonStoreCategory jsonStoreCategory = new JsonStoreCategory()
                        .setCategoryId(storeCategory.getId())
                        .setCategoryName(storeCategory.getCategoryName());
                    jsonStore.addJsonStoreCategory(jsonStoreCategory);
                }
        }

        List<JsonHour> jsonHours = bizService.findAllStoreHoursAsJson(bizStore.getId());
        jsonStore.setJsonHours(jsonHours);
        return jsonStore;
    }
}
