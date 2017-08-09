package com.noqapp.mobile.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.util.StringUtils;

import com.noqapp.domain.site.QueueUser;
import com.noqapp.domain.types.RoleEnum;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: hitender
 * Date: 5/28/14 12:42 AM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal"
})
public class OnLoginAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger LOG = LoggerFactory.getLogger(OnLoginAuthenticationSuccessHandler.class);
    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private AuthenticatedToken authenticatedToken;

    @SuppressWarnings ("unused")
    public OnLoginAuthenticationSuccessHandler() {
    }

    @Autowired
    public OnLoginAuthenticationSuccessHandler(AuthenticatedToken authenticatedToken) {
        this.authenticatedToken = authenticatedToken;
    }

    @Override
    public void onAuthenticationSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication
    ) throws ServletException, IOException {
        if (null != request.getHeader("cookie")) {
            handle(request, response, authentication);
            clearAuthenticationAttributes(request);
        } else {
            String username = ((QueueUser) authentication.getPrincipal()).getUsername();
            response.addHeader("X-R-MAIL", username);
            response.addHeader("X-R-AUTH", authenticatedToken.getUserAuthenticationKey(username));
        }

        /**
         * Refer: http://www.baeldung.com/2011/10/31/securing-a-restful-web-service-with-spring-security-3-1-part-3/
         * To execute:
         * curl -i -X POST
         * -d emailId=some@mail.com
         * -d password=realPassword
         * http://localhost:8080/receipt/login
         */
        final SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest == null) {
            clearAuthenticationAttributes(request);
            return;
        }

        final String targetUrlParameter = getTargetUrlParameter();
        if (isAlwaysUseDefaultTargetUrl() || targetUrlParameter != null &&
                StringUtils.hasText(request.getParameter(targetUrlParameter))) {
            requestCache.removeRequest(request, response);
            clearAuthenticationAttributes(request);
            return;
        }

        clearAuthenticationAttributes(request);
    }

    protected void handle(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
        String targetUrl = determineTargetUrl(auth);

        if (res.isCommitted()) {
            LOG.debug("Response has already been committed. Unable to redirect to {}", targetUrl);
            return;
        }

        redirectStrategy.sendRedirect(req, res, targetUrl);
    }

    /**
     * Builds the landing URL according to the user role when they log in.
     * Refer: http://www.baeldung.com/spring_redirect_after_login
     */
    protected String determineTargetUrl(Authentication authentication) {
        String targetURL;

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        GrantedAuthority grantedAuthority = authorities.iterator().next();
        switch (RoleEnum.valueOf(grantedAuthority.getAuthority())) {
            case ROLE_CLIENT:
                targetURL = "/access/landing.htm";
                break;
            case ROLE_SUPERVISOR:
                targetURL = "/emp/landing.htm";
                break;
            case ROLE_TECHNICIAN:
                targetURL = "/emp/landing.htm";
                break;
            case ROLE_ADMIN:
                targetURL = "/admin/landing.htm";
                break;
            default:
                LOG.error("Role set is not defined");
                throw new IllegalStateException("Role set is not defined");
        }

        return targetURL;
    }
}
