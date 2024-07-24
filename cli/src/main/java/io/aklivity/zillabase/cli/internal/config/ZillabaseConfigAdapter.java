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
package io.aklivity.zillabase.cli.internal.config;

import static io.aklivity.zillabase.cli.config.ZillabaseConfig.DEFAULT_ADMIN_PORT;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.bind.adapter.JsonbAdapter;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;

public class ZillabaseConfigAdapter implements JsonbAdapter<ZillabaseConfig, JsonObject>
{
    private static final String API_NAME = "api";
    private static final String PORT_NAME = "port";

    @Override
    public JsonObject adaptToJson(
        ZillabaseConfig config)
    {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        if (config.port != DEFAULT_ADMIN_PORT)
        {
            JsonObjectBuilder api = Json.createObjectBuilder();
            api.add(PORT_NAME, config.port);

            builder.add(API_NAME, api);
        }
        return builder.build();
    }

    @Override
    public ZillabaseConfig adaptFromJson(
        JsonObject object)
    {
        JsonObject api = object.getJsonObject(API_NAME);

        int port = DEFAULT_ADMIN_PORT;

        if (api.containsKey(PORT_NAME))
        {
            port = api.getInt(PORT_NAME);
        }

        return new ZillabaseConfig(port);
    }
}