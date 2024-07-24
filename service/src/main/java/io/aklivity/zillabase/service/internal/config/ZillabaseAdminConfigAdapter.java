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
package io.aklivity.zillabase.service.internal.config;

import static io.aklivity.zillabase.service.config.ZillabaseAdminConfig.DEFAULT_ADMIN_PORT;
import static io.aklivity.zillabase.service.config.ZillabaseAdminConfig.DEFAULT_GROUP_ID;
import static io.aklivity.zillabase.service.config.ZillabaseAdminConfig.DEFAULT_REGISTRY_URL;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.bind.adapter.JsonbAdapter;

import io.aklivity.zillabase.service.config.ZillabaseAdminConfig;

public class ZillabaseAdminConfigAdapter implements JsonbAdapter<ZillabaseAdminConfig, JsonObject>
{
    private static final String API_NAME = "api";
    private static final String PORT_NAME = "port";
    public static final String REGISTRY_NAME = "registry";
    private static final String REGISTRY_URL = "url";
    private static final String REGISTRY_GROUP_ID = "groupId";

    @Override
    public JsonObject adaptToJson(
        ZillabaseAdminConfig config)
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
    public ZillabaseAdminConfig adaptFromJson(
        JsonObject object)
    {
        JsonObject api = object.getJsonObject(API_NAME);

        int port = DEFAULT_ADMIN_PORT;
        String url = DEFAULT_REGISTRY_URL;
        String groupId = DEFAULT_GROUP_ID;

        if (api.containsKey(PORT_NAME))
        {
            port = api.getInt(PORT_NAME);
        }

        if (api.containsKey(REGISTRY_NAME))
        {
            JsonObject registry = api.getJsonObject(REGISTRY_NAME);

            if (registry.containsKey(REGISTRY_URL))
            {
                url = registry.getString(REGISTRY_URL);
            }

            if (registry.containsKey(REGISTRY_GROUP_ID))
            {
                groupId = registry.getString(REGISTRY_GROUP_ID);
            }
        }

        return new ZillabaseAdminConfig(port, url, groupId);
    }
}
