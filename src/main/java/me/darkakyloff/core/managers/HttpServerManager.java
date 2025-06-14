package me.darkakyloff.core.managers;

import me.darkakyloff.core.api.http.HttpEndpoint;
import me.darkakyloff.core.api.http.HttpRequestHandler;
import me.darkakyloff.core.api.http.HttpResponse;
import me.darkakyloff.core.modules.BaseModule;
import me.darkakyloff.core.utils.LoggerUtils;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class HttpServerManager
{
    private final int port;
    private HttpServer server;
    private final Map<String, HttpContext> contexts;
    private final Map<BaseModule, Map<String, HttpContext>> moduleContexts;

    private boolean isRunning = false;
    private boolean debugMode = false;

    public HttpServerManager(int port)
    {
        this.port = port;
        this.contexts = new ConcurrentHashMap<>();
        this.moduleContexts = new ConcurrentHashMap<>();
        
        LoggerUtils.debug("HttpServerManager создан для порта " + port);
    }

    public boolean startServer()
    {
        if (isRunning)
        {
            LoggerUtils.warning("HTTP сервер уже запущен на порту " + port);
            return true;
        }
        
        try
        {
            LoggerUtils.debug("Запуск HTTP сервера на порту " + port + "...");

            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.setExecutor(Executors.newCachedThreadPool(runnable -> 
            {
                Thread thread = new Thread(runnable, "HTTP-Worker");
                thread.setDaemon(true);
                return thread;
            }));

            server.start();
            isRunning = true;
            
            LoggerUtils.debug("HTTP сервер запущен на порту " + port);
            return true;
        }
        catch (IOException exception)
        {
            LoggerUtils.error("Не удалось запустить HTTP сервер на порту " + port, exception);
            return false;
        }
    }

    public void stopServer()
    {
        if (!isRunning)
        {
            LoggerUtils.warning("HTTP сервер не запущен");
            return;
        }
        
        try
        {
            LoggerUtils.debug("Остановка HTTP сервера...");
            
            server.stop(2);
            isRunning = false;

            contexts.clear();
            moduleContexts.clear();
            
            LoggerUtils.debug("HTTP сервер остановлен");
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка остановки HTTP сервера", exception);
        }
    }

    private void registerDefaultEndpoints()
    {

        LoggerUtils.debug("Базовые эндпоинты зарегистрированы");
    }

    public boolean registerEndpoint(String path, HttpRequestHandler handler)
    {
        return registerEndpoint(null, path, handler);
    }

    public boolean registerEndpoint(BaseModule module, String path, HttpRequestHandler handler)
    {
        if (server == null)
        {
            LoggerUtils.error("HTTP сервер не инициализирован");
            return false;
        }
        
        if (path == null || handler == null)
        {
            LoggerUtils.error("Путь и обработчик не могут быть null");
            return false;
        }
        
        try
        {
            if (contexts.containsKey(path))
            {
                server.removeContext(contexts.get(path));
            }
            
            HttpContext context = server.createContext(path, exchange -> handleRequest(exchange, handler, path));
            contexts.put(path, context);

            if (module != null)
            {
                moduleContexts.computeIfAbsent(module, k -> new ConcurrentHashMap<>()).put(path, context);
            }
            
            LoggerUtils.debug("Эндпоинт зарегистрирован: " + path + 
                             (module != null ? " (модуль: " + module.getName() + ")" : ""));
            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка регистрации эндпоинта: " + path, exception);
            return false;
        }
    }

    public boolean registerEndpoint(BaseModule module, HttpEndpoint endpoint, HttpRequestHandler handler)
    {
        String fullPath = endpoint.basePath() + endpoint.path();
        return registerEndpoint(module, fullPath, handler);
    }

    public boolean unregisterEndpoint(String path)
    {
        if (server == null || !contexts.containsKey(path))
        {
            return false;
        }
        
        try
        {
            HttpContext context = contexts.get(path);
            server.removeContext(context);
            contexts.remove(path);

            for (Map<String, HttpContext> moduleMap : moduleContexts.values())
            {
                moduleMap.remove(path);
            }
            
            LoggerUtils.debug("Эндпоинт выгружен: " + path);
            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка выгрузки эндпоинта: " + path, exception);
            return false;
        }
    }

    public int unregisterEndpoints(BaseModule module)
    {
        Map<String, HttpContext> moduleMap = moduleContexts.get(module);
        
        if (moduleMap == null)
        {
            return 0;
        }
        
        int unregistered = 0;
        
        for (String path : moduleMap.keySet())
        {
            if (unregisterEndpoint(path))
            {
                unregistered++;
            }
        }
        
        moduleContexts.remove(module);
        LoggerUtils.debug("Выгружено эндпоинтов для модуля " + module.getName() + ": " + unregistered);
        
        return unregistered;
    }

    private void handleRequest(HttpExchange exchange, HttpRequestHandler handler, String path)
    {
        long startTime = System.currentTimeMillis();

        String method = exchange.getRequestMethod();
        String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();
        
        try
        {
            if (debugMode)
            {
                LoggerUtils.debug("HTTP запрос: " + method + " " + path + " от " + clientIP);
            }

            HttpResponse response = handler.handle(exchange);

            sendResponse(exchange, response);
            
            long duration = System.currentTimeMillis() - startTime;

            LoggerUtils.http(method, path, response.getStatusCode());
            
            if (debugMode)
            {
                LoggerUtils.debug("HTTP ответ: " + response.getStatusCode() + " за " + duration + "мс");
            }
        }
        catch (Exception exception)
        {
            long duration = System.currentTimeMillis() - startTime;

            LoggerUtils.error("Ошибка HTTP запроса " + method + " " + path + " за " + duration + "мс", exception);
            
            try
            {
                sendErrorResponse(exchange, 500, "Внутренняя ошибка сервера");
            }
            catch (IOException ioException)
            {
                LoggerUtils.error("Не удалось отправить ошибку HTTP", ioException);
            }
        }
    }

    private void sendResponse(HttpExchange exchange, HttpResponse response) throws IOException
    {
        for (Map.Entry<String, String> header : response.getHeaders().entrySet())
        {
            exchange.getResponseHeaders().set(header.getKey(), header.getValue());
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        byte[] responseBytes = response.getBody().getBytes("UTF-8");
        exchange.sendResponseHeaders(response.getStatusCode(), responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody())
        {
            os.write(responseBytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException
    {
        byte[] responseBytes = message.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream os = exchange.getResponseBody())
        {
            os.write(responseBytes);
        }
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    public int getPort()
    {
        return port;
    }

    public int getEndpointCount()
    {
        return contexts.size();
    }

    public java.util.Set<String> getRegisteredPaths()
    {
        return contexts.keySet();
    }
}