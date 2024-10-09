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

public class ZillabaseZillaConfig
{
    public static final String DEFAULT_ZILLA_TAG = "0.9.97";

    private static final List<ZillabaseZillaPortConfig> DEFAULT_PORT_LIST = List.of(
        new ZillabaseZillaPortConfig()
        {
            {
                port = 8080;
                label = "http";
            }
        },
        new ZillabaseZillaPortConfig()
        {
            {
                port = 9090;
                label = "https";
            }
        },
        new ZillabaseZillaPortConfig()
        {
            {
                port = 7114;
                label = "http";
            }
        },
        new ZillabaseZillaPortConfig()
        {
            {
                port = 7143;
                label = "http";
            }
        }
    );

    public String tag = DEFAULT_ZILLA_TAG;
    public List<ZillabaseZillaPortConfig> ports = DEFAULT_PORT_LIST;
    public List<String> env;
}
