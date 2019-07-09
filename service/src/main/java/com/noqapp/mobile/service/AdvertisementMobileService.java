package com.noqapp.mobile.service;

import static com.noqapp.common.utils.AbstractDomain.ISO8601_FMT;
import static com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE.TV;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.domain.AdvertisementEntity;
import com.noqapp.domain.BizNameEntity;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StatsVigyaapanStoreDailyEntity;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.tv.JsonAdvertisement;
import com.noqapp.domain.json.tv.JsonAdvertisementList;
import com.noqapp.domain.types.AdvertisementTypeEnum;
import com.noqapp.medical.domain.MedicalRecordEntity;
import com.noqapp.medical.repository.MedicalRecordManager;
import com.noqapp.repository.BizNameManager;
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

    private BizNameManager bizNameManager;
    private BizStoreManager bizStoreManager;
    private StatsVigyaapanStoreDailyManager statsVigyaapanStoreDailyManager;
    private ProfessionalProfileService professionalProfileService;
    private MedicalRecordManager medicalRecordManager;
    private AdvertisementService advertisementService;

    @Autowired
    public AdvertisementMobileService(
        BizNameManager bizNameManager,
        BizStoreManager bizStoreManager,
        StatsVigyaapanStoreDailyManager statsVigyaapanStoreDailyManager,
        ProfessionalProfileService professionalProfileService,
        MedicalRecordManager medicalRecordManager,
        AdvertisementService advertisementService
    ) {
        this.bizNameManager = bizNameManager;
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

    public JsonAdvertisementList findAllMobileTVApprovedAdvertisements(String bizNameId) {
        JsonAdvertisementList jsonAdvertisementList = new JsonAdvertisementList();

        List<AdvertisementEntity> advertisements = advertisementService.findAllMobileTVApprovedAdvertisements(bizNameId);
        for (AdvertisementEntity advertisement : advertisements) {
            jsonAdvertisementList.addJsonAdvertisement(
                new JsonAdvertisement()
                    .setAdvertisementId(advertisement.getId())
                    .setTitle(advertisement.getTitle())
                    .setShortDescription(advertisement.getShortDescription())
                    .setImageUrls(advertisement.getImageUrls())
                    .setTermsAndConditions(advertisement.getTermsAndConditions())
                    .setAdvertisementType(advertisement.getAdvertisementType())
                    .setBusinessName(null)
                    .setAdvertisementViewerType(advertisement.getAdvertisementViewerType())
            );
        }

        MedicalRecordEntity medicalRecord = medicalRecordManager.findByBizNameId(bizNameId);
        if (null != medicalRecord) {
            JsonProfessionalProfile jsonProfessionalProfile = professionalProfileService.getJsonProfessionalProfile(medicalRecord.getDiagnosedById(), TV);
            jsonAdvertisementList.addJsonAdvertisement(new JsonAdvertisement()
                .setAdvertisementId(medicalRecord.getCodeQR())
                .setJsonProfessionalProfile(jsonProfessionalProfile)
                .setAdvertisementType(AdvertisementTypeEnum.PP)
            );
        }

        return jsonAdvertisementList;
    }

    public JsonAdvertisementList findAllMobileApprovedAdvertisements() {
        JsonAdvertisementList jsonAdvertisementList = new JsonAdvertisementList();

        List<AdvertisementEntity> advertisements = advertisementService.findAllMobileApprovedAdvertisements();
        for (AdvertisementEntity advertisement : advertisements) {
            BizNameEntity bizName = bizNameManager.getById(advertisement.getBizNameId());
            jsonAdvertisementList.addJsonAdvertisement(
                new JsonAdvertisement()
                    .setAdvertisementId(advertisement.getId())
                    .setTitle(advertisement.getTitle())
                    .setShortDescription(advertisement.getShortDescription())
                    .setImageUrls(advertisement.getImageUrls())
                    .setTermsAndConditions(advertisement.getTermsAndConditions())
                    .setAdvertisementType(advertisement.getAdvertisementType())
                    .setBusinessName(bizName.getBusinessName())
                    .setAdvertisementViewerType(advertisement.getAdvertisementViewerType())
            );
        }
        return jsonAdvertisementList;
    }
}
