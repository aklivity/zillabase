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

import java.util.List;

public class ZillaBindingConfig
{
    public final String type;
    public final String kind;
    public final ZillaBindingOptionsConfig options;
    public final List<ZillaBindingRouteConfig> routes;
    public final String exit;

    public ZillaBindingConfig(
        String type,
        String kind,
        ZillaBindingOptionsConfig options,
        List<ZillaBindingRouteConfig> routes,
        String exit)
    {
        this.type = type;
        this.kind = kind;
        this.options = options;
        this.routes = routes;
        this.exit = exit;
    }

    public static ZillaBindingConfigBuilder<ZillaBindingConfig> builder()
    {
        return new ZillaBindingConfigBuilder<>(identity());
    }

    @SuppressWarnings("unchecked")
    private static <T> java.util.function.Function<ZillaBindingConfig, T> identity()
    {
        return config -> (T) config;
    }
}
