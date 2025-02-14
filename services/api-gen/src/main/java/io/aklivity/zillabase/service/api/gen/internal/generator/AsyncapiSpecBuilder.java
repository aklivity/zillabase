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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.operation.Operation;

public final class AsyncapiSpecBuilder<T> extends SpecBuilder<T, AsyncapiSpecBuilder<T>>
{
    private final Function<AsyncapiSpec, T> mapper;

    private String asyncapi;
    private Info info;
    private Map<String, Object> servers;
    private Components components;
    private Map<String, Object> channels;
    private Map<String, Object> operations;

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

    public AsyncapiSpecBuilder<T> addChannel(
        String name,
        Channel channel)
    {
        if (channels == null)
        {
            channels = new HashMap<>();
        }

        channels.put(name, channel);

        return this;
    }

    public AsyncapiSpecBuilder<T> addOperation(
        String name,
        Operation operation)
    {
        if (operations == null)
        {
            operations = new HashMap<>();
        }

        operations.put(name, operation);

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
            operations
        );

        return mapper.apply(spec);
    }
}
