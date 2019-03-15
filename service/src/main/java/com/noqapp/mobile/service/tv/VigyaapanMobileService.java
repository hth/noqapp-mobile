package com.noqapp.mobile.service.tv;

import static com.noqapp.common.utils.AbstractDomain.ISO8601_FMT;
import static com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE.*;

import com.noqapp.common.utils.DateUtil;
import com.noqapp.domain.BizStoreEntity;
import com.noqapp.domain.StatsVigyaapanStoreDailyEntity;
import com.noqapp.domain.json.JsonProfessionalProfile;
import com.noqapp.domain.json.tv.JsonVigyaapanTV;
import com.noqapp.domain.json.tv.JsonVigyaapanTVList;
import com.noqapp.domain.types.VigyaapanTypeEnum;
import com.noqapp.medical.domain.MedicalRecordEntity;
import com.noqapp.medical.repository.MedicalRecordManager;
import com.noqapp.repository.BizStoreManager;
import com.noqapp.repository.StatsVigyaapanStoreDailyManager;
import com.noqapp.service.ProfessionalProfileService;
import com.noqapp.service.ProfessionalProfileService.POPULATE_PROFILE;

import org.apache.commons.lang3.time.DateFormatUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
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

    @Deprecated
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
                if (null == medicalRecord) {
                    return new JsonVigyaapanTV()
                        .setVigyaapanType(VigyaapanTypeEnum.PP);
                }
                JsonProfessionalProfile jsonProfessionalProfile = professionalProfileService.getJsonProfessionalProfile(medicalRecord.getDiagnosedById(), TV);
                return new JsonVigyaapanTV()
                    .setVigyaapanId(medicalRecord.getCodeQR())
                    .setJsonProfessionalProfile(jsonProfessionalProfile)
                    .setVigyaapanType(VigyaapanTypeEnum.PP);
        }
    }

    public JsonVigyaapanTVList getAllVigyaapanForBusiness(String bizNameId) {
        JsonVigyaapanTVList jsonVigyaapanTVList = new JsonVigyaapanTVList();

        for (VigyaapanTypeEnum vigyaapanType : VigyaapanTypeEnum.values()) {
            switch (vigyaapanType) {
                case DV:
                    break;
                case GI:
                    List<String> imageUrls = new LinkedList<String>() {{
                        add("https://cdn.pixabay.com/photo/2015/12/01/20/28/road-1072823_960_720.jpg");
                        add("http://cdn.theindianspot.com/wp-content/uploads/2017/01/19111807/MEATLESS-PROTEIN-SOURCE.jpg");
                    }};
                    jsonVigyaapanTVList.addJsonVigyaapanTV(new JsonVigyaapanTV()
                        .setVigyaapanId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setVigyaapanType(VigyaapanTypeEnum.MV)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 20 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );

                    imageUrls = new LinkedList<String>() {{
                        add("https://cdn.pixabay.com/photo/2013/11/28/10/36/road-220058_960_720.jpg");
                    }};
                    jsonVigyaapanTVList.addJsonVigyaapanTV(new JsonVigyaapanTV()
                        .setVigyaapanId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setVigyaapanType(VigyaapanTypeEnum.MV)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 20 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );
                    break;
                case MV:
                    imageUrls = new LinkedList<String>() {{
                        add("https://noqapp.com/imgs/appmages/garbhasanskar-ssd-march-2019.png");
                    }};
                    jsonVigyaapanTVList.addJsonVigyaapanTV(new JsonVigyaapanTV()
                        .setVigyaapanId(UUID.randomUUID().toString())
                        .setImageUrls(imageUrls)
                        .setVigyaapanType(VigyaapanTypeEnum.MV)
                        .setEndDate(DateFormatUtils.format(DateUtil.asDate(LocalDate.of( 2019 , 3 , 16 )), ISO8601_FMT, TimeZone.getTimeZone("UTC")))
                    );
                    break;
                case PP:
                default:
                    MedicalRecordEntity medicalRecord = medicalRecordManager.findByBizNameId(bizNameId);
                    if (null != medicalRecord) {
                        JsonProfessionalProfile jsonProfessionalProfile = professionalProfileService.getJsonProfessionalProfile(medicalRecord.getDiagnosedById(), TV);
                        jsonVigyaapanTVList.addJsonVigyaapanTV(new JsonVigyaapanTV()
                            .setVigyaapanId(medicalRecord.getCodeQR())
                            .setJsonProfessionalProfile(jsonProfessionalProfile)
                            .setVigyaapanType(VigyaapanTypeEnum.PP));
                    }
            }
        }

        return jsonVigyaapanTVList;
    }
}
