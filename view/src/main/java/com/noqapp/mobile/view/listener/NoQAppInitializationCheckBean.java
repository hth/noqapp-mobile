package com.noqapp.mobile.view.listener;

import com.maxmind.geoip2.DatabaseReader;
import com.noqapp.common.utils.CommonUtil;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.noqapp.common.config.FirebaseConfig;

import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
@PropertySource("classpath:build-info.properties")
public class NoQAppInitializationCheckBean {
    private static final Logger LOG = LoggerFactory.getLogger(NoQAppInitializationCheckBean.class);

    private Environment environment;
    private DataSource dataSource;
    private FirebaseConfig firebaseConfig;
    private RestHighLevelClient restHighLevelClient;
    private DatabaseReader databaseReader;

    @Autowired
    public NoQAppInitializationCheckBean(
            Environment environment,
            DataSource dataSource,
            FirebaseConfig firebaseConfig,
            RestHighLevelClient restHighLevelClient,
            DatabaseReader databaseReader
    ) {
        this.environment = environment;
        this.dataSource = dataSource;
        this.firebaseConfig = firebaseConfig;
        this.restHighLevelClient = restHighLevelClient;
        this.databaseReader = databaseReader;
    }

    @PostConstruct
    public void checkRDBConnection() throws SQLException {
        if (this.dataSource.getConnection().isClosed()) {
            LOG.error("RBS on Mobile could not be connected");
            throw new RuntimeException("RDB on Mobile could not be connected");
        }
        LOG.info("RDB on Mobile connected");
    }
    
    @PostConstruct
    public void checkFirebaseConnection() {
        if (null == firebaseConfig.getFirebaseAuth()) {
            LOG.error("Firebase on Mobile could not be connected");
            throw new RuntimeException("Firebase on Mobile could not be connected");
        }
        LOG.info("Firebase on Mobile connected");
    }

    @PostConstruct
    public void checkElasticConnection() {
        try {
            if (!restHighLevelClient.ping(CommonUtil.getMeSomeHeader())) {
                LOG.error("Elastic on Mobile could not be connected");
                throw new RuntimeException("Elastic on Mobile could not be connected");
            }

            MainResponse mainResponse = restHighLevelClient.info(CommonUtil.getMeSomeHeader());
            LOG.info("Elastic on Mobile {} connected clusterName={} nodeName={}\n  build={}\n  clusterUuid={}\n",
                    mainResponse.getVersion(),
                    mainResponse.getClusterName(),
                    mainResponse.getNodeName(),
                    mainResponse.getBuild(),
                    mainResponse.getClusterUuid());
        } catch (IOException e) {
            LOG.error("Elastic on Mobile could not be connected");
            throw new RuntimeException("Elastic on Mobile could not be connected");
        }
    }

    @PostConstruct
    public void checkGeoLite() {
        LOG.info("{} major={} minor={}\n  buildDate={}\n  ipVersion={}\n ",
                databaseReader.getMetadata().getDatabaseType(),
                databaseReader.getMetadata().getBinaryFormatMajorVersion(),
                databaseReader.getMetadata().getBinaryFormatMinorVersion(),
                databaseReader.getMetadata().getBuildDate(),
                databaseReader.getMetadata().getIpVersion());
    }

    @PreDestroy
    public void applicationDestroy() {
        LOG.info("Stopping Mobile Server for environment={}", environment.getProperty("build.env"));
        LOG.info("****************** STOPPED ******************");
    }
}
