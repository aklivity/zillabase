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
package io.aklivity.zillabase.cli.internal.commands.sso.add;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;

import io.aklivity.zillabase.cli.internal.commands.sso.ZillabaseSsoCommand;

@Command(
    name = "add",
    description = "Add a new Identity Provider")
public final class ZillabaseSsoAddCommand extends ZillabaseSsoCommand
{
    @Required
    @Option(name = {"-p", "--provider"},
        description = "Identity Provider Name")
    public String providerId;

    @Required
    @Option(name = {"-a", "--alias"},
        description = "Identity Provider Alias")
    public String alias;

    @Required
    @Option(name = {"-r", "--realm"},
        description = "Keycloak Realm")
    public String realm;

    @Required
    @Option(name = {"-c", "--client"},
        description = "Client Id")
    public String clientId;

    @Required
    @Option(name = {"-s", "--secret"},
        description = "Client Secret")
    public String secret;

    @Option(name = {"-v", "--verbose"},
        description = "Show verbose output")
    public boolean verbose;

    @Override
    protected void invoke()
    {
        try
        {
            Map<String, String> config = new HashMap<>();
            config.put("clientId", clientId);
            config.put("clientSecret", secret);

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode idpNode = mapper.createObjectNode();
            idpNode.put("alias", alias);
            idpNode.put("providerId", providerId);
            idpNode.put("enabled", true);
            idpNode.putPOJO("config", config);

            HttpClient client = HttpClient.newHttpClient();
            if (sendHttpRequest(mapper.writeValueAsString(idpNode), client))
            {
                System.out.println("Identity Provider added successfully");
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    private boolean sendHttpRequest(
        String content,
        HttpClient client)
    {
        HttpRequest httpRequest = HttpRequest
            .newBuilder(ADMIN_SERVER_DEFAULT.resolve(SSO_PATH))
            .POST(HttpRequest.BodyPublishers.ofString(content))
            .build();

        boolean status = false;
        try
        {
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            status = httpResponse.statusCode() == 201;
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
