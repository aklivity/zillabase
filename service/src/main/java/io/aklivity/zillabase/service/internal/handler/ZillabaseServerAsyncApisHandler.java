package io.aklivity.zillabase.service.internal.handler;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;

import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;

import com.sun.net.httpserver.HttpExchange;

public class ZillabaseServerAsyncApisHandler extends ZillabaseServerHandler
{
    private static final String ARTIFACT_PATH = "/apis/registry/v2/groups/{0}/artifacts";

    private final HttpClient client;
    private final String baseUrl;

    public ZillabaseServerAsyncApisHandler(
        HttpClient client,
        String baseUrl)
    {
        this.client = client;
        this.baseUrl = baseUrl;
    }

    @Override
    public void handle(
        HttpExchange exchange)
    {
        String method = exchange.getRequestMethod();
        HttpRequest.Builder builder = HttpRequest.newBuilder(toURI(baseUrl, MessageFormat.format(ARTIFACT_PATH, "zilla")));
        boolean badMethod = false;
        try
        {
            switch (method)
            {
            case "POST":
                exchange.getRequestHeaders().forEach((k, v) -> builder.header(k, String.join(",", v)));
                builder.header("artifactType", "ASYNCAPI")
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
                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.statusCode(), response.body().length());

                try (OutputStream os = exchange.getResponseBody())
                {
                    os.write(response.body().getBytes());
                }
            }

            exchange.close();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
