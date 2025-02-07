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

public final class ZillabaseConfigServerConfig
{
    public static final String ZILLABASE_CONFIG_KAFKA_TOPIC = "_zillabase.config";
    public static final String ZILLABASE_API_GEN_EVENTS_KAFKA_TOPIC = "_zillabase.api-gen-events";
    public static final String ZILLABASE_CONFIG_SERVER_ZILLA_YAML = """
        ---
        name: zillabase-config-server
        bindings:
          north_tcp_server:
            type: tcp
            kind: server
            options:
              host: 0.0.0.0
              port:
                - 7114
            routes:
              - when:
                  - port: 7114
                exit: north_http_server
          north_http_server:
            type: http
            kind: server
            routes:
              - when:
                  - headers:
                      :scheme: http
                      :authority: config.zillabase.dev:7114
                exit: north_http_kafka_mapping
          north_http_kafka_mapping:
            type: http-kafka
            kind: proxy
            routes:
              - when:
                  - method: PUT
                    path: /config/{id}
                exit: north_kafka_cache_client
                with:
                  capability: produce
                  topic: _zillabase.config
                  key: ${params.id}
              - when:
                  - method: GET
                    path: /config/{id}
                exit: north_kafka_cache_client
                with:
                  capability: fetch
                  topic: _zillabase.config
                  filters:
                    - key: ${params.id}
              - when:
                  - method: DELETE
                    path: /config/{id}
                exit: north_kafka_cache_client
                with:
                  capability: produce
                  topic: _zillabase.config
                  key: ${params.id}
          north_kafka_cache_client:
            type: kafka
            kind: cache_client
            exit: south_kafka_cache_server
          south_kafka_cache_server:
            type: kafka
            kind: cache_server
            options:
              bootstrap:
                - _zillabase.config
            exit: south_kafka_client
          south_kafka_client:
            type: kafka
            kind: client
            options:
              servers:
                - ${{env.KAFKA_BOOTSTRAP_SERVER}}
            exit: south_tcp_client
          south_tcp_client:
            type: tcp
            kind: client
        telemetry:
          exporters:
            stdout_logs_exporter:
              type: stdout
        """;

    private ZillabaseConfigServerConfig()
    {
    }
}
