package io.aklivity.zillabase.service.api.gen.internal.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import reactor.core.publisher.Mono;

import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class AsyncapiSpecConfigServiceTest {

    @Mock
    private ApiGenConfig config;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AsyncapiSpecConfigService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.adminHttpPort()).thenReturn(8080);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn((RequestBodyUriSpec) requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testRegister() {
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
    void testBuild() throws JsonProcessingException {
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

    // Add more tests for fetchSpec, httpOperations, and publishConfig methods as necessary
}
