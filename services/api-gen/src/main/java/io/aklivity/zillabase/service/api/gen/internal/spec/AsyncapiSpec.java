package io.aklivity.zillabase.service.api.gen.internal.spec;

import java.util.Map;

import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;

public class AsyncapiSpec
{
    public static final String YAML = "yaml";

    public transient int id;

    public final String asyncapi; // e.g. "3.0.0"
    public final Info info;
    public final Map<String, Object> servers;
    public final Components components;
    public final Map<String, Object> channels;
    public final Map<String, Object> operations;
    public final Map<String, Object> messages;
    public final Map<String, Object> schemas;

    AsyncapiSpec(
        String asyncapi,
        Info info,
        Map<String, Object> servers,
        Components components,
        Map<String, Object> channels,
        Map<String, Object> operations,
        Map<String, Object> messages,
        Map<String, Object> schemas)
    {
        this.asyncapi = asyncapi;
        this.info = info;
        this.servers = servers;
        this.components = components;
        this.channels = channels;
        this.operations = operations;
        this.messages = messages;
        this.schemas = schemas;
    }

    public static AsyncapiSpecBuilder<AsyncapiSpec> builder()
    {
        return new AsyncapiSpecBuilder<>(identity());
    }

    @SuppressWarnings("unchecked")
    private static <T> java.util.function.Function<AsyncapiSpec, T> identity()
    {
        return spec -> (T) spec;
    }
}
