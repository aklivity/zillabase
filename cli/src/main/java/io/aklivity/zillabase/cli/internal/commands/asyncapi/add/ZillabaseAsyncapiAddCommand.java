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
package io.aklivity.zillabase.cli.internal.commands.asyncapi.add;

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

import io.aklivity.zillabase.cli.internal.commands.asyncapi.ZillabaseAsyncapiCommand;

@Command(
    name = "add",
    description = "Add a new AsyncAPI specification")
public class ZillabaseAsyncapiAddCommand extends ZillabaseAsyncapiCommand
{
    private final HttpClient client = HttpClient.newHttpClient();

    @Required
    @Option(name = {"-s", "--spec"},
        description = "AsyncAPI specification location")
    public String spec;

    @Option(name = {"-u", "--url"},
        description = "Admin Server URL")
    public URI serverURL;

    @Option(name = {"-v", "--verbose"},
        description = "Show verbose output")
    public boolean verbose;

    @Override
    protected void invoke()
    {
        Path path = Path.of(spec);
        if (Files.exists(path))
        {
            try
            {
                String response = sendHttpRequest(Files.newInputStream(path));
                if (response != null)
                {
                    System.out.println(response);
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

    private String sendHttpRequest(
        InputStream content)
    {
        if (serverURL == null)
        {
            serverURL = ADMIN_SERVER_DEFAULT;
        }

        HttpRequest httpRequest = HttpRequest
            .newBuilder(serverURL.resolve(ASYNCAPI_PATH))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> content))
            .build();

        String responseBody;
        try
        {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            responseBody = httpResponse.statusCode() == 200 ? httpResponse.body() : null;
        }
        catch (Exception ex)
        {
            responseBody = null;
            if (verbose)
            {
                ex.printStackTrace();
            }
        }
        return responseBody;
    }
}
