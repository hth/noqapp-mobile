package com.noqapp.mobile.view.listener;

import com.noqapp.common.config.FirebaseConfig;
import com.noqapp.search.elastic.domain.BizStoreElastic;
import com.noqapp.search.elastic.service.ElasticAdministrationService;
import com.noqapp.service.payment.PaymentGatewayService;

import com.maxmind.geoip2.DatabaseReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.MainResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

/**
 * User: hitender
 * Date: 11/17/16 4:19 PM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
@Component
@PropertySource("classpath:build-info.properties")
public class NoQueueInitializationCheckBean {
    private static final Logger LOG = LoggerFactory.getLogger(NoQueueInitializationCheckBean.class);

    private DataSource dataSource;
    private FirebaseConfig firebaseConfig;
    private RestHighLevelClient restHighLevelClient;
    private ElasticAdministrationService elasticAdministrationService;
    private JmsTemplate jmsMailSignUpTemplate;
    private DatabaseReader databaseReader;
    private PaymentGatewayService paymentGatewayService;

    private String buildEnvironment;

    @Autowired
    public NoQueueInitializationCheckBean(
        Environment environment,
        DataSource dataSource,
        FirebaseConfig firebaseConfig,
        RestHighLevelClient restHighLevelClient,
        ElasticAdministrationService elasticAdministrationService,

        @Qualifier("jmsMailSignUpTemplate")
        JmsTemplate jmsMailSignUpTemplate,
        DatabaseReader databaseReader,
        PaymentGatewayService paymentGatewayService
    ) {
        this.buildEnvironment = environment.getProperty("build.env");

        this.dataSource = dataSource;
        this.firebaseConfig = firebaseConfig;
        this.restHighLevelClient = restHighLevelClient;
        this.elasticAdministrationService = elasticAdministrationService;
        this.jmsMailSignUpTemplate = jmsMailSignUpTemplate;
        this.databaseReader = databaseReader;
        this.paymentGatewayService = paymentGatewayService;
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
            if (!restHighLevelClient.ping(RequestOptions.DEFAULT)) {
                LOG.error("Elastic on Mobile could not be connected");
                throw new RuntimeException("Elastic on Mobile could not be connected");
            }

            MainResponse mainResponse = restHighLevelClient.info(RequestOptions.DEFAULT);
            LOG.info("Elastic connected clusterName={} nodeName={}\n  build={}\n  clusterUuid={}\n  luceneVersion={}\n",
                mainResponse.getClusterName(),
                mainResponse.getNodeName(),
                mainResponse.getVersion().getNumber(),
                mainResponse.getClusterUuid(),
                mainResponse.getVersion().getLuceneVersion());
        } catch (IOException e) {
            LOG.error("Elastic on Mobile could not be connected");
            throw new RuntimeException("Elastic on Mobile could not be connected");
        }
    }

    @PostConstruct
    public void checkElasticIndex() {
        if (!elasticAdministrationService.doesIndexExists(BizStoreElastic.INDEX)) {
            LOG.warn("Elastic Index not found {}", BizStoreElastic.INDEX);
        } else {
            LOG.info("Elastic Index={} found", BizStoreElastic.INDEX);
        }
    }

    @PostConstruct
    public void checkJMS() {
        if (Objects.requireNonNull(jmsMailSignUpTemplate.getDefaultDestinationName()).endsWith("jms.mail.signup")) {
            LOG.info("ActiveMQ JMS running destinationName={}", jmsMailSignUpTemplate.getDefaultDestinationName());
        } else {
            LOG.error("Failed connecting ActiveMQ JMS");
            throw new RuntimeException("ActiveMQ could not be connected");
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

    @PostConstruct
    public void checkPaymentGateway() {
        boolean cashfreeSuccess = paymentGatewayService.verifyCashfree();
        if (!cashfreeSuccess) {
            LOG.error("Cashfree Payment Gateway could not be verified");
            throw new RuntimeException("Cashfree Payment Gateway could not be verified");
        }
    }

    @PreDestroy
    public void applicationDestroy() {
        LOG.info("Stopping Mobile Server for environment={}", buildEnvironment);
        LOG.info("****************** STOPPED ******************");
    }
}
