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
package io.aklivity.zillabase.service.api.gen.internal.service;

import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.HTTP_ASYNCAPI_ARTIFACT_ID;
import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;

@Service
public class InitService implements CommandLineRunner
{
    @Autowired
    private ApicurioHelper specHelper;

    @Override
    public void run(
        String... args) throws Exception
    {
        final String kafkaSpec = """
             asyncapi: 3.0.0
             info:
               title: Kafka-AsyncAPI
               version: 1.0.0
            """;
        specHelper.register(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpec);

        final String httpSpec = """
             asyncapi: 3.0.0
             info:
               title: HTTP-AsyncAPI
               version: 1.0.0
            """;
        specHelper.register(HTTP_ASYNCAPI_ARTIFACT_ID, httpSpec);
    }
}
