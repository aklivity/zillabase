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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.net.httpserver.HttpExchange;

import io.aklivity.zillabase.service.internal.util.ZillabaseAuthUtil;

public class ZillabaseAuthUsersHandler extends ZillabaseServerHandler
{
    private final HttpClient client;
    private final ZillabaseAuthUtil util;
    private final String keycloakUrl;

    public ZillabaseAuthUsersHandler(
        HttpClient client,
        String keycloakUrl)
    {
        this.client = client;
        this.util = new ZillabaseAuthUtil(client, keycloakUrl);
        this.keycloakUrl = keycloakUrl;
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
            String token = util.fetchAccessToken();
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
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode users = objectMapper.readTree(responseBody);
            ArrayNode filteredUsers = objectMapper.createArrayNode();

            for (JsonNode userNode : users)
            {
                filteredUsers.add(filterUserInfo(userNode, objectMapper));
            }

            byte[] responseBytes = objectMapper.writeValueAsBytes(filteredUsers);
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

    private byte[] transformRequestBody(
        InputStream requestBody)
    {
        ObjectMapper mapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try
        {
            Map<String, Object> userRequestMap = mapper.readValue(requestBody, Map.class);

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("username", userRequestMap.get("username"));
            requestBodyMap.put("email", userRequestMap.get("email"));
            requestBodyMap.put("firstName", userRequestMap.get("firstName"));
            requestBodyMap.put("lastName", userRequestMap.get("lastName"));
            requestBodyMap.put("enabled", true);

            Map<String, Object> credentials = new HashMap<>();
            credentials.put("type", "password");
            credentials.put("value", userRequestMap.get("password"));
            credentials.put("temporary", false);

            requestBodyMap.put("credentials", List.of(credentials));

            mapper.writeValue(outputStream, requestBodyMap);
        }
        catch (IOException ex)
        {
            ex.printStackTrace(System.err);
        }
        return outputStream.toByteArray();
    }
}
