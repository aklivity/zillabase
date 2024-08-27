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

import io.aklivity.zillabase.service.internal.handler.ZillabaseConfigServerHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApiSpecificationIdHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApisHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseSsoAliasHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseSsoHandler;

public class ZillabaseServer implements Runnable
{
    private static final String REGISTRY_URL = "REGISTRY_URL";
    private static final String REGISTRY_GROUP_ID = "REGISTRY_GROUP_ID";
    private static final String ADMIN_PORT = "ADMIN_PORT";
    private static final int DEFAULT_ADMIN_PORT = 7184;
    private static final String DEFAULT_GROUP_ID = "default";
    private static final String DEFAULT_REGISTRY_URL = "http://apicurio.zillabase.dev:8080";
    private static final String DEFAULT_CONFIG_SERVER_URL = "http://config.zillabase.dev:7114";
    private static final String DEFAULT_KEYCLOAK_URL = "http://keycloak.zillabase.dev:8180";
    private static final String CONFIG_SERVER_URL = "CONFIG_SERVER_URL";
    private static final String KEYCLOAK_URL = "KEYCLOAK_URL";

    private final HttpServer server;
    private final HttpClient client;
    private final String baseUrl;
    private final String groupId;
    private final int port;
    private final boolean debug;
    private final String configServerUrl;
    private final String keycloakUrl;

    public ZillabaseServer()
    {
        String registryUrl = System.getenv(REGISTRY_URL);
        this.baseUrl = registryUrl != null ? registryUrl : DEFAULT_REGISTRY_URL;
        String registryGroupId = System.getenv(REGISTRY_GROUP_ID);
        this.groupId = registryGroupId != null ? registryGroupId : DEFAULT_GROUP_ID;

        String debug = System.getenv("DEBUG");
        this.debug = debug != null && "true".equals(debug);

        String configServerUrl = System.getenv(CONFIG_SERVER_URL);
        this.configServerUrl = configServerUrl != null ? configServerUrl : DEFAULT_CONFIG_SERVER_URL;
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
        server.createContext("/v1/asyncapis", new ZillabaseServerAsyncApisHandler(client, baseUrl, groupId));
        server.createContext("/v1/asyncapis/", new ZillabaseServerAsyncApiSpecificationIdHandler(client, baseUrl, groupId));
        server.createContext("/v1/config/", new ZillabaseConfigServerHandler(client, configServerUrl));
        server.createContext("/v1/sso", new ZillabaseSsoHandler(client, keycloakUrl));
        server.createContext("/v1/sso/", new ZillabaseSsoAliasHandler(client, keycloakUrl));

        server.start();

        System.out.format("started\n");

        if (debug)
        {
            System.out.format("""
                environment:
                  ADMIN_PORT=%d
                  REGISTRY_URL=%s
                  REGISTRY_GROUP_ID=%s
                  CONFIG_SERVER_URL=%s
                """, port, baseUrl, groupId, configServerUrl);
        }

    }

    public void stop()
    {
        server.stop(0);

        System.out.format("stopped\n");
    }
}
