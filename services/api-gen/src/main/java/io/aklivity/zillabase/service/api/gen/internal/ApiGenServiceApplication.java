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
package io.aklivity.zillabase.service.api.gen.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;

@SpringBootApplication
@SuppressWarnings({"HideUtilityClassConstructor"})
public class ApiGenServiceApplication
{
    @Autowired
    private ApicurioHelper specHelper;

    public static void main(
        String[] args)
    {
        SpringApplication.run(ApiGenServiceApplication.class, args);
    }
}
