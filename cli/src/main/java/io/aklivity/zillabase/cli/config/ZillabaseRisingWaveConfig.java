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

public final class ZillabaseRisingWaveConfig
{
    public static final int DEFAULT_RISINGWAVE_PORT = 4566;

    private static final String DEFAULT_RISINGWAVE_URL = "localhost:%d".formatted(DEFAULT_RISINGWAVE_PORT);
    private static final String DEFAULT_RISINGWAVE_DB = "dev";

    public String url = DEFAULT_RISINGWAVE_URL;
    public String db = DEFAULT_RISINGWAVE_DB;
}
