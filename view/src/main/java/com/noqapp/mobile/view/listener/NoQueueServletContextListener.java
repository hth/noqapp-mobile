package com.noqapp.mobile.view.listener;

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
public class NoQueueServletContextListener implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(NoQueueServletContextListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        LOG.info("NoQueue Mobile context destroyed");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        LOG.info("NoQueue Mobile context initialized");
    }
}
