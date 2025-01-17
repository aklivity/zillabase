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
package io.aklivity.zillabase.service.api.gen.internal.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import reactor.core.publisher.Mono;

@SpringBootTest
public class AsyncapiSpecConfigServiceTest
{

    @Mock
    private ApiGenConfig config;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AsyncapiSpecConfigService service;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        when(config.adminHttpPort()).thenReturn(8080);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn((RequestBodyUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void shouldRegisterSpec()
    {
        String id = "testId";
        String spec = "testSpec";
        String expectedResponse = "{ \"id\": \"newVersion\" }";
        when(responseSpec.toEntity(String.class)).thenReturn(Mono.just(ResponseEntity.ok(expectedResponse)));

        String newVersion = service.register(id, spec);

        assertEquals("newVersion", newVersion);
        verify(requestBodyUriSpec).uri("http://localhost:8080/v1/asyncapis");
        verify(requestHeadersUriSpec).retrieve();
    }

    @Test
    void shouldBuildSpec() throws JsonProcessingException
    {
        Info info = new Info();
        Components components = new Components();
        Map<String, Object> channels = new HashMap<>();
        Map<String, Object> operations = new HashMap<>();
        Map<String, Object> servers = new HashMap<>();

        String yaml = service.build(info, components, channels, operations, servers);

        AsyncAPI asyncAPI = new AsyncAPI();
        asyncAPI.setAsyncapi("3.0.0");
        asyncAPI.setInfo(info);
        asyncAPI.setComponents(components);
        asyncAPI.setChannels(channels);
        asyncAPI.setOperations(operations);
        asyncAPI.setServers(servers);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).setSerializationInclusion(NON_NULL);
        String expectedYaml = mapper.writeValueAsString(asyncAPI);

        assertEquals(expectedYaml, yaml);
    }
}
