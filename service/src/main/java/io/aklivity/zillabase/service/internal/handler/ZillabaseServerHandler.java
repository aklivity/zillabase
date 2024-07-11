package io.aklivity.zillabase.service.internal.handler;

import java.net.URI;

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
}
