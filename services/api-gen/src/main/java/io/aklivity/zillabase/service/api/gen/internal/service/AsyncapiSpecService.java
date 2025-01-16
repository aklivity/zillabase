package io.aklivity.zillabase.service.api.gen.internal.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiSpecRegisterResponse;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

@Service
public class AsyncapiSpecService
{
    private final ApiGenConfig config;
    private final WebClient webClient;

    public AsyncapiSpecService(
        ApiGenConfig config,
        WebClient webClient)
    {
        this.config = config;
        this.webClient = webClient;
    }

    public String register(
        String id,
        String spec) throws IOException
    {
        String newVersion = null;

        ResponseEntity<String> httpResponse = webClient.post()
            .uri(URI.create("http://localhost:%d".formatted(config.adminHttpPort())).resolve("/v1/asyncapis"))
            .header("Content-Type", "application/vnd.aai.asyncapi+yaml")
            .header("X-Registry-ArtifactId", id)
            .bodyValue(spec)
            .retrieve()
            .toEntity(String.class)
            .block();

        String response = httpResponse.getStatusCode().value() == 200 ? httpResponse.getBody() : null;

        if (response != null)
        {
            Jsonb jsonb = JsonbBuilder.newBuilder().build();
            AsyncapiSpecRegisterResponse register = jsonb.fromJson(response, AsyncapiSpecRegisterResponse.class);
            newVersion = register.id;
            System.out.println("Registered AsyncAPI spec: %s".formatted(register.id));
        }
        else
        {
            System.out.println("Error registering AsyncAPI spec");
        }

        return newVersion;
    }

     public String build(
        Info info,
        Components components,
        Map<String, Object> channels,
        Map<String, Object> operations,
        Map<String, Object> servers) throws JsonProcessingException
    {
        final AsyncAPI asyncAPI = new AsyncAPI();

        asyncAPI.setAsyncapi("3.0.0");
        asyncAPI.setInfo(info);
        asyncAPI.setServers(servers);
        asyncAPI.setComponents(components);
        asyncAPI.setChannels(channels);
        asyncAPI.setOperations(operations);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .setSerializationInclusion(NON_NULL);

        return mapper.writeValueAsString(asyncAPI);
    }
}
