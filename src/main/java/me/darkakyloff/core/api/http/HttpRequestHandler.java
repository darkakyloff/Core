package me.darkakyloff.core.api.http;

import com.sun.net.httpserver.HttpExchange;

public interface HttpRequestHandler
{
    HttpResponse handle(HttpExchange exchange) throws Exception;
}