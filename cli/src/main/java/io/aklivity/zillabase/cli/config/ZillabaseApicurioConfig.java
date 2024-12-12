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

public final class ZillabaseApicurioConfig
{
    private static final String DEFAULT_APICURIO_TAG = "3.0.4";
    private static final String DEFAULT_APICURIO_GROUP_ID = "default";

    public static final String DEFAULT_APICURIO_URL = "http://apicurio.zillabase.dev:8080";

    public String tag = DEFAULT_APICURIO_TAG;
    public String url = DEFAULT_APICURIO_URL;
    public String groupId = DEFAULT_APICURIO_GROUP_ID;
}
