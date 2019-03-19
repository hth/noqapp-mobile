package com.noqapp.mobile.view.controller.api;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_UPLOAD;

import com.noqapp.common.utils.FileUtil;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.domain.types.medical.LabCategoryEnum;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.service.MedicalFileService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.service.AccountMobileService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.mobile.view.controller.api.client.ClientProfileAPIController;
import com.noqapp.mobile.view.controller.api.merchant.health.MedicalRecordController;
import com.noqapp.mobile.view.controller.api.merchant.store.PurchaseOrderController;
import com.noqapp.service.FileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 2019-01-23 15:32
 */
@Controller
public class ImageCommonHelper extends CommonHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ImageCommonHelper.class);

    private AuthenticateMobileService authenticateMobileService;
    private FileService fileService;
    private MedicalFileService medicalFileService;
    private ApiHealthService apiHealthService;

    @Autowired
    public ImageCommonHelper(
        AccountMobileService accountMobileService,
        AuthenticateMobileService authenticateMobileService,
        FileService fileService,
        MedicalFileService medicalFileService,
        ApiHealthService apiHealthService
    ) {
        super(accountMobileService);
        this.authenticateMobileService = authenticateMobileService;
        this.fileService = fileService;
        this.medicalFileService = medicalFileService;
        this.apiHealthService = apiHealthService;
    }

    public String uploadProfileImage(
        String did,
        String dt,
        String mail,
        String auth,
        String profileImageOfQid,
        MultipartFile multipartFile,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = false;
        Instant start = Instant.now();
        LOG.info("Profile Image upload dt={} did={} mail={}, auth={}", dt, did, mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail, auth);
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (null == checkSelfOrDependent(qid, profileImageOfQid)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (multipartFile.isEmpty()) {
            LOG.error("File name missing in request or no file uploaded");
            return ErrorEncounteredJson.toJson("File missing in request or no file uploaded.", MOBILE_UPLOAD);
        }

        try {
            processProfileImage(profileImageOfQid, multipartFile);
            methodStatusSuccess = true;
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed uploading profile image reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/upload",
                "upload",
                ClientProfileAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    public String removeProfileImage(
        String did,
        String dt,
        String mail,
        String auth,
        String profileImageOfQid,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = false;
        Instant start = Instant.now();
        LOG.info("Profile Image upload dt={} did={} mail={}, auth={}", dt, did, mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail, auth);
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (null == checkSelfOrDependent(qid, profileImageOfQid)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            fileService.removeProfileImage(qid);
            methodStatusSuccess = true;
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed removing profile image reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/removeProfileImage",
                "removeProfileImage",
                ClientProfileAPIController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    public String uploadMedicalRecordImage(
        String did,
        String dt,
        String mail,
        String auth,
        String recordReferenceId,
        MultipartFile multipartFile,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = false;
        Instant start = Instant.now();
        LOG.info("Medical Image upload dt={} did={} mail={}, auth={}", dt, did, mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail, auth);
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (multipartFile.isEmpty()) {
            LOG.error("File name missing in request or no file uploaded");
            return ErrorEncounteredJson.toJson("File missing in request or no file uploaded.", MOBILE_UPLOAD);
        }

        try {
            String filename = medicalFileService.processMedicalImage(recordReferenceId, multipartFile);
            methodStatusSuccess = true;
            return new JsonResponse(true, filename).asJson();
        } catch (Exception e) {
            LOG.error("Failed uploading medical image reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/appendImage",
                "appendImage",
                MedicalRecordController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    public String removeMedicalImage(
        String did,
        String dt,
        String mail,
        String auth,
        String recordReferenceId,
        String filename,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = false;
        Instant start = Instant.now();
        LOG.info("Remove medical image upload dt={} did={} mail={}, auth={}", dt, did, mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail, auth);
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            medicalFileService.removeMedicalImage(qid, recordReferenceId, filename);
            methodStatusSuccess = true;
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed removing medical image reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/removeImage",
                "removeImage",
                MedicalRecordController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    public String processReport(
        String did,
        String dt,
        String mail,
        String auth,
        String transactionId,
        LabCategoryEnum labCategory,
        MultipartFile multipartFile,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = false;
        Instant start = Instant.now();
        LOG.info("Lab attachment upload dt={} did={} mail={}, auth={}", dt, did, mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail, auth);
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        if (multipartFile.isEmpty()) {
            LOG.error("File name missing in request or no file uploaded");
            return ErrorEncounteredJson.toJson("File missing in request or no file uploaded.", MOBILE_UPLOAD);
        }

        try {
            String filename = medicalFileService.processReport(transactionId, multipartFile, labCategory);
            methodStatusSuccess = true;
            return new JsonResponse(true, filename).asJson();
        } catch (Exception e) {
            LOG.error("Failed uploading lab attachment reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/addAttachment",
                "addAttachment",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    public String removeReport(
        String did,
        String dt,
        String mail,
        String auth,
        String transactionId,
        String filename,
        LabCategoryEnum labCategory,
        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = false;
        Instant start = Instant.now();
        LOG.info("Remove lab attachment upload dt={} did={} mail={}, auth={}", dt, did, mail, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail, auth);
        if (null == qid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            medicalFileService.removeReport(qid, transactionId, filename, labCategory);
            methodStatusSuccess = true;
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed removing medical image reason={}", e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return new JsonResponse(false).asJson();
        } finally {
            apiHealthService.insert(
                "/removeAttachment",
                "removeAttachment",
                PurchaseOrderController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    private void processProfileImage(String qid, MultipartFile multipartFile) throws IOException {
        BufferedImage bufferedImage = fileService.bufferedImage(multipartFile.getInputStream());
        String mimeType = FileUtil.detectMimeType(multipartFile.getInputStream());
        if (mimeType.equalsIgnoreCase(multipartFile.getContentType())) {
            fileService.addProfileImage(
                qid,
                FileUtil.createRandomFilenameOf24Chars() + FileUtil.getImageFileExtension(multipartFile.getOriginalFilename(), mimeType),
                bufferedImage);
        } else {
            LOG.error("Failed mime mismatch found={} sentMime={}", mimeType, multipartFile.getContentType());
            throw new RuntimeException("Mime type mismatch");
        }
    }
}
