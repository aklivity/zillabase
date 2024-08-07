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
import static java.net.HttpURLConnection.HTTP_BAD_METHOD;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

public class ZillabaseConfigServerHandler extends ZillabaseServerHandler
{
    private static final Pattern PATH_PATTERN = Pattern.compile("/v1/config/(.*)");

    private final HttpClient client;
    private final Matcher matcher;

    public ZillabaseConfigServerHandler(
        HttpClient client)
    {
        this.client = client;
        this.matcher = PATH_PATTERN.matcher("");
    }

    @Override
    public void handle(
        HttpExchange exchange)
    {
        String path = exchange.getRequestURI().getPath();
        if (matcher.reset(path).matches())
        {
            String key = matcher.group(1);
            String method = exchange.getRequestMethod();
            boolean badMethod = false;

            try
            {
                HttpRequest.Builder builder = HttpRequest.newBuilder(toURI("http://config.zillabase.dev:7114",
                    "/config/%s".formatted(key)));

                switch (method)
                {
                case "GET":
                    builder.GET();
                    break;
                case "PUT":
                    builder.header("Content-Length", exchange.getRequestHeaders().getFirst("Content-Length"))
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(exchange.getRequestBody().readAllBytes()));
                    break;
                default:
                    exchange.sendResponseHeaders(HTTP_BAD_METHOD, NO_RESPONSE_BODY);
                    badMethod = true;
                    break;
                }

                if (!badMethod)
                {
                    boolean error = buildResponse(client, exchange, builder.build());

                    if (error)
                    {
                        exchange.sendResponseHeaders(HTTP_BAD_GATEWAY, NO_RESPONSE_BODY);
                    }
                }

                exchange.close();
            }
            catch (Exception ex)
            {
                ex.printStackTrace(System.err);
            }
        }
    }
}
