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

import java.util.Map;

public class ZillaAsyncApiConfig
{
    public final String name;
    public final Map<String, ZillaCatalogConfig> catalogs;
    public final Map<String, ZillaGuardConfig> guards;
    public final Map<String, ZillaBindingConfig> bindings;
    public final Map<String, Object> telemetry;

    public ZillaAsyncApiConfig(
        String name,
        Map<String, ZillaCatalogConfig> catalogs,
        Map<String, ZillaGuardConfig> guards,
        Map<String, ZillaBindingConfig> bindings,
        Map<String, Object> telemetry)
    {
        this.name = name;
        this.catalogs = catalogs;
        this.guards = guards;
        this.bindings = bindings;
        this.telemetry = telemetry;
    }

    public static ZillaAsyncApiConfigBuilder<ZillaAsyncApiConfig> builder()
    {
        return new ZillaAsyncApiConfigBuilder<>(identity());
    }

    @SuppressWarnings("unchecked")
    private static <T> java.util.function.Function<ZillaAsyncApiConfig, T> identity()
    {
        return spec -> (T) spec;
    }
}
