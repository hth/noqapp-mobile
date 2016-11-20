package com.token.mobile.view.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * User: hitender
 * Date: 11/17/16 4:18 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
public class TokenServletContextListener implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(TokenServletContextListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        LOG.info("Token mobile context destroyed");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        LOG.info("Token context initialized");
    }
}
