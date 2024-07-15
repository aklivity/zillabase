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
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.sun.net.httpserver.HttpServer;

import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApiSpecificationIdHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApisHandler;

public class ZillabaseServer implements Runnable
{
    private static final String API = "api";
    private static final String REGISTRY_URL = "registry.url";
    private static final String PORT = "port";
    private static final int DEFAULT_PORT = 7184;
    private static final String REGISTRY_GROUP_ID = "registry.groupId";
    private static final String DEFAULT_GROUP_ID = "zilla";

    private final HttpServer server;
    private final HttpClient client;
    private final String baseUrl;
    private final String groupId;
    private final Map<String, String> configs;

    public ZillabaseServer()
    {
        Path configPath = Paths.get("zillabase/config.yaml");
        this.configs = new HashMap<>();

        if (Files.exists(configPath))
        {
            try
            {
                InputStream inputStream = Files.newInputStream(configPath);
                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(inputStream);

                if (config.containsKey(API))
                {
                    this.configs.putAll((Map<String, String>) config.get(API));
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
        }

        this.baseUrl = configs.containsKey(REGISTRY_URL) ? configs.get(REGISTRY_URL) : null;
        this.groupId = configs.containsKey(REGISTRY_GROUP_ID) ? configs.get(REGISTRY_GROUP_ID) : DEFAULT_GROUP_ID;

        try
        {
            this.server = HttpServer.create(
                new InetSocketAddress(configs.containsKey(PORT) ? Integer.parseInt(configs.get(PORT)) : DEFAULT_PORT), 0);
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
