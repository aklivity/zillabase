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

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    protected boolean buildResponse(
        HttpClient client,
        HttpExchange exchange,
        HttpRequest request)
    {
        boolean error = false;
        try
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
        catch (Exception ex)
        {
            error = true;
            ex.printStackTrace(System.err);
        }
        return error;
    }
}
