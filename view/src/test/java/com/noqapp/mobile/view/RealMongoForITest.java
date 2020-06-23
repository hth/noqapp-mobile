package com.noqapp.mobile.view;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.apache.commons.lang3.StringUtils;

import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * hitender
 * 12/7/17 5:56 AM
 */
public abstract class RealMongoForITest extends ElasticForITest {
    /**
     * please store Starter or RuntimeConfig in a static final field
     * if you want to use artifact store caching (or else disable caching)
     */
    private static final MongodStarter starter = MongodStarter.getDefaultInstance();

    private MongodExecutable mongodExecutable;
    private MongodProcess mongodProcess;

    private MongoClient mongoClient;
    private MongoTemplate mongoTemplate;

    private static final String DATABASE_NAME = "noqapp-i-test";
    int port;

    @BeforeAll
    public void globalSetup() throws Exception {
        port = Network.getFreeServerPort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net("localhost", port, Network.localhostIsIPv6()))
                .build());
        mongodProcess = mongodExecutable.start();
        mongoClient = MongoClients.create(populateMongoClientSettings());
    }

    private MongoClientSettings populateMongoClientSettings() {
        MongoClientSettings settings = MongoClientSettings.builder()
            .applicationName("NoQueue Test")
            .applyToClusterSettings(builder -> builder.hosts(getMongoSeeds()))
            .build();

        return settings;
    }

    private List<ServerAddress> getMongoSeeds() {
        List<ServerAddress> serverAddresses = new ArrayList<>();
        String mongoReplicaSet = "localhost:" + port;

        if (StringUtils.isNotBlank(mongoReplicaSet)) {
            String[] mongoInternetAddresses = mongoReplicaSet.split(",");
            for (String mongoInternetAddress : mongoInternetAddresses) {
                String[] mongoIpAndPort = mongoInternetAddress.split(":");
                ServerAddress serverAddress = new ServerAddress(mongoIpAndPort[0], Integer.parseInt(mongoIpAndPort[1]));
                serverAddresses.add(serverAddress);
            }
        } else {
            throw new RuntimeException("No Mongo Seed(s) listed");
        }

        return serverAddresses;
    }

    @AfterAll
    public void tearDown() {
        try {
            mongodProcess.stop();
            mongodExecutable.stop();
        } catch (Exception e) {
            //Normally it fails to stop. Throws exception. So just catch and do nothing.
        }
    }

    public MongoClient getMongo() {
        return mongoClient;
    }

    MongoTemplate getMongoTemplate() {
        if (null == mongoTemplate) {
            mongoTemplate = new MongoTemplate(mongoClient, DATABASE_NAME);
        }
        return mongoTemplate;
    }

    MongoTransactionManager transactionManager() {
        return new MongoTransactionManager(mongoTemplate.getMongoDbFactory());
    }
}
