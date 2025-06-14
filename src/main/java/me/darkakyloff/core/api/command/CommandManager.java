package me.darkakyloff.core.api.command;

import me.darkakyloff.core.CorePlugin;
import me.darkakyloff.core.modules.BaseModule;
import me.darkakyloff.core.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CommandManager
{
    private static final Map<BaseModule, Map<String, BukkitCommand>> moduleCommands = new ConcurrentHashMap<>();
    private static CommandMap commandMap;

    public static void initialize()
    {
        commandMap = getServerCommandMap();

        if (commandMap != null)
        {
            LoggerUtils.debug("CommandManager инициализирован");
        }
        else
        {
            LoggerUtils.error("Не удалось получить CommandMap сервера!");
        }
    }

    public static boolean registerCommand(BaseModule module, BukkitCommand command)
    {
        if (commandMap == null)
        {
            LoggerUtils.error("CommandMap не инициализирован!");
            return false;
        }

        if (module == null || command == null)
        {
            LoggerUtils.error("Модуль или команда не могут быть null!");
            return false;
        }

        try
        {
            String pluginName = module.getCorePlugin().getName();
            boolean registered = commandMap.register(pluginName, command);

            if (registered)
            {
                moduleCommands.computeIfAbsent(module, k -> new HashMap<>()).put(command.getName(), command);

                LoggerUtils.debug("Команда зарегистрирована: /" + command.getName() + " (модуль: " + module.getName() + ")");
                return true;
            }
            else return false;

        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка регистрации команды: /" + command.getName(), exception);
            return false;
        }
    }

    public static boolean unregisterCommand(BukkitCommand command)
    {
        if (commandMap == null || command == null)
        {
            return false;
        }

        try
        {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);

            knownCommands.remove(command.getName());

            for (String alias : command.getAliases())
            {
                knownCommands.remove(alias);
            }
            String pluginPrefix = CorePlugin.getInstance().getName().toLowerCase();
            knownCommands.remove(pluginPrefix + ":" + command.getName());

            command.unregister(commandMap);

            LoggerUtils.debug("Команда выгружена: /" + command.getName());
            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка выгрузки команды: /" + command.getName(), exception);
            return false;
        }
    }

    public static int unregisterCommands(BaseModule module)
    {
        if (module == null)
        {
            return 0;
        }

        Map<String, BukkitCommand> commands = moduleCommands.get(module);

        if (commands == null || commands.isEmpty())
        {
            LoggerUtils.debug("У модуля " + module.getName() + " нет зарегистрированных команд");
            return 0;
        }

        int unregistered = 0;

        for (BukkitCommand command : commands.values())
        {
            if (unregisterCommand(command))
            {
                unregistered++;
            }
        }

        moduleCommands.remove(module);

        LoggerUtils.debug("Выгружено команд для модуля " + module.getName() + ": " + unregistered);
        return unregistered;
    }

    public static int unregisterAllCommands()
    {
        int totalUnregistered = 0;

        for (BaseModule module : moduleCommands.keySet())
        {
            totalUnregistered += unregisterCommands(module);
        }

        LoggerUtils.debug("Все команды выгружены. Всего: " + totalUnregistered);
        return totalUnregistered;
    }

    public static boolean isCommandRegistered(String commandName)
    {
        if (commandMap == null)
        {
            return false;
        }

        Command command = commandMap.getCommand(commandName);
        return command != null;
    }

    public static Command getCommand(String commandName)
    {
        if (commandMap == null)
        {
            return null;
        }

        return commandMap.getCommand(commandName);
    }

    public static Map<String, BukkitCommand> getModuleCommands(BaseModule module)
    {
        Map<String, BukkitCommand> commands = moduleCommands.get(module);
        return commands != null ? new HashMap<>(commands) : new HashMap<>();
    }

    public static int getCommandCount(BaseModule module)
    {
        Map<String, BukkitCommand> commands = moduleCommands.get(module);
        return commands != null ? commands.size() : 0;
    }

    public static int getTotalCommandCount()
    {
        return moduleCommands.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    public static Set<BaseModule> getModulesWithCommands()
    {
        return moduleCommands.keySet();
    }

    public static boolean hasCommands(BaseModule module)
    {
        Map<String, BukkitCommand> commands = moduleCommands.get(module);
        return commands != null && !commands.isEmpty();
    }

    public static boolean reloadCommands(BaseModule module)
    {
        if (module == null)
        {
            return false;
        }

        LoggerUtils.debug("Перезагрузка команд модуля: " + module.getName());

        int unregistered = unregisterCommands(module);

        // TODO: Здесь должна быть логика для повторной регистрации команд модуля

        LoggerUtils.debug("Команды модуля " + module.getName() + " перезагружены. Выгружено: " + unregistered);
        return true;
    }

    public static boolean isAvailable()
    {
        return commandMap != null;
    }


    private static CommandMap getServerCommandMap()
    {
        try
        {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Не удалось получить CommandMap сервера", exception);
            return null;
        }
    }

    public static void forceRefreshCommandMap()
    {
        LoggerUtils.warning("Принудительное обновление CommandMap...");
        commandMap = getServerCommandMap();

        if (commandMap != null)
        {
            LoggerUtils.debug("CommandMap успешно обновлен");
        }
        else
        {
            LoggerUtils.error("Не удалось обновить CommandMap!");
        }
    }

    public static void shutdown()
    {
        LoggerUtils.debug("Остановка CommandManager...");

        int totalUnregistered = unregisterAllCommands();

        moduleCommands.clear();
        commandMap = null;

        LoggerUtils.debug("CommandManager остановлен");
    }
}