package com.noqapp.mobile.view;

import com.mongodb.BasicDBList;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongoCmdOptionsBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.distribution.GenericVersion;
import de.flapdoodle.embed.process.runtime.Network;
import org.bson.Document;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * hitender
 * 11/6/18 2:46 PM
 * https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/257
 * https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/blob/master/src/main/java/de/flapdoodle/embed/mongo/tests/MongosSystemForTestFactory.java
 */
public class HackyBit {

    private MongodExecutable node1MongodExe;
    private MongodProcess node1Mongod;
    private MongodExecutable node2MongodExe;
    private MongodProcess node2Mongod;
    private MongodExecutable node3MongodExe;
    private MongodProcess node3Mongod;
    private MongoClient mongo;

    private IFeatureAwareVersion version = de.flapdoodle.embed.mongo.distribution.Versions.withFeatures(
        new GenericVersion("4.0.3"),
        Version.Main.PRODUCTION.getFeatures());

    @Test
    public void testSmth() throws IOException {
        MongodStarter runtime = MongodStarter.getDefaultInstance();
        int node1Port = Network.getFreeServerPort();
        int node2Port = Network.getFreeServerPort();
        int node3Port = Network.getFreeServerPort();

        try {
            List<ServerAddress> serverAddresses = new ArrayList<ServerAddress>() {{
                add(new ServerAddress(Network.getLocalHost(), node1Port));
                add(new ServerAddress(Network.getLocalHost(), node2Port));
                add(new ServerAddress(Network.getLocalHost(), node3Port));
            }};

            Net net1 = new Net(Network.getLocalHost().getHostAddress(), node1Port, Network.localhostIsIPv6());
            Net net2 = new Net(Network.getLocalHost().getHostAddress(), node2Port, Network.localhostIsIPv6());
            Net net3 = new Net(Network.getLocalHost().getHostAddress(), node3Port, Network.localhostIsIPv6());

            node1MongodExe = runtime.prepare(new MongodConfigBuilder()
                .version(version)
                .withLaunchArgument("--replSet", "rs0")
                .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
                .net(net1)
                .build());
            node1Mongod = node1MongodExe.start();

            node2MongodExe = runtime.prepare(new MongodConfigBuilder()
                .version(version)
                .withLaunchArgument("--replSet", "rs0")
                .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
                .net(net2)
                .build());
            node2Mongod = node2MongodExe.start();

            node3MongodExe = runtime.prepare(new MongodConfigBuilder()
                .version(version)
                .withLaunchArgument("--replSet", "rs0")
                .cmdOptions(new MongoCmdOptionsBuilder().useNoJournal(false).build())
                .net(net3)
                .build());
            node3Mongod = node3MongodExe.start();
            mongo = new MongoClient(serverAddresses);

            MongoDatabase adminDatabase = mongo.getDatabase("admin");

            Document config = new Document("_id", "rs0");
            BasicDBList members = new BasicDBList();
            members.add(new Document("_id", 0).append("host", "localhost:" + node1Port));
            members.add(new Document("_id", 1).append("host", "localhost:" + node2Port));
            members.add(new Document("_id", 2).append("host", "localhost:" + node3Port));
            config.put("members", members);

            adminDatabase.runCommand(new Document("replSetInitiate", config));
            System.out.println(">>>>>>>>" + adminDatabase.runCommand(new Document("replSetGetStatus", 1)));

            MongoDatabase funDb = mongo.getDatabase("fun");
            MongoCollection<Document> testCollection = funDb.getCollection("test");

            System.out.println(">>>>>>>> inserting data");
            testCollection.insertOne(new Document("fancy", "value"));
            System.out.println(">>>>>>>> finding data");
            //assertThat(testCollection.find().first().get("fancy")).isEqualTo("value");
        } finally {
            System.out.println(">>>>>> shutting down");
            if (mongo != null) {
                mongo.close();
            }
            if (node1Mongod != null) {
                node1MongodExe.stop();
            }
            if (node1Mongod != null) {
                node1Mongod.stop();
            }
            if (node2Mongod != null) {
                node2MongodExe.stop();
            }
            if (node2Mongod != null) {
                node2Mongod.stop();
            }
            if (node3Mongod != null) {
                node3MongodExe.stop();
            }
            if (node3Mongod != null) {
                node3Mongod.stop();
            }
        }
    }
}