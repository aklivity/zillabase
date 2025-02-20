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
package io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.Builder;

public class ZillaAsyncApiConfigBuilder<T> extends Builder<T, ZillaAsyncApiConfigBuilder<T>>
{
    private final Function<ZillaAsyncApiConfig, T> mapper;

    private String name;
    private Map<String, ZillaCatalogConfig> catalogs;
    private Map<String, ZillaGuardConfig> guards;
    private Map<String, ZillaBindingConfig> bindings;
    private Map<String, Object> telemetry;


    public ZillaAsyncApiConfigBuilder(Function<ZillaAsyncApiConfig, T> mapper)
    {
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<ZillaAsyncApiConfigBuilder<T>> thisType()
    {
        return (Class<ZillaAsyncApiConfigBuilder<T>>) getClass();
    }

    public ZillaAsyncApiConfigBuilder<T> name(
        String name)
    {
        this.name = name;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> catalogs(
        Map<String, ZillaCatalogConfig> catalogs)
    {
        this.catalogs = catalogs;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> guards(
        Map<String, ZillaGuardConfig> guards)
    {
        this.guards = guards;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> bindings(
        Map<String, ZillaBindingConfig> bindings)
    {
        this.bindings = bindings;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> telemetry(
        Map<String, Object> telemetry)
    {
        this.telemetry = telemetry;
        return this;
    }

    public ZillaAsyncApiConfigBuilder<T> addBinding(
        String name,
        ZillaBindingConfig binding)
    {
        if (bindings == null)
        {
            bindings = new HashMap<>();
        }

        bindings.put(name, binding);

        return this;
    }

    @Override
    public T build()
    {
        ZillaAsyncApiConfig zilla = new ZillaAsyncApiConfig(
            name,
            catalogs,
            guards,
            bindings,
            telemetry);

        return mapper.apply(zilla);
    }
}
