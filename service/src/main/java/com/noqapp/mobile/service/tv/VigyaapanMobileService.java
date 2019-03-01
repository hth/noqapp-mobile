package com.noqapp.mobile.service.tv;

import static com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE.*;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StatsVigyaapanStoreDailyEntity;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.tv.JsonVigyaapanTV;
import com.noqapp.domain.types.VigyaapanTypeEnum;
import com.noqapp.medical.domain.MedicalRecordEntity;
import com.noqapp.medical.repository.MedicalRecordManager;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.repository.StatsVigyaapanStoreDailyManager;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * hitender
 * 2018-12-20 10:15
 */
@Service
public class VigyaapanMobileService {
    private static final Logger LOG = LoggerFactory.getLogger(VigyaapanMobileService.class);

    private BizStoreManager bizStoreManager;
    private StatsVigyaapanStoreDailyManager statsVigyaapanStoreDailyManager;
    private ProfessionalProfileService professionalProfileService;
    private MedicalRecordManager medicalRecordManager;

    @Autowired
    public VigyaapanMobileService(
        BizStoreManager bizStoreManager,
        StatsVigyaapanStoreDailyManager statsVigyaapanStoreDailyManager,
        ProfessionalProfileService professionalProfileService,
        MedicalRecordManager medicalRecordManager
    ) {
        this.statsVigyaapanStoreDailyManager = statsVigyaapanStoreDailyManager;
        this.bizStoreManager = bizStoreManager;
        this.professionalProfileService = professionalProfileService;
        this.medicalRecordManager = medicalRecordManager;
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

    public JsonVigyaapanTV displayVigyaapan(VigyaapanTypeEnum vigyaapanType) {
        switch (vigyaapanType) {
            case DV:
                List<String> imageUrls = new LinkedList<String>() {{
                    add("https://noqapp.com/static2/internal/img/banner.jpg");
                }};
                return new JsonVigyaapanTV()
                    .setVigyaapanId(UUID.randomUUID().toString())
                    .setImageUrls(imageUrls)
                    .setVigyaapanType(VigyaapanTypeEnum.DV);
            case GI:
                imageUrls = new LinkedList<String>() {{
                    add("https://i.pinimg.com/736x/6e/75/34/6e7534e0882e3e543419027bb00effb5--exercise--fitness-health-fitness.jpg");
                }};
                return new JsonVigyaapanTV()
                    .setVigyaapanId(UUID.randomUUID().toString())
                    .setImageUrls(imageUrls)
                    .setVigyaapanType(VigyaapanTypeEnum.GI);
            case MV:
                imageUrls = new LinkedList<String>() {{
                    add("https://noqapp.com/imgs/appmages/garbhasanskar-ssd-march-2019.png");
                }};
                return new JsonVigyaapanTV()
                    .setVigyaapanId(UUID.randomUUID().toString())
                    .setImageUrls(imageUrls)
                    .setVigyaapanType(VigyaapanTypeEnum.MV);
            case PP:
            default:
                MedicalRecordEntity medicalRecord = medicalRecordManager.findOne();
                JsonProfessionalProfile jsonProfessionalProfile = professionalProfileService.getJsonProfessionalProfile(medicalRecord.getDiagnosedById(), TV);
                return new JsonVigyaapanTV()
                    .setVigyaapanId(medicalRecord.getCodeQR())
                    .setJsonProfessionalProfile(jsonProfessionalProfile)
                    .setVigyaapanType(VigyaapanTypeEnum.PP);
        }
    }
}
