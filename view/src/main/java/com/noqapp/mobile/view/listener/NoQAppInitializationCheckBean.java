package com.noqapp.mobile.view.listener;

import com.noqapp.utils.CommonUtil;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.noqapp.service.config.FirebaseConfig;

import java.io.IOException;
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
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    public NoQAppInitializationCheckBean(
            DataSource dataSource,
            FirebaseConfig firebaseConfig,
            RestHighLevelClient restHighLevelClient
    ) {
        this.dataSource = dataSource;
        this.firebaseConfig = firebaseConfig;
        this.restHighLevelClient = restHighLevelClient;
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
        LOG.info("Firebase connected");
    }

    @PostConstruct
    public void checkElasticConnection() {
        try {
            if (!restHighLevelClient.ping(CommonUtil.getMeSomeHeader())) {
                LOG.error("Elastic could not be connected");
                throw new RuntimeException("Elastic could not be connected");
            }

            MainResponse mainResponse = restHighLevelClient.info(CommonUtil.getMeSomeHeader());
            LOG.info("Elastic {} connected clusterName={} nodeName={}\n  build={}\n  clusterUuid={}\n",
                    mainResponse.getVersion(),
                    mainResponse.getClusterName(),
                    mainResponse.getNodeName(),
                    mainResponse.getBuild(),
                    mainResponse.getClusterUuid());
        } catch (IOException e) {
            LOG.error("Elastic could not be connected");
            throw new RuntimeException("Elastic could not be connected");
        }
    }
}
