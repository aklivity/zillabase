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
package io.aklivity.zillabase.service.internal.handler;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

import io.aklivity.zillabase.service.internal.common.ZillabaseAuthHelper;

public class ZillabaseSsoAliasHandler extends ZillabaseServerHandler
{
    private static final Pattern PATH_PATTERN = Pattern.compile("/v1/sso/(.*)");

    private final HttpClient client;
    private final ZillabaseAuthHelper helper;
    private final Matcher matcher;
    private final String keycloakUrl;

    public ZillabaseSsoAliasHandler(
        HttpClient client,
        String keycloakUrl)
    {
        this.client = client;
        this.helper = new ZillabaseAuthHelper(client, keycloakUrl);
        this.matcher = PATH_PATTERN.matcher("");
        this.keycloakUrl = keycloakUrl;
    }

    @Override
    public void handle(
        HttpExchange exchange)
    {
        String path = exchange.getRequestURI().getPath();
        if (matcher.reset(path).matches())
        {
            String alias = matcher.group(1);
            String method = exchange.getRequestMethod();
            boolean badMethod = false;
            HttpRequest.Builder builder = HttpRequest.newBuilder(toURI(keycloakUrl,
                "/admin/realms/%s/identity-provider/instances/%s".formatted(
                    exchange.getRequestHeaders().getFirst("Keycloak-Realm"), alias)));

            try
            {
                String token = helper.fetchAccessToken();
                if (token != null)
                {
                    switch (method)
                    {
                    case "GET":
                        builder.header("Authorization", "Bearer %s".formatted(token))
                            .GET();
                        break;
                    case "DELETE":
                        builder.header("Authorization", "Bearer %s".formatted(token))
                            .DELETE();
                        break;
                    default:
                        exchange.sendResponseHeaders(HTTP_BAD_METHOD, NO_RESPONSE_BODY);
                        badMethod = true;
                        break;
                    }
                }

                if (!badMethod)
                {
                    buildResponse(client, exchange, builder.build());
                }
            }
            catch (Exception ex)
            {
                badGatewayResponse(exchange);
                ex.printStackTrace(System.err);
            }
            finally
            {
                exchange.close();
            }
        }
    }
}
