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

import java.util.List;

public final class KafkaTopicSchemaRecord
{
    public String name;
    public String label;
    public String subject;
    public String type;
    public String schema;
    public List<String> cleanupPolicies;

    public KafkaTopicSchemaRecord(
        String name,
        List<String> cleanupPolicies,
        String label,
        String subject,
        String type,
        String schema)
    {
        this.name = name;
        this.cleanupPolicies = cleanupPolicies;
        this.label = label;
        this.subject = subject;
        this.type = type;
        this.schema = schema;
    }
}
