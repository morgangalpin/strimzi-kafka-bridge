/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.bridge.http;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.strimzi.kafka.bridge.config.BridgeConfig;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConsumerGeneratedNameTest extends HttpBridgeTestBase {

    private String groupId = "my-group";
    private static String bridgeID = "";
    private String name = "";

    @BeforeAll
    static void unsetBridgeID() {
        bridgeID = config.get(BridgeConfig.BRIDGE_ID).toString();
        config.remove(BridgeConfig.BRIDGE_ID);

        bridgeConfig = BridgeConfig.fromMap(config);
        if (!"TRUE".equalsIgnoreCase(System.getenv("STRIMZI_USE_SYSTEM_BRIDGE"))) {
            httpBridge = new HttpBridge(bridgeConfig);
        }
    }

    @AfterAll
    static void revertUnsetBridgeID() {
        config.put(BridgeConfig.BRIDGE_ID, bridgeID);
    }

    @Test
    void createConsumerNameNotSet(VertxTestContext context) throws InterruptedException, ExecutionException, TimeoutException {
        JsonObject json = new JsonObject();

        CompletableFuture<Boolean> create = new CompletableFuture<>();
        consumerService()
            .createConsumerRequest(groupId, json)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(json, ar -> {
                    context.verify(() -> {
                        assertThat(ar.succeeded(), is(true));
                        HttpResponse<JsonObject> response = ar.result();
                        assertThat(response.statusCode(), is(HttpResponseStatus.OK.code()));
                        JsonObject bridgeResponse = response.body();
                        String consumerInstanceId = bridgeResponse.getString("instance_id");
                        name = consumerInstanceId;
                        assertThat(consumerInstanceId.startsWith("kafka-bridge-consumer-"), is(true));
                        create.complete(true);
                    });
                });
        create.get(TEST_TIMEOUT, TimeUnit.SECONDS);
        consumerService()
            .deleteConsumer(context, groupId, name);
        context.completeNow();
    }

    @Test
    void createConsumerNameSet(VertxTestContext context) throws InterruptedException, ExecutionException, TimeoutException {
        JsonObject json = new JsonObject()
                .put("name", "consumer-1")
                .put("format", "json");

        CompletableFuture<Boolean> create = new CompletableFuture<>();
        consumerService()
                .createConsumerRequest(groupId, json)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(json, ar -> {
                    context.verify(() -> {
                        assertThat(ar.succeeded(), is(true));
                        HttpResponse<JsonObject> response = ar.result();
                        assertThat(response.statusCode(), is(HttpResponseStatus.OK.code()));
                        JsonObject bridgeResponse = response.body();
                        String consumerInstanceId = bridgeResponse.getString("instance_id");
                        assertThat(consumerInstanceId, is("consumer-1"));
                        create.complete(true);
                    });
                });
        create.get(TEST_TIMEOUT, TimeUnit.SECONDS);
        consumerService()
            .deleteConsumer(context, groupId, "consumer-1");
        context.completeNow();
    }
}
