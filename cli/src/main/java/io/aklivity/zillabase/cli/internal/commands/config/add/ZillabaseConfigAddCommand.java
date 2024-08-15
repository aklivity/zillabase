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
package io.aklivity.zillabase.cli.internal.commands.config.add;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;

import io.aklivity.zillabase.cli.internal.commands.config.ZillabaseConfigCommand;

@Command(
    name = "add",
    description = "Add or Update a config")
public final class ZillabaseConfigAddCommand extends ZillabaseConfigCommand
{
    @Required
    @Option(name = {"-c", "--config"},
        description = "Config location")
    public String config;

    @Option(name = {"--id"},
        description = "Config identifier")
    public String id;

    @Option(name = {"-u", "--url"},
        description = "Admin Server URL")
    public URI serverURL;

    @Option(name = {"-v", "--verbose"},
        description = "Show verbose output")
    public boolean verbose;

    @Override
    protected void invoke()
    {
        Path path = Path.of(config);
        if (Files.exists(path))
        {
            try
            {
                HttpClient client = HttpClient.newHttpClient();

                if (sendHttpRequest(CONFIG_ID_PATH.formatted(id != null ? id : path), Files.newInputStream(path), client))
                {
                    System.out.println("Config Server is populated with %s".formatted(path));
                }
                else
                {
                    System.out.println("error");
                }
            }
            catch (IOException ex)
            {
                System.out.println("Failed to load: " + path);
            }
        }
    }

    private boolean sendHttpRequest(
        String path,
        InputStream content,
        HttpClient client)
    {
        if (serverURL == null)
        {
            serverURL = ADMIN_SERVER_DEFAULT;
        }

        HttpRequest httpRequest = HttpRequest
            .newBuilder(serverURL.resolve(path))
            .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> content))
            .build();

        boolean status = false;
        try
        {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            status = httpResponse.statusCode() == 204;
        }
        catch (Exception ex)
        {
            if (verbose)
            {
                ex.printStackTrace();
            }
        }
        return status;
    }
}
