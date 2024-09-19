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
package io.aklivity.zillabase.service.internal.util;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class ZillabaseSsoUtil
{
    private static final String DEFAULT_ADMIN_CREDENTIAL = "admin";
    private static final String CONNECT_TOKEN_PATH = "/realms/master/protocol/openid-connect/token";

    private final HttpClient client;
    private final URI baseUrl;

    public ZillabaseSsoUtil(
        HttpClient client,
        String keycloakUrl)
    {
        this.client = client;
        this.baseUrl = URI.create(keycloakUrl);
    }

    public String fetchAccessToken()
    {
        String token = null;
        try
        {
            String form = "client_id=admin-cli&username=%s&password=%s&grant_type=password"
                .formatted(DEFAULT_ADMIN_CREDENTIAL, DEFAULT_ADMIN_CREDENTIAL);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUrl.resolve(CONNECT_TOKEN_PATH))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();

            if (responseBody != null)
            {
                JsonReader reader = Json.createReader(new StringReader(responseBody));
                JsonObject object = reader.readObject();

                if (object.containsKey("access_token"))
                {
                    token = object.getString("access_token");
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }

        return token;
    }
}
