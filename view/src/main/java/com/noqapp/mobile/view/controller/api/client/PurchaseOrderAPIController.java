package com.noqapp.mobile.view.controller.api.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noqapp.common.utils.ScrubbedInput;
import com.noqapp.domain.json.JsonPurchaseOrder;
import com.noqapp.domain.json.JsonResponse;
import com.noqapp.health.domain.types.HealthStatusEnum;
import com.noqapp.health.service.ApiHealthService;
import com.noqapp.mobile.common.util.ErrorEncounteredJson;
import com.noqapp.mobile.service.AuthenticateMobileService;
import com.noqapp.service.PurchaseOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static com.noqapp.mobile.common.util.MobileSystemErrorCodeEnum.MOBILE_JSON;
import static com.noqapp.mobile.view.controller.api.client.TokenQueueAPIController.authorizeRequest;

/**
 * hitender
 * 3/30/18 2:05 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@RestController
@RequestMapping(value = "/api/c/purchaseOrder")
public class PurchaseOrderAPIController {
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseOrderAPIController.class);

    private PurchaseOrderService purchaseOrderService;
    private ApiHealthService apiHealthService;
    private AuthenticateMobileService authenticateMobileService;

    @Autowired
    public PurchaseOrderAPIController(
            PurchaseOrderService purchaseOrderService,
            ApiHealthService apiHealthService,
            AuthenticateMobileService authenticateMobileService
    ) {
        this.purchaseOrderService = purchaseOrderService;
        this.apiHealthService = apiHealthService;
        this.authenticateMobileService = authenticateMobileService;
    }

    /** Add purchase when user presses confirm. */
    @PostMapping(
            value = "/purchase",
            produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"
    )
    public String service(
            @RequestHeader("X-R-DID")
            ScrubbedInput did,

            @RequestHeader ("X-R-DT")
            ScrubbedInput dt,

            @RequestHeader ("X-R-MAIL")
            ScrubbedInput mail,

            @RequestHeader ("X-R-AUTH")
            ScrubbedInput auth,

            @RequestBody
            String bodyJson,

            HttpServletResponse response
    ) throws IOException {
        Instant start = Instant.now();
        LOG.info("Purchase Order API for did={} dt={} order={}", did, dt, bodyJson);
        String qid = authenticateMobileService.getQueueUserId(mail.getText(), auth.getText());
        if (authorizeRequest(response, qid)) return null;

        JsonPurchaseOrder jsonPurchaseOrder;
        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonPurchaseOrder = mapper.readValue(bodyJson, JsonPurchaseOrder.class);
        } catch (IOException e) {
            LOG.error("Could not parse json={} reason={}", bodyJson, e.getLocalizedMessage(), e);
            return ErrorEncounteredJson.toJson("Could not parse JSON", MOBILE_JSON);
        }

        boolean orderPlacedSuccess = false;
        try {
            if (qid.equals(jsonPurchaseOrder.getQueueUserId())) {
                LOG.warn("Un-Authorized, order submitted does not match queueUserId in the order qid={} orderQid={}",
                        qid,
                        jsonPurchaseOrder.getQueueUserId());

                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Purchase Order");
                return null;
            }

            orderPlacedSuccess = purchaseOrderService.createOrder(jsonPurchaseOrder);
            return new JsonResponse(orderPlacedSuccess).asJson();
        } catch (Exception e) {
            LOG.error("Failed processing purchase order reason={}", e.getLocalizedMessage(), e);
            apiHealthService.insert(
                    "/purchase",
                    "purchase",
                    PurchaseOrderAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.F);
            return new JsonResponse(orderPlacedSuccess).asJson();
        } finally {
            apiHealthService.insert(
                    "/purchase",
                    "purchase",
                    PurchaseOrderAPIController.class.getName(),
                    Duration.between(start, Instant.now()),
                    HealthStatusEnum.G);
        }
    }
}
