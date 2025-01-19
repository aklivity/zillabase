/*
 * Copyright 2024 Aklivity Inc
 *
 * Licensed under the Aklivity Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 *   https://www.aklivity.io/aklivity-community-license/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.aklivity.zillabase.service.api.gen.internal.component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import reactor.core.publisher.Mono;

public class KafkaTopicSchemaHelperTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private WebClient webClient;

    @Mock
    private AdminClient adminClient;

    @InjectMocks
    private KafkaTopicSchemaHelper kafkaTopicSchemaHelper;

    private String schema;

    @BeforeEach
    public void setUp() throws URISyntaxException, IOException
    {
        MockitoAnnotations.initMocks(this);
        when(config.karapaceUrl()).thenReturn("http://localhost:8081");

        schema = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(
            getClass().getClassLoader().getResource("schemas/pet.json")).toURI())), UTF_8);
    }

    @Test
    public void shouldResolveTopicSchemas()
        throws ExecutionException, InterruptedException, JsonProcessingException
    {
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String expectedResponse = schema;

        when(webClient.get())
            .thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class)))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve())
            .thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.just(expectedResponse));

        TopicListing topicListing = mock(TopicListing.class);
        when(topicListing.name()).thenReturn("test-topic");
        when(topicListing.isInternal()).thenReturn(false);

        KafkaFuture<List<TopicListing>> future = KafkaFuture.completedFuture(Collections.singletonList(topicListing));
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
        when(listTopicsResult.listings()).thenReturn((KafkaFuture) future);

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, "test-topic");
        Config config = new Config(List.of(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")));

        DescribeConfigsResult describeConfigsResult = mock(DescribeConfigsResult.class);
        when(describeConfigsResult.all()).thenReturn(KafkaFuture.completedFuture(Map.of(resource, config)));
        when(adminClient.describeConfigs(List.of(resource))).thenReturn(describeConfigsResult);

        List<KafkaTopicSchemaRecord> records = kafkaTopicSchemaHelper.resolve();

        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals("test-topic", records.get(0).name);
    }

    @Test
    public void testExtractIdentityFieldFromProtobufSchema()
    {
        String schema = "syntax = \"proto3\"; message TestMessage { int32 id = 1; string name_identity = 2; }";
        String identityField = kafkaTopicSchemaHelper.extractIdentityFieldFromProtobufSchema(schema);
        assertEquals("name_identity", identityField);
    }

    @Test
    public void testExtractIdentityFieldFromSchema() throws JsonProcessingException
    {
        String schema = "{\"fields\": [{\"name\": \"id\", \"type\": \"int\"}," +
            " {\"name\": \"name_identity\", \"type\": \"string\"}]}";
        String identityField = kafkaTopicSchemaHelper.extractIdentityFieldFromSchema(schema);
        assertEquals("name_identity", identityField);
    }
}
