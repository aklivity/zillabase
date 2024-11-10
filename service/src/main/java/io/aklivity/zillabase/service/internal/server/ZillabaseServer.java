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
import java.util.Optional;

import com.sun.net.httpserver.HttpServer;

import io.aklivity.zillabase.service.internal.handler.ZillabaseAuthUserIdHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseAuthUsersHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseSsoAliasHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseSsoHandler;

public class ZillabaseServer implements Runnable
{
    private static final String AUTH_SERVER_PORT = "AUTH_SERVER_PORT";
    private static final int DEFAULT_AUTH_PORT = 7185;
    private static final String DEFAULT_KEYCLOAK_URL = "http://keycloak.zillabase.dev:8180";
    private static final String KEYCLOAK_URL = "KEYCLOAK_URL";
    private static final String KEYCLOAK_REALM = "KEYCLOAK_REALM";
    private static final String DEFAULT_REALM = "zillabase";

    private final HttpServer server;
    private final HttpClient client;
    private final int port;
    private final boolean debug;
    private final String url;
    private final String realm;

    public ZillabaseServer()
    {
        String debug = System.getenv("DEBUG");
        this.debug = debug != null && "true".equals(debug);

        this.url = Optional.ofNullable(System.getenv(KEYCLOAK_URL)).orElse(DEFAULT_KEYCLOAK_URL);
        this.realm = Optional.ofNullable(System.getenv(KEYCLOAK_REALM)).orElse(DEFAULT_REALM);
        this.port = Optional.ofNullable(System.getenv(AUTH_SERVER_PORT))
            .map(Integer::parseInt)
            .orElse(DEFAULT_AUTH_PORT);

        try
        {
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
        server.createContext("/v1/sso", new ZillabaseSsoHandler(client, url, realm));
        server.createContext("/v1/sso/", new ZillabaseSsoAliasHandler(client, url, realm));
        server.createContext("/v1/auth/users", new ZillabaseAuthUsersHandler(client, url, realm));
        server.createContext("/v1/auth/users/", new ZillabaseAuthUserIdHandler(client, url, realm));

        server.start();

        System.out.format("started\n");

        if (debug)
        {
            System.out.format("""
                environment:
                  AUTH_SERVER_PORT=%d
                  KEYCLOAK_URL=%s
                  KEYCLOAK_REALM=%s
                """, port, url, realm);
        }

    }

    public void stop()
    {
        server.stop(0);

        System.out.format("stopped\n");
    }
}
