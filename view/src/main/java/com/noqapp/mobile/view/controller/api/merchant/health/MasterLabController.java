package com.noqapp.mobile.view.controller.api.merchant.health;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.domain.types.RoleEnum.ROLE_Q_SUPERVISOR;
import static com.noqapp.domain.types.RoleEnum.ROLE_S_MANAGER;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.mobile.view.controller.open.DeviceController.getErrorReason;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.MasterLabEntity;
import com.noqapp.medical.domain.json.JsonMasterLab;
import com.noqapp.medical.service.MasterLabService;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.FtpService;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.util.FileObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 2018-11-23 08:55
 */
@SuppressWarnings ({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/m/h/lab")
public class MasterLabController {
    private static final Logger LOG = LoggerFactory.getLogger(MasterLabController.class);

    private AuthenticateMobileService authenticateMobileService;
    private MasterLabService masterLabService;
    private ApiHealthService apiHealthService;

    @Autowired
    public MasterLabController(
        AuthenticateMobileService authenticateMobileService,
        MasterLabService masterLabService,
        ApiHealthService apiHealthService
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.masterLabService = masterLabService;
        this.apiHealthService = apiHealthService;
    }

    /** Gets file of all products as zip in CSV format for preferred business store id. */
    @GetMapping(
        value = "/file",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public void file(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Fetch preferred file mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/lab/file by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return;
        }

        UserAccountEntity userAccount = authenticateMobileService.findByQueueUserId(qid);
        if (!userAccount.getRoles().contains(ROLE_S_MANAGER) && !userAccount.getRoles().contains(ROLE_Q_SUPERVISOR)) {
            LOG.info("Your are not authorized to get file for as roles not match mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return;
        }

        DefaultFileSystemManager manager = new StandardFileSystemManager();
        try {
            manager.init();
            FileObject fileObject = masterLabService.getMasterTarGZ(manager);
            if (fileObject != null && fileObject.getContent() != null) {
                response.setHeader("Content-disposition", "attachment; filename=\"" + com.noqapp.common.utils.FileUtil.getFileName(fileObject) + "\"");
                response.setContentType("application/gzip");
                response.setContentLength((int)fileObject.getContent().getSize());
                try (OutputStream out = response.getOutputStream()) {
                    out.write(FileObjectUtils.getContentAsByteArray(fileObject));
                } catch (IOException e) {
                    LOG.error("Failed to get file for reason={}", e.getLocalizedMessage(), e);
                }

                return;
            }

            LOG.warn("Failed getting lab file");
            response.setContentType("application/gzip");
            response.setHeader("Content-Disposition", String.format("attachment; filename=%s", ""));
            response.setContentLength(0);
        } catch (FileSystemException e) {
            LOG.error("Failed to get directory={} reason={}", FtpService.MASTER_MEDICAL, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
        } catch (Exception e) {
            LOG.error("Failed getting lab qid={} message={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
        } finally {
            manager.close();
            apiHealthService.insert(
                "/api/m/h/lab/file",
                "file",
                MasterLabController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Adds data to master data. We need not use this. */
    @Deprecated
    @PostMapping(
        value = "/add",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String add(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonMasterLab jsonMasterLab,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Add medical record mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/lab/add by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        UserAccountEntity userAccount = authenticateMobileService.findByQueueUserId(qid);
        if (!userAccount.getRoles().contains(ROLE_S_MANAGER)) {
            LOG.info("Your are not authorized to get file for as roles not match mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            MasterLabEntity masterLab = new MasterLabEntity()
                .setHealthCareService(jsonMasterLab.getHealthCareService())
                .setProductName(jsonMasterLab.getProductName())
                .setProductShortName(jsonMasterLab.getProductShortName())
                .setMedicalDepartments(jsonMasterLab.getMedicalDepartments());
            masterLabService.save(masterLab);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing medical record json={} qid={} message={}", jsonMasterLab.asJson(), qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/add",
                "add",
                MasterLabController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    @PostMapping(
        value = "/flag",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String flag(
        @RequestHeader("X-R-DID")
        ScrubbedInput did,

        @RequestHeader ("X-R-DT")
        ScrubbedInput deviceType,

        @RequestHeader ("X-R-MAIL")
        ScrubbedInput mail,

        @RequestHeader ("X-R-AUTH")
        ScrubbedInput auth,

        @RequestBody
        JsonMasterLab jsonMasterLab,

        HttpServletResponse response
    ) throws IOException {
        boolean methodStatusSuccess = true;
        Instant start = Instant.now();
        LOG.info("Add medical record mail={} did={} deviceType={} auth={}", mail, did, deviceType, AUTH_KEY_HIDDEN);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (null == qid) {
            LOG.warn("Un-authorized access to /api/m/h/lab/flag by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        try {
            masterLabService.flagData(jsonMasterLab.getProductName(), jsonMasterLab.getHealthCareService(), qid);
            return new JsonResponse(true).asJson();
        } catch (Exception e) {
            LOG.error("Failed flagging medical data json={} qid={} message={}", jsonMasterLab.asJson(), qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return getErrorReason("Something went wrong. Engineers are looking into this.", SEVERE);
        } finally {
            apiHealthService.insert(
                "/flag",
                "flag",
                MasterLabController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
