package me.darkakyloff.core.api.placeholder;

import me.darkakyloff.core.utils.LoggerUtils;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion
{
    private final String identifier;
    private final String version;
    private final String author;
    private final Map<String, PlaceholderHandler> handlers;

    public PlaceholderExpansion(String identifier, String version, String author)
    {
        this.identifier = identifier.toLowerCase();
        this.version = version;
        this.author = author;
        this.handlers = new ConcurrentHashMap<>();
        
        LoggerUtils.debug("Создана экспансия плейсхолдеров: " + identifier);
    }
    
    @Override
    public String getIdentifier()
    {
        return identifier;
    }
    
    @Override
    public String getVersion()
    {
        return version;
    }
    
    @Override
    public String getAuthor()
    {
        return author;
    }
    
    @Override
    public boolean persist()
    {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String params)
    {
        if (params == null || params.isEmpty())
        {
            return null;
        }
        
        PlaceholderHandler handler = handlers.get(params.toLowerCase());
        
        if (handler != null)
        {
            try
            {
                return handler.onPlaceholderRequest(player, params);
            }
            catch (Exception exception)
            {
                LoggerUtils.error("Ошибка обработки плейсхолдера: " + identifier + "_" + params, exception);
                return "ERROR";
            }
        }
        
        for (Map.Entry<String, PlaceholderHandler> entry : handlers.entrySet())
        {
            String handlerKey = entry.getKey();
            
            if (params.toLowerCase().startsWith(handlerKey.toLowerCase() + "_"))
            {
                try
                {
                    return entry.getValue().onPlaceholderRequest(player, params);
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка обработки параметризованного плейсхолдера: " + 
                                     identifier + "_" + params, exception);
                    return "ERROR";
                }
            }
        }
        
        return null;
    }

    public void registerHandler(String placeholder, PlaceholderHandler handler)
    {
        if (placeholder == null || placeholder.isEmpty() || handler == null)
        {
            LoggerUtils.warning("Невалидные параметры для регистрации плейсхолдера");
            return;
        }
        
        handlers.put(placeholder.toLowerCase(), handler);
        LoggerUtils.debug("Зарегистрирован плейсхолдер: " + identifier + "_" + placeholder);
    }

    public void unregisterHandler(String placeholder)
    {
        if (placeholder == null || placeholder.isEmpty())
        {
            return;
        }
        
        handlers.remove(placeholder.toLowerCase());
        LoggerUtils.debug("Выгружен плейсхолдер: " + identifier + "_" + placeholder);
    }

    public boolean hasHandler(String placeholder)
    {
        return placeholder != null && handlers.containsKey(placeholder.toLowerCase());
    }

    public int getHandlerCount()
    {
        return handlers.size();
    }

    public java.util.Set<String> getRegisteredPlaceholders()
    {
        return handlers.keySet();
    }

    public void clearHandlers()
    {
        handlers.clear();
        LoggerUtils.debug("Очищены все плейсхолдеры экспансии: " + identifier);
    }
}