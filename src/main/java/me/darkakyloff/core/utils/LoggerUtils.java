package me.darkakyloff.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

public class LoggerUtils
{
    private static final String COLOR_INFO = ChatColor.GOLD.toString();
    private static final String COLOR_SUCCESS = ChatColor.GREEN.toString();
    private static final String COLOR_WARNING = ChatColor.YELLOW.toString();
    private static final String COLOR_ERROR = ChatColor.RED.toString();
    private static final String COLOR_DEBUG = ChatColor.LIGHT_PURPLE.toString();
    private static final String COLOR_RESET = ChatColor.RESET.toString();

    private static ConsoleCommandSender console;
    private static boolean debugMode = false;

    public static void initialize()
    {
        console = Bukkit.getConsoleSender();

        debug("Система логирования инициализирована");
    }

    public static void setDebugMode(boolean enabled)
    {
        debugMode = enabled;
        info("Режим отладки " + (enabled ? "включен" : "отключен"));
    }

    public static boolean isDebugMode()
    {
        return debugMode;
    }

    public static void info(String message)
    {
        logToConsole(COLOR_INFO + "[ИНФО]" + COLOR_RESET, message);
    }

    public static void warning(String message)
    {
        logToConsole(COLOR_WARNING + "[ВНИМАНИЕ]" + COLOR_RESET, message);
    }

    public static void error(String message)
    {
        logToConsole(COLOR_ERROR + "[ОШИБКА]" + COLOR_RESET, message);
    }

    public static void error(String message, Throwable throwable)
    {
        error(message);
        error("Детали ошибки: " + throwable.getMessage());

        if (debugMode) throwable.printStackTrace();
    }

    public static void debug(String message)
    {
        if (debugMode)
        {
            logToConsole(COLOR_DEBUG + "[ОТЛАДКА]" + COLOR_RESET, message);
        }
    }

    public static void module(String moduleName, String status)
    {
        String color = getStatusColor(status);
        logToConsole(color + "[МОДУЛЬ]" + COLOR_RESET, "Модуль " + moduleName + " - " + status);
    }

    public static void command(String playerName, String command, boolean success)
    {
        String status = success ? "выполнена" : "отклонена";
        String color = success ? COLOR_SUCCESS : COLOR_ERROR;

        logToConsole(color + "[КОМАНДА]" + COLOR_RESET, "Игрок " + playerName + " - команда " + command + " " + status);
    }

    public static void database(String status, String details)
    {
        String color = getStatusColor(status);
        logToConsole(color + "[БД]" + COLOR_RESET, status + " - " + details);
    }

    public static void http(String method, String path, int responseCode)
    {
        String color = responseCode < 400 ? COLOR_SUCCESS : COLOR_ERROR;
        logToConsole(color + "[HTTP]" + COLOR_RESET, method + " " + path + " - " + responseCode);
    }

    public static void separator(String title)
    {
        String separator = "================================================";
        logToConsole(COLOR_INFO + separator + COLOR_RESET, "");
        logToConsole(COLOR_INFO + "  " + title + COLOR_RESET, "");
        logToConsole(COLOR_INFO + separator + COLOR_RESET, "");
    }

    public static void stats(String title, String... stats)
    {
        logToConsole(COLOR_INFO + "[ИНФОРМАЦИЯ]" + COLOR_RESET, title);

        for (int i = 0; i < stats.length; i += 2)
        {
            if (i + 1 < stats.length)
            {
                String key = stats[i];
                String value = stats[i + 1];
                logToConsole(COLOR_INFO + "  →" + COLOR_RESET, key + ": " + value);
            }
        }
    }


    private static String getStatusColor(String status)
    {
        return switch (status.toLowerCase())
        {
            case "загружен", "успех", "подключено", "запущен" -> COLOR_SUCCESS;
            case "ошибка", "отключено", "провал", "остановлен" -> COLOR_ERROR;
            case "предупреждение", "внимание" -> COLOR_WARNING;
            default -> COLOR_INFO;
        };
    }

    private static void logToConsole(String prefix, String message)
    {
        if (console != null)
        {
            String fullMessage = prefix + " " + message;
            console.sendMessage(fullMessage);
        }
    }
}