package io.aklivity.zillabase.service.api.gen.internal.spec;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.Map;
import java.util.function.Function;


public final class AsyncapiSpecBuilder<T> extends SpecBuilder<T, AsyncapiSpecBuilder<T>>
{
    private final Function<AsyncapiSpec, T> mapper;

    private String asyncapi;
    private Info info;
    private Map<String, Object> servers;
    private Components components;
    private Map<String, Object> channels;
    private Map<String, Object> operations;
    private Map<String, Object> messages;
    private Map<String, Object> schemas;

    public AsyncapiSpecBuilder(Function<AsyncapiSpec, T> mapper)
    {
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<AsyncapiSpecBuilder<T>> thisType()
    {
        return (Class<AsyncapiSpecBuilder<T>>) getClass();
    }

    public AsyncapiSpecBuilder<T> asyncapi(
        String asyncapi)
    {
        this.asyncapi = asyncapi;
        return this;
    }

    public AsyncapiSpecBuilder<T> info(
        Info info)
    {
        this.info = info;
        return this;
    }

    public AsyncapiSpecBuilder<T> servers(
        Map<String, Object> servers)
    {
        this.servers = servers;
        return this;
    }

    public AsyncapiSpecBuilder<T> components(
        Components components)
    {
        this.components = components;
        return this;
    }

    public AsyncapiSpecBuilder<T> channels(
        Map<String, Object> channels)
    {
        this.channels = channels;
        return this;
    }

    public AsyncapiSpecBuilder<T> operations(
        Map<String, Object> operations)
    {
        this.operations = operations;
        return this;
    }

    public AsyncapiSpecBuilder<T> messages(
        Map<String, Object> messages)
    {
        this.messages = messages;
        return this;
    }

    public AsyncapiSpecBuilder<T> schemas(
        Map<String, Object> schemas)
    {
        this.schemas = schemas;
        return this;
    }

    @Override
    public T build()
    {
        AsyncapiSpec spec = new AsyncapiSpec(
            asyncapi,
            info,
            servers,
            components,
            channels,
            operations,
            messages,
            schemas
        );

        return mapper.apply(spec);
    }

    public String buildYaml() throws Exception
    {
        AsyncAPI asyncAPI = (AsyncAPI) build();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setSerializationInclusion(NON_NULL);

        return yamlMapper.writeValueAsString(asyncAPI);
    }
}
