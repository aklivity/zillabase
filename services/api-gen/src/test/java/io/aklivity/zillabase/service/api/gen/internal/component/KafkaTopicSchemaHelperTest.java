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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.List;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1)
public class KafkaTopicSchemaHelperTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private WebClient webClient;

    @Autowired
    private KafkaTopicSchemaHelper kafkaTopicSchemaService;

    @Autowired
    private AdminClient adminClient;

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldResolveTopicSchemas() throws Exception
    {
        String topicName = "test-topic";
        adminClient.createTopics(Collections.singletonList(new NewTopic(topicName, 1, (short) 1)));

        List<KafkaTopicSchemaRecord> records = kafkaTopicSchemaService.resolve();

        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(topicName, records.get(0).name);
    }

    @Test
    public void testExtractIdentityFieldFromProtobufSchema()
    {
        String schema = "syntax = \"proto3\"; message TestMessage { int32 id = 1; string name_identity = 2; }";
        String identityField = kafkaTopicSchemaService.extractIdentityFieldFromProtobufSchema(schema);
        assertEquals("name_identity", identityField);
    }

    @Test
    public void testExtractIdentityFieldFromSchema() throws JsonProcessingException
    {
        String schema = "{\"fields\": [{\"name\": \"id\", \"type\": \"int\"}," +
            " {\"name\": \"name_identity\", \"type\": \"string\"}]}";
        String identityField = kafkaTopicSchemaService.extractIdentityFieldFromSchema(schema);
        assertEquals("name_identity", identityField);
    }
}
