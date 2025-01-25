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
package io.aklivity.zillabase.service.api.gen.internal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.gen")
public class ApiGenConfig
{
    @Value("${risingwave.db:dev}")
    private String risingwaveDb;

    @Value("${config.server.url:http://config.zillabase.dev:7114/}")
    private String configServerUrl;

    @Value("${apicurio.registry.url:http://apicurio.zillabase.dev:8080/}")
    private String apicurioUrl;

    @Value("${apicurio.group.id:default}")
    private String apicurioGroupId;

    @Value("${zcatalogs.topic:public.zcatalogs}")
    private String zcatalogsTopic;

    @Value("${api.gen.events.topic:_zillabase.api-gen-events}")
    private String eventsTopic;

    public String risingwaveDb()
    {
        return risingwaveDb;
    }

    public String configServerUrl()
    {
        return configServerUrl;
    }

    public String apicurioUrl()
    {
        return apicurioUrl;
    }

    public String apicurioGroupId()
    {
        return apicurioGroupId;
    }



    public String zcatalogsTopic()
    {
        return zcatalogsTopic;
    }

    public String eventsTopic()
    {
        return eventsTopic;
    }
}
