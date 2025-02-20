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

import io.aklivity.zillabase.cli.internal.Zillabase;

public final class ZillabaseStudioConfig
{
    public static final String DEFAULT_STUDIO_TAG = Zillabase.version();
    public static final int DEFAULT_STUDIO_HTTP_PORT = 7194;

    private final String tag = DEFAULT_STUDIO_TAG;
    private final int port = DEFAULT_STUDIO_HTTP_PORT;

    private final String zillaConfig = """
        ---
        name: example
        bindings:
          north_tcp_server:
            type: tcp
            kind: server
            options:
              host: 0.0.0.0
              port:
                - %d
            routes:
                - when:
                    - port: %d
                  exit: north_http_server
          north_http_server:
            type: http
            kind: server
            routes:
              - when:
                  - headers:
                      :scheme: http
                      :authority: localhost:%d
                exit: east_http_filesystem_mapping
          east_http_filesystem_mapping:
            type: http-filesystem
            kind: proxy
            routes:
              - when:
                  - path: /
                exit: east_filesystem_server
                with:
                  path: index.html
              - when:
                  - path: /{path}
                exit: east_filesystem_server
                with:
                  path: ${params.path}
          east_filesystem_server:
            type: filesystem
            kind: server
            options:
              location: /var/www/
        telemetry:
          exporters:
            stdout_logs_exporter:
              type: stdout
        """.formatted(port, port, port);

    public String zillaConfig()
    {
        return zillaConfig;
    }

    public int port()
    {
        return port;
    }

    public String tag()
    {
        return tag;
    }
}
