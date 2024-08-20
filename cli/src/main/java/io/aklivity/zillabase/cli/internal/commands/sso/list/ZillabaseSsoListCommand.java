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
package io.aklivity.zillabase.cli.internal.commands.sso.list;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;

import io.aklivity.zillabase.cli.internal.commands.sso.ZillabaseSsoCommand;

@Command(
    name = "list",
    description = "List Identity Provider")
public final class ZillabaseSsoListCommand extends ZillabaseSsoCommand
{
    @Required
    @Option(name = {"-r", "--realm"},
        description = "Keycloak Realm")
    public String realm;

    @Option(name = {"-a", "--alias"},
        description = "Identity Provider Alias")
    public String alias;

    @Option(name = {"-u", "--url"},
        description = "Admin Server URL")
    public URI serverURL;

    @Option(name = {"-v", "--verbose"},
        description = "Show verbose output")
    public boolean verbose;

    @Override
    protected void invoke()
    {
        String response;
        HttpClient client = HttpClient.newHttpClient();

        response = sendHttpRequest(client);

        if (response != null)
        {
            System.out.println(response);
        }
    }

    private String sendHttpRequest(
        HttpClient client)
    {
        if (serverURL == null)
        {
            serverURL = ADMIN_SERVER_DEFAULT;
        }

        HttpRequest httpRequest = HttpRequest
            .newBuilder(serverURL.resolve(alias == null ? SSO_PATH : SSO_ALIAS_PATH.formatted(alias)))
            .header("Keycloak-Realm", realm)
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
