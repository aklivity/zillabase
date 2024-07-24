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
package io.aklivity.zillabase.service.config;

public class ZillabaseAdminConfig
{
    public static final int DEFAULT_ADMIN_PORT = 7184;
    public static final String DEFAULT_GROUP_ID = "zilla";
    public static final String DEFAULT_REGISTRY_URL = "http://localhost:8080";

    public int port;
    public String registryUrl;
    public String registryGroupId;

    public ZillabaseAdminConfig(
        int port,
        String registryUrl,
        String registryGroupId)
    {
        this.port = port;
        this.registryUrl = registryUrl;
        this.registryGroupId = registryGroupId;
    }

    public ZillabaseAdminConfig()
    {
        this.port = DEFAULT_ADMIN_PORT;
        this.registryUrl = DEFAULT_REGISTRY_URL;
        this.registryGroupId = DEFAULT_GROUP_ID;
    }
}
