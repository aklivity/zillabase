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
package io.aklivity.zillabase.service.api.gen.internal.asyncapi;

import java.util.Map;

import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;

public class AsyncapiSpec
{
    public transient int id;

    public final String version;
    public final Info info;
    public final Map<String, Object> servers;
    public final Components components;
    public final Map<String, Object> channels;
    public final Map<String, Object> operations;

    AsyncapiSpec(
        String version,
        Info info,
        Map<String, Object> servers,
        Components components,
        Map<String, Object> channels,
        Map<String, Object> operations)
    {
        this.version = version;
        this.info = info;
        this.servers = servers;
        this.components = components;
        this.channels = channels;
        this.operations = operations;
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
