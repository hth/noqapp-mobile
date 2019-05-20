package com.noqapp.mobile.service;

import static com.noqapp.common.utils.AbstractDomain.ISO8601_FMT;
import static com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE.TV;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StatsVigyaapanStoreDailyEntity;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.tv.JsonAdvertisement;
import com.noqapp.domain.json.tv.JsonAdvertisementList;
import com.noqapp.domain.types.AdvertisementTypeEnum;
import com.noqapp.medical.domain.MedicalRecordEntity;
import com.noqapp.medical.repository.MedicalRecordManager;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.repository.StatsVigyaapanStoreDailyManager;
import com.noqapp.service.AdvertisementService;
import com.noqapp.service.ProfessionalProfileService;

import org.apache.commons.lang3.time.DateFormatUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

/**
 * hitender
 * 2018-12-20 10:15
 */
@Service
public class AdvertisementMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(AdvertisementMobileService.class);

    private BizStoreManager bizStoreManager;
    private StatsVigyaapanStoreDailyManager statsVigyaapanStoreDailyManager;
    private ProfessionalProfileService professionalProfileService;
    private MedicalRecordManager medicalRecordManager;
    private AdvertisementService advertisementService;

    @Autowired
    public AdvertisementMobileService(
        BizStoreManager bizStoreManager,
        StatsVigyaapanStoreDailyManager statsVigyaapanStoreDailyManager,
        ProfessionalProfileService professionalProfileService,
        MedicalRecordManager medicalRecordManager,
        AdvertisementService advertisementService
    ) {
        this.statsVigyaapanStoreDailyManager = statsVigyaapanStoreDailyManager;
        this.bizStoreManager = bizStoreManager;
        this.professionalProfileService = professionalProfileService;
        this.medicalRecordManager = medicalRecordManager;
        this.advertisementService = advertisementService;
    }

    @Async
    public void tagStoreAsDisplayed(String codeQR) {
        try {
            statsVigyaapanStoreDailyManager.tagAsDisplayed(codeQR, DateUtil.getUTCDayOfWeek());
        } catch (Exception e) {
            LOG.error("Error updating store as displayed reason={}", e.getLocalizedMessage(), e);
            BizStoreEntity bizStore = bizStoreManager.findByCodeQR(codeQR);
            StatsVigyaapanStoreDailyEntity statsVigyaapanStoreDaily = new StatsVigyaapanStoreDailyEntity()
                .setBizStoreId(bizStore.getId())
                .setBizNameId(bizStore.getBizName().getId())
                .setCodeQR(bizStore.getCodeQR())
                .setDayOfWeek(DateUtil.getUTCDayOfWeek())
                .setTimesDisplayed(1);
            statsVigyaapanStoreDailyManager.save(statsVigyaapanStoreDaily);
        }
    }

    public JsonAdvertisementList getAllTvAdvertisementsForBusiness(String bizNameId) {
        JsonAdvertisementList jsonAdvertisementList = new JsonAdvertisementList();

        for (AdvertisementTypeEnum advertisementType : AdvertisementTypeEnum.values()) {
            switch (advertisementType) {
                case GI:
                    List<String> imageUrls = new LinkedList<String>() {{
                        add("https://noqapp.com/imgs/appmages/calcium-rich-food.jpg");
                        add("https://noqapp.com/imgs/appmages/vitamin-d-rich-food.jpg");
                    }};
                    jsonAdvertisementList.addJsonVigyaapanTV(new JsonAdvertisement()
                        .setAdvertisementId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setAdvertisementType(AdvertisementTypeEnum.MA)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 20 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );

                    imageUrls = new LinkedList<String>() {{
                        add("https://noqapp.com/imgs/appmages/vegan-protien-rich-food.png");
                    }};
                    jsonAdvertisementList.addJsonVigyaapanTV(new JsonAdvertisement()
                        .setAdvertisementId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setAdvertisementType(AdvertisementTypeEnum.MA)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 20 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );
                    break;
                case MA:
                    imageUrls = new LinkedList<String>() {{
                        add("https://noqapp.com/imgs/appmages/garbhasanskar-ssd-march-2019.png");
                    }};
                    jsonAdvertisementList.addJsonVigyaapanTV(new JsonAdvertisement()
                        .setAdvertisementId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setAdvertisementType(AdvertisementTypeEnum.MA)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 16 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );
                    break;
                case PP:
                default:
                    MedicalRecordEntity medicalRecord = medicalRecordManager.findByBizNameId(bizNameId);
                    if (null != medicalRecord) {
                        JsonProfessionalProfile jsonProfessionalProfile = professionalProfileService.getJsonProfessionalProfile(medicalRecord.getDiagnosedById(), TV);
                        jsonAdvertisementList.addJsonVigyaapanTV(new JsonAdvertisement()
                            .setAdvertisementId(medicalRecord.getCodeQR())
                            .setJsonProfessionalProfile(jsonProfessionalProfile)
                            .setAdvertisementType(AdvertisementTypeEnum.PP));
                    }
            }
        }

        return jsonAdvertisementList;
    }

    public JsonAdvertisementList getAllMobileAdvertisements() {
        JsonAdvertisementList jsonAdvertisementList = new JsonAdvertisementList();

        //advertisementService.

        for (AdvertisementTypeEnum advertisementType : AdvertisementTypeEnum.values()) {
            switch (advertisementType) {
                case GI:
                    List<String> imageUrls = new LinkedList<String>() {{
                        add("https://noqapp.com/imgs/appmages/calcium-rich-food.jpg");
                        add("https://noqapp.com/imgs/appmages/vitamin-d-rich-food.jpg");
                    }};
                    jsonAdvertisementList.addJsonVigyaapanTV(new JsonAdvertisement()
                        .setAdvertisementId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setAdvertisementType(AdvertisementTypeEnum.MA)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 20 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );

                    imageUrls = new LinkedList<String>() {{
                        add("https://noqapp.com/imgs/appmages/vegan-protien-rich-food.png");
                    }};
                    jsonAdvertisementList.addJsonVigyaapanTV(new JsonAdvertisement()
                        .setAdvertisementId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setAdvertisementType(AdvertisementTypeEnum.MA)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 20 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );
                    break;
                case MA:
                    imageUrls = new LinkedList<String>() {{
                        add("https://noqapp.com/imgs/appmages/garbhasanskar-ssd-march-2019.png");
                    }};
                    jsonAdvertisementList.addJsonVigyaapanTV(new JsonAdvertisement()
                        .setAdvertisementId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setAdvertisementType(AdvertisementTypeEnum.MA)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 16 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );
                    break;
            }
        }

        return jsonAdvertisementList;
    }
}
