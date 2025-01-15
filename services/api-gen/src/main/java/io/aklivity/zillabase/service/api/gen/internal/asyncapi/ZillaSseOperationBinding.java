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
package io.aklivity.zillabase.service.api.gen.internal.asyncapi;

import com.asyncapi.bindings.OperationBinding;
import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ZillaSseOperationBinding extends OperationBinding
{
    @JsonProperty("method")
    private HTTPOperationMethod method;


    @JsonProperty("bindingVersion")
    private String bindingVersion;

    public ZillaSseOperationBinding(
        HTTPOperationMethod method,
        String bindingVersion)
    {
        this.method = method;
        this.bindingVersion = bindingVersion;
    }

    public ZillaSseOperationBinding()
    {
    }
}
