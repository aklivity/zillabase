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

public class ZillaBindingRouteConfig
{
    public final Object when;
    public final Object with;
    public final String exit;

    public ZillaBindingRouteConfig(
        Object when,
        Object with,
        String exit)
    {
        this.when = when;
        this.with = with;
        this.exit = exit;
    }

    public static ZillaBindingRouteConfigBuilder<ZillaBindingRouteConfig> builder()
    {
        return new ZillaBindingRouteConfigBuilder<>(identity());
    }

    @SuppressWarnings("unchecked")
    private static <T> java.util.function.Function<ZillaBindingRouteConfig, T> identity()
    {
        return config -> (T) config;
    }
}
