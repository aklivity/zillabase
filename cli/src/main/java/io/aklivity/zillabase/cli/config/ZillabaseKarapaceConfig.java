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

public final class ZillabaseKarapaceConfig
{
    private static final String DEFAULT_KARAPACE_CONTEXT = "default";
    private static final String DEFAULT_KARAPACE_TAG = "latest";

    public static final String DEFAULT_KARAPACE_URL = "http://karapace.zillabase.dev:8081";
    public static final String DEFAULT_CLIENT_KARAPACE_URL = "http://localhost:8081";

    public String tag = DEFAULT_KARAPACE_TAG;
    public String url = DEFAULT_KARAPACE_URL;
    public String context = DEFAULT_KARAPACE_CONTEXT;
}
