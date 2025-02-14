package io.aklivity.zillabase.service.api.gen.internal.builder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;

public class HttpAsyncApiBuilderTest {

    @Mock
    private ApiGenConfig config;

    @Mock
    private KafkaConfig kafkaConfig;

    @InjectMocks
    private HttpAsyncApiBuilder specBuilder;

    private String kafkaSpec;

    private String expectedHttpSpec;

    @BeforeEach
    public void setUp() throws URISyntaxException, IOException
    {
        MockitoAnnotations.initMocks(this);
        when(kafkaConfig.bootstrapServers()).thenReturn("localhost:9092");
        when(kafkaConfig.karapaceUrl()).thenReturn("http://karapace.zillabase.dev:8081");

        kafkaSpec = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(
            getClass().getClassLoader().getResource("specs/kafka-asyncapi.yaml")).toURI())), UTF_8);

        expectedHttpSpec = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(
            getClass().getClassLoader().getResource("specs/http-asyncapi.yaml")).toURI())), UTF_8);
    }

    @Test
    public void shouldBuildHttpAsyncapiSpec() throws Exception
    {
        String actualHttpSpec = specBuilder.buildSpec(kafkaSpec);

        assertEquals(expectedHttpSpec, actualHttpSpec);
    }
}
