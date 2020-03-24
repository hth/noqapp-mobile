package com.noqapp.mobile.view.validator;

import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPLOAD;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPLOAD_EXCEED_SIZE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPLOAD_NO_SIZE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.MOBILE_UPLOAD_UNSUPPORTED_FORMAT;
import static com.noqapp.mobile.view.validator.ImageValidator.SUPPORTED_FILE.IMAGE_AND_PDF;

import com.noqapp.common.errors.ErrorEncounteredJson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * hitender
 * 7/16/18 11:30 AM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Component
public class ImageValidator {
    private static final Logger LOG = LoggerFactory.getLogger(ImageValidator.class);

    private String[] supportedFormat = new String[]{"image/jpg", "image/jpeg", "image/png"};
    private String[] supportedPDFFormat = new String[]{"image/jpg", "image/jpeg", "image/png", "application/pdf"};

    public enum SUPPORTED_FILE {IMAGE, IMAGE_AND_PDF}

    public Map<String, String> validate(MultipartFile file, SUPPORTED_FILE supportedFile) {
        Map<String, String> errors = new HashMap<>();
        try {
            if (file.isEmpty() || file.getSize() == 0) {
                LOG.info("Please select a file to upload");
                errors.put(ErrorEncounteredJson.REASON, "Please select a file to upload");
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MOBILE_UPLOAD_NO_SIZE.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MOBILE_UPLOAD_NO_SIZE.getCode());
            }

            String s = file.getContentType().toLowerCase();
            boolean contains = Arrays.asList(supportedFile == IMAGE_AND_PDF ? supportedPDFFormat : supportedFormat).contains(s);
            if (!contains) {
                String filesSupportedFormat;
                if (supportedFile == IMAGE_AND_PDF) {
                    filesSupportedFormat = "jpg/png/pdf";
                } else {
                    filesSupportedFormat = "jpg/png";
                }
                LOG.error("Supported file formats are {}", filesSupportedFormat);
                errors.put(ErrorEncounteredJson.REASON, "Supported file formats are " + filesSupportedFormat);
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MOBILE_UPLOAD_UNSUPPORTED_FORMAT.name());
                errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MOBILE_UPLOAD_UNSUPPORTED_FORMAT.getCode());
            }

            if (file.getSize() > 0) {
                // Get length of file in bytes
                long fileSizeInBytes = file.getSize();
                // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
                long fileSizeInKB = fileSizeInBytes / 1024;
                // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
                long fileSizeInMB = fileSizeInKB / 1024;

                if (fileSizeInMB > 5) {
                    LOG.error("Selected file size exceeds 5MB");
                    errors.put(ErrorEncounteredJson.REASON, "Selected file size exceeds 5MB");
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MOBILE_UPLOAD_EXCEED_SIZE.name());
                    errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MOBILE_UPLOAD_EXCEED_SIZE.getCode());
                }
            }
        } catch (Exception e) {
            LOG.error("Failed validating image file");
            errors.put(ErrorEncounteredJson.REASON, "Something went wrong. Engineers are looking into this.");
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR, MOBILE_UPLOAD.name());
            errors.put(ErrorEncounteredJson.SYSTEM_ERROR_CODE, MOBILE_UPLOAD.getCode());
        }

        return errors;
    }
}
