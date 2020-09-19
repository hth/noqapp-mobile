package com.noqapp.mobile.service;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreCategoryEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.annotation.Mobile;
import com.noqapp.domain.json.JsonHour;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonStore;
import com.noqapp.domain.json.JsonStoreCategory;
import com.noqapp.domain.json.JsonStoreProduct;
import com.noqapp.domain.types.medical.PharmacyCategoryEnum;
import com.noqapp.service.BizService;
import com.noqapp.service.StoreCategoryService;
import com.noqapp.service.StoreProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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

    @Mobile
    public JsonStore storeDetail(String codeQR) {
        BizStoreEntity bizStore = bizService.findByCodeQR(codeQR);
        JsonStore jsonStore = new JsonStore();

        JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(codeQR);
        jsonStore.setJsonQueue(jsonQueue);

        List<StoreProductEntity> storeProducts = storeProductService.findAll(bizStore.getId());
        for (StoreProductEntity storeProduct : storeProducts) {
            JsonStoreProduct jsonStoreProduct = new JsonStoreProduct()
                .setProductId(storeProduct.getId())
                .setProductName(storeProduct.getProductName())
                .setProductPrice(storeProduct.getProductPrice())
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
                .setActive(storeProduct.isActive());
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
