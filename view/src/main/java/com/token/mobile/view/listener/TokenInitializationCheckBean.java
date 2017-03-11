package com.token.mobile.view.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
public class TokenInitializationCheckBean {

    private static final Logger LOG = LoggerFactory.getLogger(TokenInitializationCheckBean.class);

    private DataSource dataSource;

    @Autowired
    public TokenInitializationCheckBean(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void checkRDBConnection() throws SQLException {
        if (this.dataSource.getConnection().isClosed()) {
            LOG.error("RBS could not be connected");
            throw new RuntimeException("RDBS could not be connected");
        }
        LOG.info("RDBS connected");
    }
}
