package com.noqapp.mobile.view.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.noqapp.service.config.FirebaseConfig;

import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * User: hitender
 * Date: 11/17/16 4:19 PM
 */
@SuppressWarnings ({
        "PMD.BeanMembersShouldSerialize",
        "PMD.LocalVariableCouldBeFinal",
        "PMD.MethodArgumentCouldBeFinal",
        "PMD.LongVariable"
})
@Component
public class NoQAppInitializationCheckBean {

    private static final Logger LOG = LoggerFactory.getLogger(NoQAppInitializationCheckBean.class);

    private DataSource dataSource;
    private FirebaseConfig firebaseConfig;

    @Autowired
    public NoQAppInitializationCheckBean(DataSource dataSource, FirebaseConfig firebaseConfig) {
        this.dataSource = dataSource;
        this.firebaseConfig = firebaseConfig;
    }

    @PostConstruct
    public void checkRDBConnection() throws SQLException {
        if (this.dataSource.getConnection().isClosed()) {
            LOG.error("RBS could not be connected");
            throw new RuntimeException("RDB could not be connected");
        }
        LOG.info("RDB connected");
    }
    
    @PostConstruct
    public void checkFirebaseConnection() {
        if (null == firebaseConfig.getFirebaseAuth()) {
            LOG.error("Firebase could not be connected");
            throw new RuntimeException("Firebase could not be connected");
        }
        LOG.info("Firebase connected db={}", firebaseConfig.getFirebaseApp().getOptions().getDatabaseUrl());
    }
}
