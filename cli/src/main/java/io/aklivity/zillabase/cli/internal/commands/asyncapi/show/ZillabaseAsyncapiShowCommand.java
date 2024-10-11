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
package io.aklivity.zillabase.cli.internal.commands.asyncapi.show;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;

import io.aklivity.zillabase.cli.internal.commands.asyncapi.ZillabaseAsyncapiCommand;

@Command(
    name = "show",
    description = "Show AsyncAPI specifications")
public final class ZillabaseAsyncapiShowCommand extends ZillabaseAsyncapiCommand
{
    @Required
    @Arguments(title = {"id"},
        description = "AsyncAPI specification identifier")
    public String id;

    @Option(name = {"-v", "--verbose"},
        description = "Show verbose output")
    public boolean verbose;

    @Override
    protected void invoke()
    {
        HttpClient client = HttpClient.newHttpClient();

        String response = sendHttpRequest(String.format(ASYNCAPI_ID_PATH, id), client);

        if (response != null)
        {
            System.out.println(response);
        }
    }

    private String sendHttpRequest(
        String path,
        HttpClient client)
    {
        HttpRequest httpRequest = HttpRequest
            .newBuilder(ADMIN_SERVER_DEFAULT.resolve(path))
            .GET()
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
