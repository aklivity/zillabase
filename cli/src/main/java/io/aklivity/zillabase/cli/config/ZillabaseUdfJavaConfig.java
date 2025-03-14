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

import java.util.List;

import io.aklivity.zillabase.cli.internal.Zillabase;

public final class ZillabaseUdfJavaConfig
{
    public static final String DEFAULT_UDF_JAVA_TAG = Zillabase.version();
    public static final String DEFAULT_UDF_JAVA_API_URL = "http://udf-server-java.zillabase.dev:5001";
    public static final String DEFAULT_UDF_JAVA_SERVER_URL = "http://udf-server-java.zillabase.dev:8815";

    public String tag = DEFAULT_UDF_JAVA_TAG;
    public List<String> env;
    public String apiUrl = DEFAULT_UDF_JAVA_API_URL;
    public String serverUrl = DEFAULT_UDF_JAVA_SERVER_URL;
}
