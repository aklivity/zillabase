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
package io.aklivity.zillabase.service.internal.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import com.sun.net.httpserver.HttpServer;

import io.aklivity.zillabase.service.internal.handler.ZillabaseAuthUserIdHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseAuthUsersHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseSsoAliasHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseSsoHandler;

public class ZillabaseServer implements Runnable
{
    private static final String ADMIN_PORT = "ADMIN_PORT";
    private static final int DEFAULT_ADMIN_PORT = 7185;
    private static final String DEFAULT_KEYCLOAK_URL = "http://keycloak.zillabase.dev:8180";
    private static final String KEYCLOAK_URL = "KEYCLOAK_URL";

    private final HttpServer server;
    private final HttpClient client;
    private final int port;
    private final boolean debug;
    private final String keycloakUrl;

    public ZillabaseServer()
    {
        String debug = System.getenv("DEBUG");
        this.debug = debug != null && "true".equals(debug);

        String keycloakUrl = System.getenv(KEYCLOAK_URL);
        this.keycloakUrl = keycloakUrl != null ? keycloakUrl : DEFAULT_KEYCLOAK_URL;

        try
        {
            String adminPort = System.getenv(ADMIN_PORT);
            this.port = adminPort != null ? Integer.parseInt(adminPort) : DEFAULT_ADMIN_PORT;
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.client = HttpClient.newHttpClient();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void run()
    {
        server.createContext("/v1/sso", new ZillabaseSsoHandler(client, keycloakUrl));
        server.createContext("/v1/sso/", new ZillabaseSsoAliasHandler(client, keycloakUrl));
        server.createContext("/v1/auth/users", new ZillabaseAuthUsersHandler(client, keycloakUrl));
        server.createContext("/v1/auth/users/", new ZillabaseAuthUserIdHandler(client, keycloakUrl));

        server.start();

        System.out.format("started\n");

        if (debug)
        {
            System.out.format("""
                environment:
                  ADMIN_PORT=%d
                  KEYCLOAK_URL=%s
                """, port, keycloakUrl);
        }

    }

    public void stop()
    {
        server.stop(0);

        System.out.format("stopped\n");
    }
}
