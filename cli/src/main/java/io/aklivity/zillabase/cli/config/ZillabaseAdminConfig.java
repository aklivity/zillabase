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
package io.aklivity.zillabase.cli.config;

public final class ZillabaseAdminConfig
{
    private static final int DEFAULT_ADMIN_PORT = 7184;
    private static final String DEFAULT_CONFIG_SERVER_URL = "http://config.zillabase.dev:7114";

    public String tag = "latest";
    public int port = DEFAULT_ADMIN_PORT;
    public String configServerUrl = DEFAULT_CONFIG_SERVER_URL;
}
