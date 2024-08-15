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
package io.aklivity.zillabase.cli.internal.commands.config.remove;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;

import io.aklivity.zillabase.cli.internal.commands.config.ZillabaseConfigCommand;

@Command(
    name = "remove",
    description = "Delete a Config")
public final class ZillabaseConfigRemoveCommand extends ZillabaseConfigCommand
{
    private static final String SUCCESSFULLY_DELETED = "The config was successfully deleted";

    @Required
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
        HttpClient client = HttpClient.newHttpClient();
        String response = sendHttpRequest(CONFIG_ID_PATH.formatted(id), client);

        if (response != null)
        {
            System.out.println(response);
        }
    }

    private String sendHttpRequest(
        String path,
        HttpClient client)
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
            responseBody = httpResponse.statusCode() == 204 ? SUCCESSFULLY_DELETED : null;
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
