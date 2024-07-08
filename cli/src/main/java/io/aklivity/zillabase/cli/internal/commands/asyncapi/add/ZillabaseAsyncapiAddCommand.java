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

import static io.aklivity.zillabase.cli.internal.commands.asyncapi.ZillabaseAsyncapiCommand.ASYNCAPI_PATH;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

@Command(
    name = "add",
    description = "Add a new AsyncAPI specification")
public class ZillabaseAsyncapiAddCommand extends ZillabaseCommand
{
    private static final URI ZILLABASE_DEFAULT_LOCATION = URI.create("./asyncapi.yml");

    private final HttpClient client = HttpClient.newHttpClient();

    @Option(name = {"-s", "--spec"},
        description = "AsyncAPI specification location")
    public URI spec;

    @Option(name = {"-u", "--url"},
        description = "Admin Server URL")
    public URI serverURL;

    @Option(name = {"-v", "--verbose"},
        description = "Show verbose output")
    public boolean verbose;

    @Override
    protected void invoke()
    {
        Path path = Path.of(spec != null ? spec : ZILLABASE_DEFAULT_LOCATION);
        if (Files.exists(path))
        {
            try
            {
                String response = sendHttpRequest(Files.newInputStream(path));
                if (response != null)
                {
                    System.out.println(response);
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
