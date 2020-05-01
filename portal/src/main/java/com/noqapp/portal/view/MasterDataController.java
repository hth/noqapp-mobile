package com.noqapp.portal.view;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.utils.CommonUtil.UNAUTHORIZED;
import static com.noqapp.domain.types.RoleEnum.ROLE_Q_SUPERVISOR;
import static com.noqapp.domain.types.RoleEnum.ROLE_S_MANAGER;

import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.UserAccountEntity;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.medical.domain.MasterLabEntity;
import com.noqapp.medical.domain.json.JsonGlobalMedicalData;
import com.noqapp.medical.domain.json.JsonGlobalMedicalDataList;
import com.noqapp.medical.repository.MasterLabManager;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 4/29/20 6:23 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/portal/view/master")
public class MasterDataController {
    private static final Logger LOG = LoggerFactory.getLogger(MasterDataController.class);

    private AuthenticateMobileService authenticateMobileService;
    private MasterLabService masterLabService;
    private ApiHealthService apiHealthService;
    private MasterLabManager masterLabManager;

    @Autowired
    public MasterDataController(
        AuthenticateMobileService authenticateMobileService,
        MasterLabService masterLabService,
        ApiHealthService apiHealthService,
        MasterLabManager masterLabManager
    ) {
        this.authenticateMobileService = authenticateMobileService;
        this.masterLabService = masterLabService;
        this.apiHealthService = apiHealthService;
        this.masterLabManager = masterLabManager;
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
            LOG.warn("Un-authorized access to /portal/view/master/file by mail={}", mail);
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
                "/portal/view/master/file",
                "file",
                MasterDataController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }

    /** Gets file of all products as zip in CSV format for preferred business store id. */
    @GetMapping(
        value = "/db",
        produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
    )
    public String db(
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
            LOG.warn("Un-authorized access to /portal/view/master/db by mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        UserAccountEntity userAccount = authenticateMobileService.findByQueueUserId(qid);
        if (!userAccount.getRoles().contains(ROLE_S_MANAGER) && !userAccount.getRoles().contains(ROLE_Q_SUPERVISOR)) {
            LOG.info("Your are not authorized to get file for as roles not match mail={}", mail);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED);
            return null;
        }

        JsonGlobalMedicalDataList jsonGlobalMedicalDataList = new JsonGlobalMedicalDataList();
        try {
            List<MasterLabEntity> masterLabs = masterLabManager.findAll();
            for (MasterLabEntity masterLab : masterLabs) {
                jsonGlobalMedicalDataList.getJsonGlobalMedicalDataList().add(JsonGlobalMedicalData.toJson(masterLab));
            }

            return jsonGlobalMedicalDataList.asJson();
        } catch (Exception e) {
            LOG.error("Failed getting lab qid={} message={}", qid, e.getLocalizedMessage(), e);
            methodStatusSuccess = false;
            return jsonGlobalMedicalDataList.asJson();
        } finally {
            apiHealthService.insert(
                "/portal/view/master/db",
                "file",
                MasterDataController.class.getName(),
                Duration.between(start, Instant.now()),
                methodStatusSuccess ? HealthStatusEnum.G : HealthStatusEnum.F);
        }
    }
}
