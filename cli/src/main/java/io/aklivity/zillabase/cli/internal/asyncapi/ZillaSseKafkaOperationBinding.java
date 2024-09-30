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

import java.util.List;

import com.asyncapi.bindings.OperationBinding;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ZillaSseKafkaOperationBinding extends OperationBinding
{
    @JsonProperty("filters")
    private List<AsyncapiSseKafkaFilter> filters;

    public ZillaSseKafkaOperationBinding(
        List<AsyncapiSseKafkaFilter> filters)
    {
        this.filters = filters;
    }

}