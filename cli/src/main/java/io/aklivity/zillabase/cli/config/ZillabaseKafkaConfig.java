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
package io.aklivity.zillabase.cli.config;

public final class ZillabaseKafkaConfig
{
    private static final String DEFAULT_KAFKA_TAG = "3.2.3";
    public static final String DEFAULT_KAFKA_BOOTSTRAP_URL = "kafka.zillabase.dev:29092";

    public String tag = DEFAULT_KAFKA_TAG;
    public String bootstrapUrl = DEFAULT_KAFKA_BOOTSTRAP_URL;
}
