package me.darkakyloff.core.managers;

import me.darkakyloff.core.api.placeholder.PlaceholderHandler;
import me.darkakyloff.core.api.placeholder.PlaceholderExpansion;
import me.darkakyloff.core.modules.BaseModule;
import me.darkakyloff.core.utils.LoggerUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceholderManager
{
    private final Map<String, PlaceholderHandler> internalHandlers;
    private final Map<BaseModule, PlaceholderExpansion> moduleExpansions;
    private PlaceholderExpansion coreExpansion;

    private boolean isInitialized = false;
    private boolean placeholderAPIAvailable = false;

    public PlaceholderManager()
    {
        this.internalHandlers = new ConcurrentHashMap<>();
        this.moduleExpansions = new ConcurrentHashMap<>();
        
        LoggerUtils.debug("PlaceholderManager создан");
    }

    public boolean initialize()
    {
        if (isInitialized)
        {
            LoggerUtils.warning("PlaceholderManager уже инициализирован");
            return true;
        }
        
        LoggerUtils.debug("Инициализация системы плейсхолдеров...");

        checkPlaceholderAPIAvailability();
        
        if (placeholderAPIAvailable)
        {
            registerCoreExpansion();
            registerInternalPlaceholders();
        }
        else
        {
            LoggerUtils.warning("PlaceholderAPI недоступен, используется только внутренняя система");
        }
        
        isInitialized = true;
        LoggerUtils.debug("Система плейсхолдеров инициализирована");
        
        return true;
    }

    private void registerInternalPlaceholders()
    {
        registerInternalHandler("player_name", (player, params) -> player != null ? player.getName() : "Unknown");

        LoggerUtils.debug("Внутренние плейсхолдеры зарегистрированы");
    }

    private void checkPlaceholderAPIAvailability()
    {
        try
        {

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            {
                Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
                
                placeholderAPIAvailable = true;
                LoggerUtils.debug("PlaceholderAPI обнаружен и доступен");
            }
            else LoggerUtils.debug("PlaceholderAPI не установлен");

        }
        catch (ClassNotFoundException exception)
        {
            LoggerUtils.warning("PlaceholderAPI установлен, но классы недоступны: " + exception.getMessage());
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка проверки PlaceholderAPI", exception);
        }
    }

    private void registerCoreExpansion()
    {
        try
        {
            coreExpansion = new PlaceholderExpansion("core", "2.0", "darkakyloff");

            if (coreExpansion.register())
            {
                LoggerUtils.debug("Основная экспансия Core зарегистрирована");
            }
            else
            {
                LoggerUtils.error("Не удалось зарегистрировать основную экспансию Core");
            }
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка регистрации основной экспансии Core", exception);
        }
    }

    public boolean registerPlaceholder(BaseModule module, String identifier, PlaceholderHandler handler)
    {
        if (!isInitialized)
        {
            LoggerUtils.warning("PlaceholderManager не инициализирован");
            return false;
        }
        
        try
        {
            if (placeholderAPIAvailable)
            {
                PlaceholderExpansion expansion = getOrCreateModuleExpansion(module);
                expansion.registerHandler(identifier, handler);
                
                LoggerUtils.debug("Плейсхолдер зарегистрирован в PlaceholderAPI: " + module.getName().toLowerCase() + "_" + identifier);
            }

            String fullIdentifier = module.getName().toLowerCase() + "_" + identifier;
            internalHandlers.put(fullIdentifier, handler);
            
            LoggerUtils.debug("Плейсхолдер зарегистрирован во внутренней системе: " + fullIdentifier);
            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка регистрации плейсхолдера: " + identifier, exception);
            return false;
        }
    }

    public boolean unregisterPlaceholder(BaseModule module, String identifier)
    {
        try
        {
            if (placeholderAPIAvailable)
            {
                PlaceholderExpansion expansion = moduleExpansions.get(module);
                if (expansion != null)
                {
                    expansion.unregisterHandler(identifier);
                }
            }

            String fullIdentifier = module.getName().toLowerCase() + "_" + identifier;
            internalHandlers.remove(fullIdentifier);
            
            LoggerUtils.debug("Плейсхолдер выгружен: " + fullIdentifier);
            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка выгрузки плейсхолдера: " + identifier, exception);
            return false;
        }
    }

    public int unregisterPlaceholders(BaseModule module)
    {
        int unregistered = 0;
        
        try
        {
            if (placeholderAPIAvailable)
            {
                PlaceholderExpansion expansion = moduleExpansions.get(module);
                if (expansion != null)
                {
                    expansion.unregister();
                    moduleExpansions.remove(module);
                    unregistered++;
                }
            }

            String modulePrefix = module.getName().toLowerCase() + "_";
            internalHandlers.entrySet().removeIf(entry -> 
            {
                return entry.getKey().startsWith(modulePrefix);
            });
            
            LoggerUtils.debug("Плейсхолдеры модуля " + module.getName() + " выгружены");
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка выгрузки плейсхолдеров модуля: " + module.getName(), exception);
        }
        
        return unregistered;
    }

    public String setPlaceholders(OfflinePlayer player, String text)
    {
        if (text == null || text.isEmpty()) return text;

        
        String result = text;

        if (placeholderAPIAvailable && player != null)
        {
            try
            {
                result = PlaceholderAPI.setPlaceholders(player, result);
            }
            catch (Exception exception)
            {
                LoggerUtils.error("Ошибка обработки PlaceholderAPI плейсхолдеров", exception);
            }
        }

        for (Map.Entry<String, PlaceholderHandler> entry : internalHandlers.entrySet())
        {
            String placeholder = "%" + entry.getKey() + "%";
            if (result.contains(placeholder))
            {
                try
                {
                    String value = entry.getValue().onPlaceholderRequest(player, entry.getKey());
                    if (value != null)
                    {
                        result = result.replace(placeholder, value);
                    }
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка обработки внутреннего плейсхолдера: " + entry.getKey(), exception);
                }
            }
        }
        
        return result;
    }

    private void registerInternalHandler(String identifier, PlaceholderHandler handler)
    {
        internalHandlers.put(identifier, handler);
    }

    private PlaceholderExpansion getOrCreateModuleExpansion(BaseModule module)
    {
        return moduleExpansions.computeIfAbsent(module, m -> 
        {
            PlaceholderExpansion expansion = new PlaceholderExpansion(
                m.getName().toLowerCase(), 
                m.getVersion(), 
                m.getAuthor()
            );
            expansion.register();
            return expansion;
        });
    }

    public boolean isInitialized()
    {
        return isInitialized;
    }

    public boolean isPlaceholderAPIAvailable()
    {
        return placeholderAPIAvailable;
    }
    

    public void shutdown()
    {
        if (!isInitialized)
        {
            return;
        }
        
        LoggerUtils.debug("Остановка системы плейсхолдеров...");
        
        try
        {
            for (PlaceholderExpansion expansion : moduleExpansions.values())
            {
                try
                {
                    expansion.unregister();
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка выгрузки экспансии плейсхолдеров", exception);
                }
            }

            if (coreExpansion != null)
            {
                try
                {
                    coreExpansion.unregister();
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка выгрузки основной экспансии Core", exception);
                }
            }

            moduleExpansions.clear();
            internalHandlers.clear();
            
            isInitialized = false;
            
            LoggerUtils.debug("Система плейсхолдеров остановлена");
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка остановки системы плейсхолдеров", exception);
        }
    }
}