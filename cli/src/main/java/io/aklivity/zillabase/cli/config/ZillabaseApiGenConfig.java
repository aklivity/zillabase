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

import io.aklivity.zillabase.cli.internal.Zillabase;

public final class ZillabaseApiGenConfig
{
    private static final String DEFAULT_API_GEN_TAG = Zillabase.version();
    public static final int DEFAULT_API_GEN_PORT = 7185;
    public static final String DEFAULT_API_GEN_HOST = "api.gen.zillabase.dev";

    public String tag = DEFAULT_API_GEN_TAG;
}
