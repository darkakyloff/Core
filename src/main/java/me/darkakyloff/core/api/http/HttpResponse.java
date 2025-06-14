package me.darkakyloff.core.api.http;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse
{
    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    public HttpResponse(int statusCode, Map<String, String> headers, String body)
    {
        this.statusCode = statusCode;
        this.headers = new HashMap<>(headers);
        this.body = body != null ? body : "";
    }

    public HttpResponse(int statusCode, String contentType, String body)
    {
        this.statusCode = statusCode;
        this.headers = new HashMap<>();
        this.body = body != null ? body : "";
        
        if (contentType != null && !contentType.isEmpty())
        {
            this.headers.put("Content-Type", contentType);
        }
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public Map<String, String> getHeaders()
    {
        return new HashMap<>(headers);
    }

    public String getBody()
    {
        return body;
    }

    public HttpResponse withHeader(String name, String value)
    {
        Map<String, String> newHeaders = new HashMap<>(this.headers);
        newHeaders.put(name, value);
        return new HttpResponse(statusCode, newHeaders, body);
    }

    public static HttpResponse ok(String contentType, String body)
    {
        return new HttpResponse(200, contentType, body);
    }

    public static HttpResponse html(String html)
    {
        return ok("text/html; charset=utf-8", html);
    }

    public static HttpResponse json(String json)
    {
        return ok("application/json; charset=utf-8", json);
    }

    public static HttpResponse text(String text)
    {
        return ok("text/plain; charset=utf-8", text);
    }

    public static HttpResponse notFound(String message)
    {
        return new HttpResponse(404, "text/plain; charset=utf-8", 
                               message != null ? message : "Not Found");
    }

    public static HttpResponse badRequest(String message)
    {
        return new HttpResponse(400, "text/plain; charset=utf-8", 
                               message != null ? message : "Bad Request");
    }

    public static HttpResponse unauthorized(String message)
    {
        return new HttpResponse(401, "text/plain; charset=utf-8", 
                               message != null ? message : "Unauthorized");
    }

    public static HttpResponse forbidden(String message)
    {
        return new HttpResponse(403, "text/plain; charset=utf-8", 
                               message != null ? message : "Forbidden");
    }

    public static HttpResponse internalError(String message)
    {
        return new HttpResponse(500, "text/plain; charset=utf-8", 
                               message != null ? message : "Internal Server Error");
    }

    public static HttpResponse status(int statusCode, String contentType, String body)
    {
        return new HttpResponse(statusCode, contentType, body);
    }

    public static HttpResponse redirect(String location)
    {
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", location);
        return new HttpResponse(302, headers, "");
    }

    public static HttpResponse permanentRedirect(String location)
    {
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", location);
        return new HttpResponse(301, headers, "");
    }
}