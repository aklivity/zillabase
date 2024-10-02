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

import java.util.List;

public final class ZillabaseAdminConfig
{
    public static final int DEFAULT_ADMIN_HTTP_PORT = 7184;
    public static final int DEFAULT_ADMIN_PGSQL_PORT = 4567;

    public static final String ZILLABASE_ADMIN_SERVER_ZILLA_YAML = """
        ---
        name: admin
        catalogs:
          karapace_catalog:
            type: karapace
            options:
              url: ${{env.KARAPACE_URL}}
        bindings:
          tcp_server:
            type: tcp
            kind: server
            options:
              host: 0.0.0.0
              port:
                - 4567
                - 7184
            routes:
              - when:
                  - port: 4567
                exit: pgsql_server
              - when:
                  - port: 7184
                exit: http_server
          pgsql_server:
            type: pgsql
            kind: server
            exit: risingwave_proxy
          risingwave_proxy:
            type: risingwave
            kind: proxy
            options:
              kafka:
                properties:
                  bootstrap.server: ${{env.KAFKA_BOOTSTRAP_SERVER}}
                format:
                  model: avro
                  catalog:
                    karapace_catalog:
                      - strategy: topic
              udf:
                - server: ${{env.UDF_SERVER}}
            routes:
              - exit: pgsql_kafka_proxy
                when:
                  - commands:
                      - "CREATE TOPIC"
            exit: pgsql_client
          pgsql_client:
            type: pgsql
            kind: client
            exit: pgsql_tcp_client
          pgsql_tcp_client:
            type: tcp
            kind: client
            options:
              host: ${{env.RISINGWAVE_HOST}}
              port: ${{env.RISINGWAVE_PORT}}
          pgsql_kafka_proxy:
            type: pgsql-kafka
            kind: proxy
            catalog:
              karapace_catalog:
                - strategy: topic
            exit: kafka_client
          kafka_client:
            type: kafka
            kind: client
            options:
              servers:
                - ${{env.KAFKA_BOOTSTRAP_SERVER}}
            exit: kafka_tcp_client
          kafka_tcp_client:
            type: tcp
            kind: client
          http_server:
            type: http
            kind: server
            routes:
              - when:
                  - headers:
                      :scheme: http
                      :authority: localhost:7184
                      :path: /v1/config/{id}
                with:
                  headers:
                    overrides:
                      :authority: ${{env.CONFIG_SERVER_HOST}}:${{env.CONFIG_SERVER_PORT}}
                      :path: /config/${params.id}
                exit: config_http_client
              - when:
                  - headers:
                      :scheme: http
                      :authority: localhost:7184
                      :path: /v1/asyncapis
                with:
                  headers:
                    overrides:
                      :authority: ${{env.APICURIO_HOST}}:${{env.APICURIO_PORT}}
                      :path: /apis/registry/v2/groups/${{env.REGISTRY_GROUP_ID}}/artifacts
                      x-registry-artifacttype: ASYNCAPI
                exit: apicurio_http_client
              - when:
                  - headers:
                      :method: GET
                      :scheme: http
                      :authority: localhost:7184
                      :path: /v1/asyncapis/{id}
                with:
                  headers:
                    overrides:
                      :authority: ${{env.APICURIO_HOST}}:${{env.APICURIO_PORT}}
                      :path: /apis/registry/v2/ids/globalIds/${params.id}
                exit: apicurio_http_client
              - when:
                  - headers:
                      :method: PUT
                      :scheme: http
                      :authority: localhost:7184
                      :path: /v1/asyncapis/{id}
                with:
                  headers:
                    overrides:
                      :authority: ${{env.APICURIO_HOST}}:${{env.APICURIO_PORT}}
                      :path: /apis/registry/v2/groups/${{env.REGISTRY_GROUP_ID}}/artifacts/${params.id}
                exit: apicurio_http_client
              - when:
                  - headers:
                      :method: DELETE
                      :scheme: http
                      :authority: localhost:7184
                      :path: /v1/asyncapis/{id}
                with:
                  headers:
                    overrides:
                      :authority: ${{env.APICURIO_HOST}}:${{env.APICURIO_PORT}}
                      :path: /apis/registry/v2/groups/${{env.REGISTRY_GROUP_ID}}/artifacts/${params.id}
                exit: apicurio_http_client
              - when:
                  - headers:
                      :scheme: http
                      :authority: localhost:7184
                      :path: /v1/sso
                with:
                  headers:
                    overrides:
                      :authority: ${{env.SSO_ADMIN_HOST}}:${{env.SSO_ADMIN_PORT}}
                exit: sso_http_client
              - when:
                  - headers:
                      :scheme: http
                      :authority: localhost:7184
                      :path: /v1/sso/*
                with:
                  headers:
                    overrides:
                      :authority: ${{env.SSO_ADMIN_HOST}}:${{env.SSO_ADMIN_PORT}}
                exit: sso_http_client
          config_http_client:
            type: http
            kind: client
            exit: config_tcp_client
          config_tcp_client:
            type: tcp
            kind: client
            options:
              host: ${{env.CONFIG_SERVER_HOST}}
              port: ${{env.CONFIG_SERVER_PORT}}
          apicurio_http_client:
            type: http
            kind: client
            exit: apicurio_tcp_client
          apicurio_tcp_client:
            type: tcp
            kind: client
            options:
              host: ${{env.APICURIO_HOST}}
              port: ${{env.APICURIO_PORT}}
          sso_http_client:
            type: http
            kind: client
            exit: sso_tcp_client
          sso_tcp_client:
            type: tcp
            kind: client
            options:
              host: ${{env.SSO_ADMIN_HOST}}
              port: ${{env.SSO_ADMIN_PORT}}
        telemetry:
          exporters:
            stdout_logs_exporter:
              type: stdout
        """;
    public static final List<Integer> PORTS = List.of(DEFAULT_ADMIN_HTTP_PORT, DEFAULT_ADMIN_PGSQL_PORT);

    private static final String DEFAULT_CONFIG_SERVER_URL = "http://config.zillabase.dev:7114";

    public String tag = "latest";
    public String configServerUrl = DEFAULT_CONFIG_SERVER_URL;
}
