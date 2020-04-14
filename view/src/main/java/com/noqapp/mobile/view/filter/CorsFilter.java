package com.noqapp.mobile.view.filter;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * hitender
 * 4/14/20 10:20 AM
 */
@Component
public class CorsFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(CorsFilter.class);
    private String allowedOrigins = "http://localhost:4200";

    static final String ORIGIN = "Origin";

    private boolean enabled = true;

    @Value("${cors.enabled}")
    public void setEnabled(String enabled) {
        if (enabled != null) {
            this.enabled = enabled.matches("^[ \\t]*(true|1|yes)[ \\t]*$");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        LOG.info("Origin={} method={}", request.getHeader(ORIGIN), request.getMethod());
        if (request.getHeader(ORIGIN) == null || request.getHeader(ORIGIN).equals("null") || request.getHeader(ORIGIN).equals(getAllowedOrigins())) {
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.addHeader("Access-Control-Max-Age", "10");

            String reqHead = request.getHeader("Access-Control-Request-Headers");

            if (!StringUtils.isEmpty(reqHead)) {
                response.addHeader("Access-Control-Allow-Headers", reqHead);
            }
        }

        if (request.getMethod().equals("OPTIONS")) {
            try {
                response.getWriter().print("OK");
                response.getWriter().flush();
            } catch (IOException e) {
                LOG.error("Failed at CorsFilter reason={}", e.getLocalizedMessage(), e);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
