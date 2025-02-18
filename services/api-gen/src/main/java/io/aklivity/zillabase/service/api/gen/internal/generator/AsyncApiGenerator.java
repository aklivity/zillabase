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
package io.aklivity.zillabase.service.api.gen.internal.generator;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.info.License;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public abstract class AsyncApiGenerator
{
    protected <C> AsyncapiSpecBuilder<C> injectInfo(
        AsyncapiSpecBuilder<C> builder,
        String title)
    {
        Info info = Info.builder()
            .title("API Document for %s".formatted(title))
            .version("1.0.0")
            .license(new License(
                "Aklivity Community License",
                "https://github.com/aklivity/zillabase/blob/develop/LICENSE"
            ))
            .build();
        return builder.info(info);
    }

    protected String buildYaml(
        AsyncapiSpec spec) throws Exception
    {
        AsyncAPI asyncAPI = AsyncAPI.builder()
            .asyncapi(spec.version)
            .info(spec.info)
            .servers(spec.servers)
            .components(spec.components)
            .channels(spec.channels)
            .operations(spec.operations)
            .build();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setSerializationInclusion(NON_NULL);

        return yamlMapper.writeValueAsString(asyncAPI);
    }
}
