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

import java.util.function.Function;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.Builder;

public class ZillaBindingRouteConfigBuilder <T> extends Builder<T, ZillaBindingRouteConfigBuilder<T>>
{
    private final Function<ZillaBindingRouteConfig, T> mapper;

    private Object when;
    private Object with;
    private String exit;

    public ZillaBindingRouteConfigBuilder(Function<ZillaBindingRouteConfig, T> mapper)
    {
        this.mapper = mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<ZillaBindingRouteConfigBuilder<T>> thisType()
    {
        return (Class<ZillaBindingRouteConfigBuilder<T>>) getClass();
    }

    public ZillaBindingRouteConfigBuilder<T> when(
        Object when)
    {
        this.when = when;
        return this;
    }

    public ZillaBindingRouteConfigBuilder<T> with(
        Object with)
    {
        this.with = with;
        return this;
    }

    public ZillaBindingRouteConfigBuilder<T> exit(
        String exit)
    {
        this.exit = exit;
        return this;
    }


    @Override
    public T build()
    {
        ZillaBindingRouteConfig route = new ZillaBindingRouteConfig(
            when,
            with,
            exit);

        return mapper.apply(route);
    }
}
