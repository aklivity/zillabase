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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import reactor.core.publisher.Mono;

public class ApicurioHelperTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private WebClient webClient;

    @InjectMocks
    private ApicurioHelper specConfigService;

    @BeforeEach
    public void setup()
    {
        MockitoAnnotations.openMocks(this);
        when(config.apicurioUrl()).thenReturn("http://localhost:8080/");
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
        when(requestBodyUriSpec.uri(any(URI.class)))
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
    public void shouldPublishSpec()
    {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        String body = "test-spec";

        when(webClient.post())
            .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class)))
            .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(any(), any()))
            .thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(body))
            .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve())
            .thenReturn(responseSpec);

        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"version\":\"new-version-123\"}", HttpStatus.OK);
        when(responseSpec.toEntity(String.class)).thenReturn(Mono.just(responseEntity));

        String result = specConfigService.publishSpec("test-id", body);

        assertNotNull(result);
        assertEquals("new-version-123", result);
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
    public void shouldReturnHttpOperations() throws JsonProcessingException
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
