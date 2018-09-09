package com.noqapp.mobile.view.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/**
 * hitender
 * 3/20/18 6:03 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
public final class HttpRequestResponseParser {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestResponseParser.class);

    private HttpRequestResponseParser() {
    }

    public static String printHeader(HttpServletRequest request) {
        StringBuilder stringBuilder = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            stringBuilder.append(headerName).append(":").append(headerValue).append(",");
        }
        return stringBuilder.toString();
    }

    /**
     * Make sure tomcat conf has this setting for forwarding IP.
     *
     * <Valve className="org.apache.catalina.valves.RemoteIpValve"
     *                remoteIpHeader="X-Forwarded-For"
     *                requestAttributesEnabled="true"
     *                internalProxies="127\.0\.0\.1"  />
     *
     * Returns clients IP address.
     *
     * @param request
     * @return
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (null == ip) {
            LOG.warn("IP Address found is NULL");
        }
        return ip;
    }
}