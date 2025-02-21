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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.yaml.snakeyaml.Yaml;

import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;

public class HttpAsyncApiGeneratorTest
{

    @Mock
    private ApiGenConfig config;

    @Mock
    private KafkaConfig kafkaConfig;

    @InjectMocks
    private HttpAsyncApiGenerator specBuilder;

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
        String actualHttpSpec = specBuilder.generate(kafkaSpec);

        Yaml yaml = new Yaml();
        Map<String, Object> expectedMap = yaml.load(expectedHttpSpec);
        Map<String, Object> actualMap = yaml.load(actualHttpSpec);

        assertThat(actualMap).isEqualTo(expectedMap);
    }
}
