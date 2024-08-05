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
package io.aklivity.zillabase.cli.internal.asyncapi;

import java.util.Map;

import com.asyncapi.bindings.OperationBinding;
import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ZillaHttpOperationBinding extends OperationBinding
{
    @JsonProperty("method")
    private HTTPOperationMethod method;

    @JsonProperty("overrides")
    private Map<String, String> overrides;

    public ZillaHttpOperationBinding(
        HTTPOperationMethod method,
        Map<String, String> overrides)
    {
        this.method = method;
        this.overrides = overrides;
    }

}
