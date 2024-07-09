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
package io.aklivity.zillabase.cli.internal.commands.asyncapi.remove;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;

import io.aklivity.zillabase.cli.internal.commands.asyncapi.ZillabaseAsyncapiCommand;

@Command(
    name = "remove",
    description = "Delete an AsyncAPI specification")
public class ZillabaseAsyncapiRemoveCommand extends ZillabaseAsyncapiCommand
{
    private final HttpClient client = HttpClient.newHttpClient();

    @Option(name = {"-id"},
        description = "AsyncAPI specification identifier")
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
        String response = null;

        if (id != null)
        {
            response = sendHttpRequest(String.format(ASYNCAPI_ID_PATH, ASYNCAPI_PATH, id));
        }

        if (response != null)
        {
            System.out.println(response);
        }
    }

    private String sendHttpRequest(
        String path)
    {
        if (serverURL == null)
        {
            serverURL = ADMIN_SERVER_DEFAULT;
        }

        HttpRequest httpRequest = HttpRequest
            .newBuilder(serverURL.resolve(path))
            .DELETE()
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
