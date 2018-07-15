package com.noqapp.mobile.service;

import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StoreCategoryEntity;
import com.noqapp.domain.StoreHourEntity;
import com.noqapp.domain.StoreProductEntity;
import com.noqapp.domain.annotation.Mobile;
import com.noqapp.domain.json.JsonHour;
import com.noqapp.domain.json.JsonQueue;
import com.noqapp.domain.json.JsonStore;
import com.noqapp.domain.json.JsonStoreCategory;
import com.noqapp.domain.json.JsonStoreProduct;
import com.noqapp.service.BizService;
import com.noqapp.service.StoreCategoryService;
import com.noqapp.service.StoreProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        JsonQueue jsonQueue = tokenQueueMobileService.findTokenState(codeQR);
        List<StoreProductEntity> storeProducts = storeProductService.findAll(bizStore.getId());
        List<StoreCategoryEntity> storeCategories = storeCategoryService.findAll(bizStore.getId());

        JsonStore jsonStore = new JsonStore();
        jsonStore.setJsonQueue(jsonQueue);
        for (StoreProductEntity storeProduct : storeProducts) {
            JsonStoreProduct jsonStoreProduct = new JsonStoreProduct()
                    .setProductId(storeProduct.getId())
                    .setProductName(storeProduct.getProductName())
                    .setProductPrice(storeProduct.getProductPrice())
                    .setProductDiscount(storeProduct.getProductDiscount())
                    .setProductInfo(storeProduct.getProductInfo())
                    .setStoreCategoryId(storeProduct.getStoreCategoryId())
                    .setProductType(storeProduct.getProductType())
                    .setUnitOfMeasurement(storeProduct.getUnitOfMeasurement())
                    .setProductReference(storeProduct.getProductReference());
            jsonStore.addJsonStoreProduct(jsonStoreProduct);
        }

        for (StoreCategoryEntity storeCategory : storeCategories) {
            JsonStoreCategory jsonStoreCategory = new JsonStoreCategory()
                    .setCategoryId(storeCategory.getId())
                    .setCategoryName(storeCategory.getCategoryName());
            jsonStore.addJsonStoreCategory(jsonStoreCategory);
        }

        List<StoreHourEntity> storeHours = bizService.findAllStoreHours(bizStore.getId());
        for (StoreHourEntity storeHour : storeHours) {
            JsonHour jsonHour = new JsonHour()
                    .setDayOfWeek(storeHour.getDayOfWeek())
                    .setTokenAvailableFrom(storeHour.getTokenAvailableFrom())
                    .setTokenNotAvailableFrom(storeHour.getTokenNotAvailableFrom())
                    .setStartHour(storeHour.getStartHour())
                    .setEndHour(storeHour.getEndHour())
                    .setPreventJoining(storeHour.isPreventJoining())
                    .setDayClosed(storeHour.isDayClosed())
                    .setDelayedInMinutes(storeHour.getDelayedInMinutes());
            jsonStore.addJsonHour(jsonHour);
        }

        return jsonStore;
    }
}
