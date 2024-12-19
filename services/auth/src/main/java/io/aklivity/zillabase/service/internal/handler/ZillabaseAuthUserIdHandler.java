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

import java.io.IOException;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import com.sun.net.httpserver.HttpExchange;

import io.aklivity.zillabase.service.internal.helper.ZillabaseAuthHelper;
import io.aklivity.zillabase.service.internal.model.ZillabaseAuthUserInfo;

public final class ZillabaseAuthUserIdHandler extends ZillabaseServerHandler
{
    private static final Pattern PATH_PATTERN = Pattern.compile("/v1/auth/users/(.*)");

    private final HttpClient client;
    private final ZillabaseAuthHelper helper;
    private final String url;
    private final Matcher matcher;
    private final Jsonb jsonb;
    private final String realm;

    public ZillabaseAuthUserIdHandler(
        HttpClient client,
        String url,
        String realm)
    {
        this.client = client;
        this.helper = new ZillabaseAuthHelper(client, url);
        this.url = url;
        this.matcher = PATH_PATTERN.matcher("");
        this.jsonb = JsonbBuilder.newBuilder().build();
        this.realm = realm;
    }

    @Override
    public void handle(
        HttpExchange exchange)
    {
        String path = exchange.getRequestURI().getPath();
        if (matcher.reset(path).matches())
        {
            String userId = matcher.group(1);
            String method = exchange.getRequestMethod();
            HttpRequest.Builder builder = HttpRequest.newBuilder(toURI(url,
                "/admin/realms/%s/users/%s".formatted(realm, userId)));

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
                        handleExchangeForGetRequest(exchange, builder.build());
                        break;
                    case "PUT":
                        builder.header("Authorization", "Bearer " + token)
                            .header("Content-Type", "application/json")
                            .PUT(HttpRequest.BodyPublishers.ofByteArray(exchange.getRequestBody().readAllBytes()));
                        buildResponse(client, exchange, builder.build());
                        break;
                    case "DELETE":
                        builder.header("Authorization", "Bearer %s".formatted(token))
                            .DELETE();
                        buildResponse(client, exchange, builder.build());
                        break;
                    default:
                        exchange.sendResponseHeaders(HTTP_BAD_METHOD, NO_RESPONSE_BODY);
                        break;
                    }
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

    private void handleExchangeForGetRequest(
        HttpExchange exchange,
        HttpRequest request) throws IOException, InterruptedException
    {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        if (response.statusCode() == 200 && responseBody != null && !responseBody.isEmpty())
        {
            ZillabaseAuthUserInfo user = jsonb.fromJson(responseBody, ZillabaseAuthUserInfo.class);

            byte[] responseBytes = jsonb.toJson(user).getBytes();
            exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(responseBytes);
            }
        }
        else
        {
            exchange.sendResponseHeaders(response.statusCode(), 0);
        }
    }
}
