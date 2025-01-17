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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.server.Server;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import reactor.core.publisher.Mono;

public class AsyncapiSpecConfigServiceTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private AsyncapiSpecConfigService specConfigService;

    private final String asyncapiUrl = "http://localhost:8080/v1/asyncapis";

    @BeforeEach
    public void setup()
    {
        MockitoAnnotations.openMocks(this);
        when(config.adminHttpUrl()).thenReturn(asyncapiUrl);
    }

    @Test
    public void shouldRegisterSpec()
    {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String body = "test-spec";

        when(webClient.post())
            .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(URI.create(asyncapiUrl)))
            .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(any(), any()))
            .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(body))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve())
            .thenReturn(responseSpec);

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"id\":\"new-version-123\"}", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(Mono.just(responseEntity));

        String result = specConfigService.register("test-id", body);

        assertNotNull(result);
        assertEquals("new-version-123", result);
    }

    @Test
    public void shouldBuildSpec() throws JsonProcessingException
    {
        Info info = new Info();
        info.setTitle("Test API");
        info.setVersion("1.0.0");

        Components components = new Components();
        Map<String, Object> channels = new HashMap<>();
        channels.put("channel1", new Channel());

        Map<String, Object> operations = new HashMap<>();
        operations.put("operation1", new Operation());

        Map<String, Object> servers = new HashMap<>();
        servers.put("server1", new Server());

        String yamlResult = specConfigService.build(info, components, channels, operations, servers);

        assertNotNull(yamlResult);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        AsyncAPI result = mapper.readValue(yamlResult, AsyncAPI.class);

        assertNotNull(result);
        assertNotNull(result.getInfo());
        assertNotNull(result.getServers());
        assertNotNull(result.getChannels());
        assertNotNull(result.getOperations());
        assertEquals("3.0.0", result.getAsyncapi());
        assertEquals("Test API", result.getInfo().getTitle());
        assertEquals("1.0.0", result.getInfo().getVersion());
    }

    @Test
    public void shouldFetchSpec()
    {
        String artifactId = "test-artifact";
        String version = "1.0.0";
        String expectedResponse = "asyncapi-spec-content";

        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get())
            .thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class)))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve())
            .thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.just(expectedResponse));

        String result = specConfigService.fetchSpec(artifactId, version);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
    }

    @Test
    public void shouldReturnHttpOperations()
    {
        String httpSpec = """
                {
                    "operations": {
                        "operation1": {},
                        "operation2": {}
                    }
                }
                """;


        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(httpSpec));

        List<String> result = specConfigService.httpOperations("1.0.0");

        assertEquals(2, result.size());
        assertEquals("operation1", result.get(0));
        assertEquals("operation2", result.get(1));
    }
}
