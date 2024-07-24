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
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;

import com.sun.net.httpserver.HttpServer;

import io.aklivity.zillabase.service.config.ZillabaseAdminConfig;
import io.aklivity.zillabase.service.internal.config.ZillabaseAdminConfigAdapter;
import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApiSpecificationIdHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApisHandler;

public class ZillabaseServer implements Runnable
{
    private final HttpServer server;
    private final HttpClient client;
    private final String baseUrl;
    private final String groupId;

    public ZillabaseServer()
    {
        ZillabaseAdminConfig config;
        Path configPath = Paths.get("zillabase/config.yaml");
        try (InputStream inputStream = Files.newInputStream(configPath))
        {
            JsonbConfig jsonbConfig = new JsonbConfig().withAdapters(new ZillabaseAdminConfigAdapter());
            Jsonb jsonb = JsonbBuilder.create(jsonbConfig);

            config = jsonb.fromJson(inputStream, ZillabaseAdminConfig.class);
        }
        catch (IOException | JsonbException ex)
        {
            config = new ZillabaseAdminConfig();
        }

        this.baseUrl = config.registryUrl;
        this.groupId = config.registryGroupId;

        try
        {
            this.server = HttpServer.create(
                new InetSocketAddress(config.port), 0);
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
        if (baseUrl == null)
        {
            throw new RuntimeException("Registry URL unavailable");
        }

        server.createContext("/v1/asyncapis", new ZillabaseServerAsyncApisHandler(client, baseUrl, groupId));
        server.createContext("/v1/asyncapis/", new ZillabaseServerAsyncApiSpecificationIdHandler(client, baseUrl, groupId));

        server.start();

        System.out.format("started\n");
    }

    public void stop()
    {
        server.stop(0);

        System.out.format("stopped\n");
    }
}
