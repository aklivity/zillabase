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

public final class ZillabaseConfig
{
    public static final int DEFAULT_ADMIN_PORT = 7184;

    private static final String DEFAULT_HTTP_URL = "localhost:9090";

    public int port = DEFAULT_ADMIN_PORT;
    public String httpEndpoints = DEFAULT_HTTP_URL;
    public ZillabaseAdminConfig admin = new ZillabaseAdminConfig();
    public ZillabaseKafkaConfig kafka = new ZillabaseKafkaConfig();
    public ZillabaseApicurioConfig registry = new ZillabaseApicurioConfig();
    public ZillabaseRisingWaveConfig risingWave = new ZillabaseRisingWaveConfig();
}
