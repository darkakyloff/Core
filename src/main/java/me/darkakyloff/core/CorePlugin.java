package me.darkakyloff.core;

import me.darkakyloff.core.api.command.CommandManager;
import me.darkakyloff.core.api.config.ConfigurationManager;
import me.darkakyloff.core.api.message.MessageManager;
import me.darkakyloff.core.api.telegram.TelegramService;
import me.darkakyloff.core.managers.ModuleManager;
import me.darkakyloff.core.managers.HttpServerManager;
import me.darkakyloff.core.managers.PlaceholderManager;
import me.darkakyloff.core.tasks.AsyncTaskScheduler;
import me.darkakyloff.core.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CorePlugin extends JavaPlugin
{
    private static CorePlugin instance;

    private ConfigurationManager configManager;
    private MessageManager messageManager;
    private ModuleManager moduleManager;
    private HttpServerManager httpServerManager;
    private PlaceholderManager placeholderManager;
    private TelegramService telegramService;
    private AsyncTaskScheduler taskScheduler;

    @Override
    public void onEnable()
    {
        instance = this;

        LoggerUtils.debug("Запуск плагина Core v2.0...");

        if (!initializeCore())
        {
            LoggerUtils.error("Критическая ошибка при инициализации! Отключение плагина...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        LoggerUtils.debug("Плагин Core успешно запущен!");
    }

    @Override
    public void onDisable()
    {
        LoggerUtils.debug("Остановка плагина Core...");

        shutdownCore();

        LoggerUtils.debug("Плагин Core успешно остановлен!");

        instance = null;
    }

    private boolean initializeCore()
    {
        try
        {
            LoggerUtils.initialize();

            initializeConfigurationManager();
            initializeMessageManager();
            initializePlaceholderManager();
            initializeHttpServerManager();
            initializeTelegramService();
            initializeTaskScheduler();

            CommandManager.initialize();

            initializeModuleManager();

            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка при инициализации: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    private void shutdownCore()
    {
        try
        {
            shutdownModuleManager();
            shutdownTaskScheduler();
            shutdownTelegramService();
            shutdownHttpServerManager();
            shutdownPlaceholderManager();

            closeAllPlayerInventories();
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка при остановке: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void initializeConfigurationManager()
    {
        configManager = new ConfigurationManager(this);

        configManager.loadConfig("settings.yml");
        configManager.loadConfig("localization.yml");
        configManager.loadConfig("database.yml");
    }

    private void initializeMessageManager()
    {
        messageManager = new MessageManager(configManager);
        messageManager.initialize();
    }

    private void initializePlaceholderManager()
    {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
        {
            placeholderManager = new PlaceholderManager();
            placeholderManager.initialize();
        }
        else
        {
            LoggerUtils.warning("PlaceholderAPI не найден, плейсхолдеры отключены");
        }
    }

    private void initializeHttpServerManager()
    {
        int port = configManager.getInt("settings.yml", "http-server.port", 8080);

        httpServerManager = new HttpServerManager(port);
        httpServerManager.startServer();
    }

    private void initializeTelegramService()
    {
        String token = configManager.getString("settings.yml", "telegram.bot-token");

        if (token != null && !token.isEmpty())
        {
            telegramService = new TelegramService(token);
            telegramService.initialize();
        }
        else
        {
            LoggerUtils.warning("Токен Telegram бота не настроен");
        }
    }

    private void initializeTaskScheduler()
    {
        taskScheduler = new AsyncTaskScheduler(this);
        taskScheduler.startTasks();
    }

    private void initializeModuleManager()
    {
        moduleManager = new ModuleManager(this);
        moduleManager.loadAllModules();
    }

    private void shutdownModuleManager()
    {
        if (moduleManager != null)
        {
            moduleManager.unloadAllModules();
        }

        CommandManager.shutdown();
    }

    private void shutdownTaskScheduler()
    {
        if (taskScheduler != null)
        {
            taskScheduler.stopTasks();
        }
    }

    private void shutdownTelegramService()
    {
        if (telegramService != null)
        {
            telegramService.shutdown();
        }
    }

    private void shutdownHttpServerManager()
    {
        if (httpServerManager != null)
        {
            httpServerManager.stopServer();
        }
    }

    private void shutdownPlaceholderManager()
    {
        if (placeholderManager != null)
        {
            placeholderManager.shutdown();
            LoggerUtils.debug("Система плейсхолдеров остановлена");
        }
    }

    private void closeAllPlayerInventories()
    {
        if (this.isEnabled())
        {
            Bukkit.getScheduler().runTask(this, () ->
            {
                for (Player player : Bukkit.getOnlinePlayers())
                {
                    player.closeInventory();
                }
            });
        }
    }

    public static CorePlugin getInstance()
    {
        return instance;
    }

    public ConfigurationManager getConfigurationManager()
    {
        return configManager;
    }

    public MessageManager getMessageManager()
    {
        return messageManager;
    }

    public ModuleManager getModuleManager()
    {
        return moduleManager;
    }

    public HttpServerManager getHttpServerManager()
    {
        return httpServerManager;
    }

    public PlaceholderManager getPlaceholderManager()
    {
        return placeholderManager;
    }

    public TelegramService getTelegramService()
    {
        return telegramService;
    }

    public AsyncTaskScheduler getTaskScheduler()
    {
        return taskScheduler;
    }
}