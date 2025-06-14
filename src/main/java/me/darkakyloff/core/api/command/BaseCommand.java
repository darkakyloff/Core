package me.darkakyloff.core.api.command;

import me.darkakyloff.core.CorePlugin;
import me.darkakyloff.core.api.command.annotations.Command;
import me.darkakyloff.core.api.command.annotations.TabComplete;
import me.darkakyloff.core.api.command.annotations.TabCompletes;
import me.darkakyloff.core.api.message.MessageKeys;
import me.darkakyloff.core.api.message.MessageManager;
import me.darkakyloff.core.modules.BaseModule;
import me.darkakyloff.core.utils.LoggerUtils;
import me.darkakyloff.core.utils.PlayerUtils;
import me.darkakyloff.core.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseCommand extends BukkitCommand
{
    protected final BaseModule module;
    protected final MessageManager messageManager;

    public BaseCommand(BaseModule module, String name)
    {
        super(name);
        this.module = module;

        CorePlugin corePlugin = module.getCorePlugin();
        this.messageManager = corePlugin != null ? corePlugin.getMessageManager() : null;

        if (this.messageManager == null)
        {
            LoggerUtils.warning("MessageManager не инициализирован для команды: " + name);
        }

        registerAnnotatedCommands();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args)
    {
        return handleCommand(sender, commandLabel, args);
    }

    public abstract boolean handleCommand(CommandSender sender, String label, String[] args);

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
    {
        return handleTabComplete(sender, args);
    }

    protected List<String> handleTabComplete(CommandSender sender, String[] args)
    {
        return getTabCompletions(sender, args, this.getClass());
    }

    private void registerAnnotatedCommands()
    {
        Class<?> currentClass = this.getClass();

        if (currentClass.isAnnotationPresent(Command.class))
        {
            Command classCommand = currentClass.getAnnotation(Command.class);
            configureFromAnnotation(classCommand);
        }

        Method[] methods = currentClass.getDeclaredMethods();

        for (Method method : methods)
        {
            if (method.isAnnotationPresent(Command.class))
            {
                if (isValidCommandMethod(method))
                {
                    Command commandAnn = method.getAnnotation(Command.class);
                    BukkitCommand subCommand = createSubCommand(method, commandAnn);

                    CommandManager.registerCommand(module, subCommand);
                }
                else
                {
                    LoggerUtils.warning("Неверная сигнатура метода команды: " + method.getName());
                }
            }
        }
    }

    private void configureFromAnnotation(Command annotation)
    {
        if (annotation.aliases().length > 0)
        {
            setAliases(Arrays.asList(annotation.aliases()));
        }

        if (!annotation.usage().isEmpty())
        {
            setUsage(annotation.usage());
        }
    }

    private boolean isValidCommandMethod(Method method)
    {
        Class<?>[] paramTypes = method.getParameterTypes();
        return paramTypes.length == 3 &&
                CommandSender.class.isAssignableFrom(paramTypes[0]) &&
                String.class.equals(paramTypes[1]) &&
                String[].class.equals(paramTypes[2]) &&
                boolean.class.equals(method.getReturnType());
    }

    private BukkitCommand createSubCommand(Method method, Command annotation)
    {
        return new BukkitCommand(annotation.name())
        {
            {
                if (annotation.aliases().length > 0)
                {
                    setAliases(Arrays.asList(annotation.aliases()));
                }

                if (!annotation.usage().isEmpty())
                {
                    setUsage(annotation.usage());
                }
            }

            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args)
            {
                return executeAnnotatedCommand(sender, label, args, method, annotation);
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args)
            {
                return getTabCompletions(sender, args, BaseCommand.this.getClass());
            }
        };
    }

    private boolean executeAnnotatedCommand(CommandSender sender, String label, String[] args,
                                            Method method, Command annotation)
    {
        try
        {
            String fullCommand = buildFullCommand(label, args);

            if (!hasPermission(sender, annotation.permission()))
            {
                handleNoPermission(sender, annotation.permission(), fullCommand);
                return false;
            }

            if (!annotation.allowConsole() && !(sender instanceof Player))
            {
                if (messageManager != null)
                {
                    messageManager.sendMessage(sender, MessageKeys.ONLY_PLAYER.getKey());
                }
                SoundUtils.playError(sender);
                logCommandUsage(sender, fullCommand, false);
                return false;
            }

            if (!checkArgumentCount(sender, args, annotation, fullCommand))
            {
                return false;
            }

            logCommandUsage(sender, fullCommand, true);

            method.setAccessible(true);
            return (boolean) method.invoke(this, sender, label, args);

        }
        catch (IllegalAccessException | InvocationTargetException exception)
        {
            LoggerUtils.error("Ошибка выполнения команды: " + method.getName(), exception);
            return false;
        }
    }

    private boolean checkArgumentCount(CommandSender sender, String[] args, Command annotation, String fullCommand)
    {
        if (args.length < annotation.minArgs())
        {
            String usage = annotation.usage().isEmpty() ? fullCommand : annotation.usage();

            if (messageManager != null)
            {
                messageManager.sendMessage(sender, usage, "command", annotation.name());
            }
            SoundUtils.playError(sender);
            logCommandUsage(sender, fullCommand, false);
            return false;
        }

        if (annotation.maxArgs() != -1 && args.length > annotation.maxArgs())
        {
            String usage = annotation.usage().isEmpty() ? fullCommand : annotation.usage();
            if (messageManager != null)
            {
                messageManager.sendMessage(sender, usage, "command", annotation.name());
            }
            SoundUtils.playError(sender);
            logCommandUsage(sender, fullCommand, false);
            return false;
        }

        return true;
    }

    private List<String> getTabCompletions(CommandSender sender, String[] args, Class<?> sourceClass)
    {
        if (args.length == 0) return new ArrayList<>();

        List<String> completions = new ArrayList<>();
        int currentArgIndex = args.length - 1;
        String lastArg = args[currentArgIndex].toLowerCase();

        boolean foundCustomTabComplete = false;

        for (Method method : sourceClass.getDeclaredMethods())
        {
            if (method.isAnnotationPresent(TabCompletes.class))
            {
                TabCompletes tabCompletes = method.getAnnotation(TabCompletes.class);

                for (TabComplete tabComplete : tabCompletes.value())
                {
                    if (tabComplete.argumentIndex() == currentArgIndex)
                    {
                        completions.addAll(getCompletionsFromTabComplete(tabComplete, sender, lastArg, sourceClass));
                        foundCustomTabComplete = true;
                    }
                }
            }
            else if (method.isAnnotationPresent(TabComplete.class))
            {
                TabComplete tabComplete = method.getAnnotation(TabComplete.class);

                if (tabComplete.argumentIndex() == currentArgIndex)
                {
                    completions.addAll(getCompletionsFromTabComplete(tabComplete, sender, lastArg, sourceClass));
                    foundCustomTabComplete = true;
                }
            }
        }

        if (!foundCustomTabComplete)
        {
            completions.addAll(getFilteredPlayerNames(lastArg, sender));
        }

        return completions;
    }

    private List<String> getCompletionsFromTabComplete(TabComplete tabComplete, CommandSender sender, String prefix, Class<?> sourceClass)
    {
        if (tabComplete.suggestions().length > 0)
        {
            return filterSuggestions(tabComplete.suggestions(), prefix, tabComplete.filterByInput());
        }

        if (tabComplete.playersList())
        {
            return getFilteredPlayerNames(prefix, sender);
        }

        if (!tabComplete.customListMethod().isEmpty())
        {
            return invokeCustomListMethod(tabComplete.customListMethod(), prefix, sourceClass, tabComplete.filterByInput());
        }

        return new ArrayList<>();
    }

    private List<String> filterSuggestions(String[] suggestions, String prefix, boolean filterByInput)
    {
        if (!filterByInput)
        {
            return Arrays.asList(suggestions);
        }

        return Arrays.stream(suggestions)
                .filter(s -> s.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }

    private List<String> getFilteredPlayerNames(String prefix, CommandSender sender)
    {
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.isOnline())
                .filter(player -> sender.hasPermission("core.vanish.bypass") || !isPlayerHidden(player))
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }

    private boolean isPlayerHidden(Player player)
    {
        PlayerUtils.isHide(player);
        return false;
    }

    private List<String> invokeCustomListMethod(String methodName, String prefix, Class<?> sourceClass, boolean filterByInput)
    {
        try
        {
            Method method = sourceClass.getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object result = method.invoke(this);

            if (result instanceof List<?>)
            {
                List<String> stringList = ((List<?>) result).stream()
                        .filter(item -> item instanceof String)
                        .map(String.class::cast)
                        .collect(Collectors.toList());

                if (filterByInput)
                {
                    return stringList.stream()
                            .filter(s -> s.toLowerCase().startsWith(prefix))
                            .collect(Collectors.toList());
                }

                return stringList;
            }
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка вызова кастомного метода автодополнения: " + methodName, exception);
        }

        return new ArrayList<>();
    }

    private String buildFullCommand(String label, String[] args)
    {
        return "/" + label + (args.length > 0 ? " " + String.join(" ", args) : "");
    }

    private boolean hasPermission(CommandSender sender, String permission)
    {
        return permission.isEmpty() || sender.hasPermission(permission);
    }

    private void handleNoPermission(CommandSender sender, String permission, String fullCommand)
    {
        if (messageManager != null)
        {
            messageManager.sendMessage(sender, MessageKeys.NO_PERMISSION.getKey(), "permission", permission);
        }
        SoundUtils.playError(sender);
        logCommandUsage(sender, fullCommand, false);
    }

    private void logCommandUsage(CommandSender sender, String fullCommand, boolean success)
    {
        LoggerUtils.command(sender.getName(), fullCommand, success);
    }
}