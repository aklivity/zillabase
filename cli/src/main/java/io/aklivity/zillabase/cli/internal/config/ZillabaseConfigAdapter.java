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

import io.aklivity.zillabase.cli.config.ZillabaseAdminConfig;
import io.aklivity.zillabase.cli.config.ZillabaseConfig;

public class ZillabaseConfigAdapter implements JsonbAdapter<ZillabaseConfig, JsonObject>
{
    private static final String API_NAME = "api";
    private static final String PORT_NAME = "port";
    private static final String REGISTRY_NAME = "registry";
    private static final String URL_NAME = "url";
    private static final String REGISTRY_GROUP_ID = "groupId";
    private static final String RISINGWAVE_NAME = "risingwave";
    private static final String DB_NAME = "db";
    private static final String KAFKA_NAME = "kafka";
    private static final String ADMIN_NAME = "admin";

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
        ZillabaseConfig config = new ZillabaseConfig();

        if (object.containsKey(API_NAME))
        {
            JsonObject api = object.getJsonObject(API_NAME);

            if (api.containsKey(PORT_NAME))
            {
                config.port = api.getInt(PORT_NAME);
            }

            if (api.containsKey(REGISTRY_NAME))
            {
                JsonObject registry = api.getJsonObject(REGISTRY_NAME);

                if (registry.containsKey(URL_NAME))
                {
                    config.registryUrl = registry.getString(URL_NAME);
                }

                if (registry.containsKey(REGISTRY_GROUP_ID))
                {
                    config.registryGroupId = registry.getString(REGISTRY_GROUP_ID);
                }
            }

            if (api.containsKey(RISINGWAVE_NAME))
            {
                JsonObject risingWave = api.getJsonObject(RISINGWAVE_NAME);

                if (risingWave.containsKey(URL_NAME))
                {
                    config.risingWaveUrl = risingWave.getString(URL_NAME);
                }

                if (risingWave.containsKey(DB_NAME))
                {
                    config.risingWaveDb = risingWave.getString(DB_NAME);
                }
            }

            if (api.containsKey(KAFKA_NAME))
            {
                JsonObject kafka = api.getJsonObject(KAFKA_NAME);

                if (kafka.containsKey(URL_NAME))
                {
                    config.kafkaBootstrapUrl = kafka.getString(URL_NAME);
                }
            }

            if (api.containsKey(ADMIN_NAME))
            {
                ZillabaseAdminConfig adminConfig = new ZillabaseAdminConfig();
                JsonObject admin = api.getJsonObject(ADMIN_NAME);

                if (admin.containsKey(REGISTRY_NAME))
                {
                    JsonObject registry = admin.getJsonObject(REGISTRY_NAME);

                    if (registry.containsKey(URL_NAME))
                    {
                        adminConfig.registryUrl = registry.getString(URL_NAME);
                    }

                    if (registry.containsKey(REGISTRY_GROUP_ID))
                    {
                        adminConfig.registryGroupId = registry.getString(REGISTRY_GROUP_ID);
                    }
                }

                config.adminConfig = adminConfig;
            }
        }

        return config;
    }
}
