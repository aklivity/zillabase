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
package io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZillaBindingOptionsConfig
{
    public Map<String, Object> specs;
    public HttpAuthorizationOptionsConfig http;
    public KafkaOptionsConfig kafka;

    public static class HttpAuthorizationOptionsConfig
    {
        public Map<String, Object> authorization;
    }

    public static class KafkaOptionsConfig
    {
        public List<KafkaTopicConfig> topics;
    }

    public static class KafkaTopicConfig
    {
        public String name;
        public List<TransformConfig> transforms;
        public ModelConfig value;
    }

    public static class TransformConfig
    {
        @JsonProperty("extract-headers")
        public Map<String, String> headers;
    }

    public static class ModelConfig
    {
        public String view;
        public String model;
        public Map<String, Object> catalog;
    }
}
