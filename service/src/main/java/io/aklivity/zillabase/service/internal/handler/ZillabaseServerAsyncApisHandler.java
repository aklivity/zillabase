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
import java.text.MessageFormat;

import com.sun.net.httpserver.HttpExchange;

public class ZillabaseServerAsyncApisHandler extends ZillabaseServerHandler
{
    private static final String ARTIFACT_PATH = "/apis/registry/v2/groups/{0}/artifacts";

    private final HttpClient client;
    private final String baseUrl;
    private final String groupId;

    public ZillabaseServerAsyncApisHandler(
        HttpClient client,
        String baseUrl,
        String groupId)
    {
        this.client = client;
        this.baseUrl = baseUrl;
        this.groupId = groupId;
    }

    @Override
    public void handle(
        HttpExchange exchange)
    {
        String method = exchange.getRequestMethod();
        HttpRequest.Builder builder = HttpRequest.newBuilder(toURI(baseUrl, MessageFormat.format(ARTIFACT_PATH, groupId)));
        boolean badMethod = false;
        try
        {
            switch (method)
            {
            case "POST":
                builder.header("Content-Type", "application/vnd.aai.asyncapi+yaml")
                    .header("artifactType", "ASYNCAPI")
                    .POST(HttpRequest.BodyPublishers.ofInputStream(() -> exchange.getRequestBody()));
                break;
            case "GET":
                builder.GET();
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
