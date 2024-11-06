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

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class ZillabaseServerHandler implements HttpHandler
{
    protected static final int NO_RESPONSE_BODY = -1;

    protected URI toURI(
        String baseUrl,
        String path)
    {
        return URI.create(baseUrl).resolve(path);
    }

    protected void buildResponse(
        HttpClient client,
        HttpExchange exchange,
        HttpRequest request) throws IOException, InterruptedException
    {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        long contentLength = response.body() != null ? response.body().getBytes().length : 0;
        exchange.sendResponseHeaders(response.statusCode(), contentLength);
        if (response.body() != null && !response.body().isEmpty())
        {
            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(response.body().getBytes());
            }
        }
    }

    protected void badGatewayResponse(
        HttpExchange exchange)
    {
        try
        {
            exchange.sendResponseHeaders(HTTP_BAD_GATEWAY, NO_RESPONSE_BODY);
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    protected ObjectNode filterUserInfo(
        JsonNode userNode,
        ObjectMapper objectMapper)
    {
        ObjectNode filteredUser = objectMapper.createObjectNode();

        filteredUser.put("id", getValue(userNode, "id"));
        filteredUser.put("username", getValue(userNode, "username"));
        filteredUser.put("email", getValue(userNode, "email"));
        filteredUser.put("firstName", getValue(userNode, "firstName"));
        filteredUser.put("lastName", getValue(userNode, "lastName"));
        return filteredUser;
    }

    private String getValue(
        JsonNode node,
        String fieldName)
    {
        return Optional.ofNullable(node)
            .map(n -> n.get(fieldName))
            .map(JsonNode::asText)
            .orElse(null);
    }
}
