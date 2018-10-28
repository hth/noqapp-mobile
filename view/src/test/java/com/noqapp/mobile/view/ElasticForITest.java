package com.noqapp.mobile.view;

import static org.junit.Assert.assertEquals;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * hitender
 * 10/27/18 3:54 PM
 */
public abstract class ElasticForITest {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticForITest.class);
    protected final static int HTTP_TEST_PORT = 9200;
    protected static RestClient restClient;
    protected static RestClientBuilder restClientBuilder;
    protected static RestHighLevelClient restHighLevelClient;

    @BeforeAll
    public static void startRestClient() {
        restClientBuilder = RestClient.builder(new HttpHost("localhost", HTTP_TEST_PORT));
        restHighLevelClient = new RestHighLevelClient(restClientBuilder);
        restClient = restClientBuilder.build();
        try {
            /* Intentionally delete all existing indices instead of ElasticsearchClientConfiguration.INDEX */
            restClient.performRequest(new Request("DELETE", "/*"));
            Response response = restClient.performRequest(new Request("GET", "/"));
            assertEquals(response.getStatusLine().getStatusCode(), 200);
            String text = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8.name());
            JSONObject jsonObj = new JSONObject(text);
            assertEquals(jsonObj.getString("tagline"), "You Know, for Search");
            LOG.info("Integration tests ready to start... Cluster is running.");
        } catch (IOException e) {
            // If we have an exception here, let's ignore the test
            LOG.warn("Integration tests are skipped: [{}]", e.getMessage());
            LOG.error("Full error is", e);
            LOG.error("Something wrong is happening. REST Client seemed to raise an exception.");
        }
    }

    @AfterAll
    public static void stopRestClient() throws IOException {
        if (restClient != null) {
            restClient.close();
            restClient = null;
        }
        LOG.info("Stopping integration tests against an external cluster");
    }
}
