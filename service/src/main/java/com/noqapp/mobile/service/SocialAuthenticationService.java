package com.noqapp.mobile.service;

import static com.noqapp.common.utils.CommonUtil.AUTH_KEY_HIDDEN;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.SEVERE;
import static com.noqapp.common.errors.MobileSystemErrorCodeEnum.USER_SOCIAL;

import com.noqapp.common.errors.ErrorEncounteredJson;
import com.noqapp.mobile.domain.ProviderAndAccessToken;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * User: hitender
 * Date: 1/14/17 12:17 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Service
public class SocialAuthenticationService {
    public static final int MAX_RESPONSE_SIZE = 2048;
    public static final int MIN_RESPONSE_SIZE = -1;
    private static final Logger LOG = LoggerFactory.getLogger(SocialAuthenticationService.class);

    @Value("${auth.create:/webapi/mobile/auth-create.htm}")
    private String authCreateEndPoint;

    private WebConnectorService webConnectorService;

    @Autowired
    public SocialAuthenticationService(WebConnectorService webConnectorService) {
        this.webConnectorService = webConnectorService;
    }

    /**
     * Call this on terminal as below.
     * http localhost:9090/receipt-mobile/authenticate.json < ~/Downloads/pid.json
     *
     * @param providerId
     * @param accessToken
     * @return
     */
    public String authenticateWeb(String providerId, String accessToken, HttpClient httpClient) {
        LOG.debug("providerId={} accessToken={} webApiAccessToken={}", providerId, AUTH_KEY_HIDDEN, AUTH_KEY_HIDDEN);
        HttpPost httpPost = webConnectorService.getHttpPost(authCreateEndPoint, httpClient);
        if (httpPost == null) {
            return ErrorEncounteredJson.toJson(webConnectorService.getNoResponseFromWebServer(), SEVERE);
        }

        populateEntity(providerId, accessToken, httpPost);
        HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);
        } catch (IOException e) {
            LOG.error("error occurred while executing request path={} reason={}", httpPost.getURI(), e.getLocalizedMessage(), e);
        }

        if (response == null) {
            return ErrorEncounteredJson.toJson(webConnectorService.getNoResponseFromWebServer(), SEVERE);
        }

        int status = response.getStatusLine().getStatusCode();
        LOG.debug("status={}", status);
        if (status >= WebConnectorService.HTTP_STATUS_200 && status < WebConnectorService.HTTP_STATUS_300) {
            return responseString(response.getEntity());
        }

        LOG.error("server responded with response code={}", status);
        return ErrorEncounteredJson.toJson(providerId + " sign in failed.", USER_SOCIAL);
    }

    /**
     * Returns response.
     *
     * @param entity
     * @return
     */
    private String responseString(HttpEntity entity) {
        if (entity != null) {
            long len = entity.getContentLength();
            LOG.debug("response length={}", len);
            if (len != MIN_RESPONSE_SIZE && len < MAX_RESPONSE_SIZE) {
                try {
                    String data = EntityUtils.toString(entity);
                    LOG.debug("data={}", data);
                    return data;
                } catch (IOException e) {
                    LOG.error("error occurred while parsing entity reason={}", e.getLocalizedMessage(), e);
                    return "{}";
                }
            }
        }

        /** Stream too big */
        LOG.warn("Stream size bigger than {}", MAX_RESPONSE_SIZE);
        return ErrorEncounteredJson.toJson("Stream size bigger than " + MAX_RESPONSE_SIZE, SEVERE);
    }

    /**
     * Create Request Body.
     *
     * @param providerId
     * @param accessToken
     * @param httpPost
     */
    private void populateEntity(String providerId, String accessToken, HttpPost httpPost) {
        httpPost.setEntity(
            new StringEntity(
                new Gson().toJson(ProviderAndAccessToken.newInstance(providerId, accessToken)),
                ContentType.create(MediaType.APPLICATION_JSON_VALUE, "UTF-8")
            )
        );
    }
}

