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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import com.sun.net.httpserver.HttpExchange;

import io.aklivity.zillabase.service.internal.helper.ZillabaseAuthHelper;
import io.aklivity.zillabase.service.internal.model.ZillabaseAuthUserInfo;
import io.aklivity.zillabase.service.internal.model.ZillabaseAuthUserRequest;
import io.aklivity.zillabase.service.internal.model.ZillabaseAuthUserRequestView;

public final class ZillabaseAuthUsersHandler extends ZillabaseServerHandler
{
    private final HttpClient client;
    private final ZillabaseAuthHelper helper;
    private final String keycloakUrl;
    private final Jsonb jsonb;

    public ZillabaseAuthUsersHandler(
        HttpClient client,
        String keycloakUrl)
    {
        this.client = client;
        this.helper = new ZillabaseAuthHelper(client, keycloakUrl);
        this.keycloakUrl = keycloakUrl;
        this.jsonb = JsonbBuilder.newBuilder().build();
    }

    @Override
    public void handle(
        HttpExchange exchange)
    {
        String method = exchange.getRequestMethod();
        HttpRequest.Builder builder = HttpRequest.newBuilder(toURI(keycloakUrl,
            "/admin/realms/%s/users".formatted(
                exchange.getRequestHeaders().getFirst("Keycloak-Realm"))));
        try
        {
            String token = helper.fetchAccessToken();
            if (token != null)
            {
                switch (method)
                {
                case "POST":
                    builder.header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(transformRequestBody(exchange.getRequestBody())));
                    buildResponse(client, exchange, builder.build());
                    break;
                case "GET":
                    builder.header("Authorization", "Bearer %s".formatted(token))
                        .GET();
                    handleExchangeForGetRequest(exchange, builder.build());
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

    private void handleExchangeForGetRequest(
        HttpExchange exchange,
        HttpRequest request) throws IOException, InterruptedException
    {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        if (response.statusCode() == 200 && responseBody != null && !responseBody.isEmpty())
        {
            try (JsonReader jsonReader = Json.createReader(new StringReader(responseBody)))
            {
                List<ZillabaseAuthUserInfo> users = new ArrayList<>();
                JsonArray usersList = jsonReader.readArray();
                for (JsonValue user : usersList)
                {
                    users.add(jsonb.fromJson(user.toString(), ZillabaseAuthUserInfo.class));
                }
                byte[] responseBytes = jsonb.toJson(users).getBytes();
                exchange.sendResponseHeaders(response.statusCode(), responseBytes.length);
                try (OutputStream os = exchange.getResponseBody())
                {
                    os.write(responseBytes);
                }
            }
        }
        else
        {
            exchange.sendResponseHeaders(response.statusCode(), 0);
        }
    }

    private byte[] transformRequestBody(
        InputStream requestBody)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try
        {
            ZillabaseAuthUserRequest user = jsonb.fromJson(requestBody, ZillabaseAuthUserRequest.class);

            ZillabaseAuthUserRequestView request = ZillabaseAuthUserRequestView.of(user);

            outputStream.write(jsonb.toJson(request).getBytes());
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
        return outputStream.toByteArray();
    }
}
