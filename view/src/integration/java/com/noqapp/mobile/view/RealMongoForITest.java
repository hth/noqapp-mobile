package com.noqapp.mobile.view;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * hitender
 * 12/7/17 5:56 AM
 */
public abstract class RealMongoForITest {
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

    @BeforeAll
    public void globalSetup() throws Exception {
        int port = Network.getFreeServerPort();

        mongodExecutable = starter.prepare(new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net("localhost", port, Network.localhostIsIPv6()))
                .build());
        mongodProcess = mongodExecutable.start();
        mongoClient = new MongoClient("localhost", port);
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

    public Mongo getMongo() {
        return mongoClient;
    }

    MongoTemplate getMongoTemplate() {
        if (null == mongoTemplate) {
            mongoTemplate = new MongoTemplate(mongoClient, DATABASE_NAME);
        }
        return mongoTemplate;
    }
}
