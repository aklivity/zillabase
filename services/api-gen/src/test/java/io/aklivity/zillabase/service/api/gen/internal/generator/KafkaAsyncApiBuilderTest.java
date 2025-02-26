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
package io.aklivity.zillabase.service.api.gen.internal.generator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;

public class KafkaAsyncApiBuilderTest
{

    @Mock
    private ApiGenConfig config;

    @Mock
    private KafkaConfig kafkaConfig;

    @InjectMocks
    private KafkaAsyncApiGenerator kafkaAsyncApiGenerator;

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(kafkaConfig.bootstrapServers()).thenReturn("localhost:9092");
        when(kafkaConfig.karapaceUrl()).thenReturn("http://karapace.zillabase.dev:8081");
    }

    @Test
    public void shouldBuildSpecWithSingleTopic() throws Exception
    {
        List<KafkaTopicSchemaRecord> schemaRecords = List.of(
            new KafkaTopicSchemaRecord(
                "public.test-topic",
                List.of("delete"),
                "TestTopic",
                "test-topic-value",
                "record",
                "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                " {\"name\":\"name\",\"type\":\"string\"}]}")
        );

        String result = kafkaAsyncApiGenerator.generate(schemaRecords);

        assertTrue(result.contains("public.test-topic"));
        assertTrue(result.contains("User"));
    }

    @Test
    public void shouldBuildSpecWithMultipleTopics() throws Exception
    {
        List<KafkaTopicSchemaRecord> schemaRecords = List.of(
            new KafkaTopicSchemaRecord(
                "public.test-topic-1",
                List.of("delete"),
                "TestTopic1",
                "test-topic-value-1",
                "record",
                "{\"type\":\"record\",\"name\":\"User1\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                " {\"name\":\"name\",\"type\":\"string\"}]}"),
            new KafkaTopicSchemaRecord(
                "public.test-topic-2",
                List.of("delete"),
                "TestTopic2",
                "test-topic-value-2",
                "record",
                "{\"type\":\"record\",\"name\":\"User2\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                " {\"name\":\"name\",\"type\":\"string\"}]}")
        );

        String result = kafkaAsyncApiGenerator.generate(schemaRecords);

        assertTrue(result.contains("public.test-topic-1"));
        assertTrue(result.contains("User1"));
        assertTrue(result.contains("public.test-topic-2"));
        assertTrue(result.contains("User2"));
    }

    @Test
    public void shouldRejectInvalidSchema() throws Exception
    {
        List<KafkaTopicSchemaRecord> schemaRecords = List.of(
            new KafkaTopicSchemaRecord(
                "invalid-topic",
                List.of("delete"),
                "InvalidTopic",
                "invalid-topic-value",
                "record",
                "{\"type\":\"invalid\",\"name\":\"InvalidUser\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                " {\"name\":\"name\",\"type\":\"string\"}]}")
        );

        String result = kafkaAsyncApiGenerator.generate(schemaRecords);

        assertTrue(result.contains("invalid-topic"));
    }

    @Test
    public void shouldBuildSpecWithComplexSchema() throws Exception
    {
        List<KafkaTopicSchemaRecord> schemaRecords = List.of(
            new KafkaTopicSchemaRecord(
                "complex-topic",
                List.of("compact"),
                "ComplexTopic",
                "complex-topic-value",
                "record",
                "{\"type\":\"record\",\"name\":\"ComplexUser\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                "{\"name\":\"details\",\"type\":{\"type\":\"record\",\"name\":\"Details\",\"fields\":[{\"name\":\"age\"," +
                    "\"type\":\"int\"},{\"name\":\"address\",\"type\":\"string\"}]}}]}")
        );

        String result = kafkaAsyncApiGenerator.generate(schemaRecords);

        assertTrue(result.contains("complex-topic"));
        assertTrue(result.contains("ComplexUser"));
        assertTrue(result.contains("Details"));
    }
}
