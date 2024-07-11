package io.aklivity.zillabase.service.internal.handler;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;

import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

public class ZillabaseServerAsyncApiSpecificationIdHandler extends ZillabaseServerHandler
{
    private static final String ARTIFACT_BY_GLOBAL_ID_PATH = "/apis/registry/v2/ids/globalIds/{0}";
    private static final String ARTIFACT_VERSION_PATH = "/apis/registry/v2/groups/{0}/artifacts/{1}";
    private static final Pattern PATH_PATTERN = Pattern.compile("/asyncapis/(.*)");

    private final HttpClient client;
    private final String baseUrl;
    private final Matcher matcher;

    public ZillabaseServerAsyncApiSpecificationIdHandler(
        HttpClient client,
        String baseUrl)
    {
        this.client = client;
        this.baseUrl = baseUrl;
        this.matcher = PATH_PATTERN.matcher("");
    }

    @Override
    public void handle(
        HttpExchange exchange)
    {
        String path = exchange.getRequestURI().getPath();
        if (matcher.reset(path).matches())
        {
            String specId = matcher.group(1);
            String method = exchange.getRequestMethod();
            boolean badMethod = false;
            HttpRequest.Builder builder = HttpRequest.newBuilder(toURI(baseUrl,
                MessageFormat.format(ARTIFACT_VERSION_PATH, "zilla", specId)));

            try
            {
                switch (method)
                {
                    case "GET":
                        builder.uri(toURI(baseUrl, MessageFormat.format(ARTIFACT_BY_GLOBAL_ID_PATH, specId)))
                            .GET();
                        break;
                    case "PUT":
                        exchange.getRequestHeaders().forEach((k, v) -> builder.header(k, String.join(",", v)));
                        builder.PUT(HttpRequest.BodyPublishers.ofInputStream(() -> exchange.getRequestBody()));
                        break;
                    case "DELETE":
                        builder.DELETE();
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
}
