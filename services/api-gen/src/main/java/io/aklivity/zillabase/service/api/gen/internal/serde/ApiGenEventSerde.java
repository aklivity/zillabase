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
package io.aklivity.zillabase.service.api.gen.internal.serde;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;

public class ApiGenEventSerde implements Serde<ApiGenEvent>
{

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Serializer<ApiGenEvent> serializer()
    {
        return (topic, data) ->
        {
            try
            {
                return objectMapper.writeValueAsBytes(data);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error serializing Event", e);
            }
        };
    }

    @Override
    public Deserializer<ApiGenEvent> deserializer()
    {
        return (topic, data) ->
        {
            try
            {
                return objectMapper.readValue(data, ApiGenEvent.class);
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error deserializing Event", e);
            }
        };
    }
}
