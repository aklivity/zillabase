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
    public ZillabaseAdminConfig admin = new ZillabaseAdminConfig();
    public ZillabaseKafkaConfig kafka = new ZillabaseKafkaConfig();
    public ZillabaseRegistryConfig registry = new ZillabaseRegistryConfig();
    public ZillabaseRisingWaveConfig risingwave = new ZillabaseRisingWaveConfig();
    public ZillabaseZillaConfig zilla = new ZillabaseZillaConfig();
    public ZillabaseKeycloakConfig keycloak = new ZillabaseKeycloakConfig();
    public ZillabaseApiGenConfig apiGen = new ZillabaseApiGenConfig();
    public ZillabaseAuthConfig auth = new ZillabaseAuthConfig();
    public ZillabaseUdfConfig udf = new ZillabaseUdfConfig();
    public ZillabaseStudioConfig studio = new ZillabaseStudioConfig();
}
