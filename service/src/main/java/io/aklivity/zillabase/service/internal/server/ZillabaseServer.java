package io.aklivity.zillabase.service.internal.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import com.sun.net.httpserver.HttpServer;

import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApiSpecificationIdHandler;
import io.aklivity.zillabase.service.internal.handler.ZillabaseServerAsyncApisHandler;

public class ZillabaseServer implements Runnable
{
    private final HttpServer server;
    private final HttpClient client;
    private final String baseUrl;

    public ZillabaseServer()
    {
        this.baseUrl = "";
        try
        {
            this.server = HttpServer.create(new InetSocketAddress(7184), 0);
            this.client = HttpClient.newHttpClient();
        }
        catch (IOException ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void run()
    {
        server.createContext("/v1/asyncapis", new ZillabaseServerAsyncApisHandler(client, baseUrl));
        server.createContext("/v1/asyncapis/", new ZillabaseServerAsyncApiSpecificationIdHandler(client, baseUrl));

        server.start();

        System.out.println("started");
    }

    public void stop()
    {
        server.stop(0);

        System.out.println("stopped");
    }
}
