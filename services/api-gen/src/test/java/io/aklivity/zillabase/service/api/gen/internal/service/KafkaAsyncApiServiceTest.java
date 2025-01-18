package io.aklivity.zillabase.service.api.gen.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.component.AsyncapiSpecConfigHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

public class KafkaAsyncApiServiceTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private KafkaTopicSchemaHelper kafkaHelper;

    @Mock
    private AsyncapiSpecConfigHelper specHelper;

    @InjectMocks
    private KafkaAsyncApiService kafkaAsyncApiService;

    private String kafkaSpec;

    @BeforeEach
    public void setUp() throws URISyntaxException, IOException
    {
        MockitoAnnotations.initMocks(this);
        when(config.kafkaBootstrapServers()).thenReturn("localhost:9092");
        when(config.risingwaveDb()).thenReturn("dev");

        kafkaSpec = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(
            getClass().getClassLoader().getResource("kafkaSpec.json")).toURI())), StandardCharsets.UTF_8);
    }

    @Test
    public void shouldGenerateKafkaAsyncApiEvent() throws Exception
    {
        List<KafkaTopicSchemaRecord> schemaRecords = List.of(
            new KafkaTopicSchemaRecord(
                "test-topic",
                new String[]{"delete"},
                "TestTopic",
                "test-topic-value",
                "record",
                "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                    " {\"name\":\"name\",\"type\":\"string\"}]}")
        );
        String kafkaSpec = "{}";
        String specVersion = "1";

        when(kafkaHelper.resolve()).thenReturn(schemaRecords);
        when(specHelper.build(any(), any(), any(), any(), any())).thenReturn(kafkaSpec);
        when(specHelper.register(anyString(), anyString())).thenReturn(specVersion);

        ApiGenEvent inputEvent = new ApiGenEvent(ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, null, null);

        ApiGenEvent resultEvent = kafkaAsyncApiService.generate(inputEvent);

        assertEquals(ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, resultEvent.type());
        assertEquals(specVersion, resultEvent.kafkaVersion());

        verify(kafkaHelper, times(1)).resolve();
        verify(specHelper, times(1)).register(anyString(), anyString());
    }
}
