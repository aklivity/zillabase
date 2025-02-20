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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.Builder;

public class ZillaBindingConfigBuilder<T> extends Builder<T, ZillaBindingConfigBuilder<T>>
{
    private final Function<ZillaBindingConfig, T> mapper;

    private String type;
    private String kind;
    private ZillaBindingOptionsConfig options;
    private List<ZillaBindingRouteConfig> routes;
    private String exit;

    public ZillaBindingConfigBuilder(Function<ZillaBindingConfig, T> mapper)
    {
        this.mapper = mapper;
    }

        @Override
    @SuppressWarnings("unchecked")
    protected Class<ZillaBindingConfigBuilder<T>> thisType()
    {
        return (Class<ZillaBindingConfigBuilder<T>>) getClass();
    }

    public ZillaBindingConfigBuilder<T> type(
        String type)
    {
        this.type = type;
        return this;
    }

    public ZillaBindingConfigBuilder<T> kind(
        String kind)
    {
        this.kind = kind;
        return this;
    }

    public ZillaBindingConfigBuilder<T> options(
        ZillaBindingOptionsConfig options)
    {
        this.options = options;
        return this;
    }

    public ZillaBindingConfigBuilder<T> routes(
        List<ZillaBindingRouteConfig> routes)
    {
        this.routes = routes;
        return this;
    }

    public ZillaBindingConfigBuilder<T> exit(
        String exit)
    {
        this.exit = exit;
        return this;
    }

    public ZillaBindingConfigBuilder<T> addRoute(
        ZillaBindingRouteConfig route)
    {
        if (routes == null)
        {
            routes = new ArrayList<>();
        }
        routes.add(route);

        return this;
    }

    @Override
    public T build()
    {
        return mapper.apply(new ZillaBindingConfig(type, kind, options, routes, exit));
    }
}
