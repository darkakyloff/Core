package me.darkakyloff.core.modules.core;

import me.darkakyloff.core.api.command.CommandManager;
import me.darkakyloff.core.modules.BaseModule;
import me.darkakyloff.core.modules.core.commands.CoreCommand;
import me.darkakyloff.core.utils.LoggerUtils;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class CoreModule extends BaseModule
{
    public CoreModule(Plugin plugin, String name)
    {
        super(plugin, name, "2.0", "darkakyloff", "Основной модуль системы Core");
    }

    @Override
    public void onLoad()
    {
        LoggerUtils.debug("Загрузка основного модуля Core...");
        
        try
        {
            CommandManager.initialize();

            registerCommands();

            registerListeners();

            initializeUtilities();
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка загрузки основного модуля Core", exception);
            throw new RuntimeException("Критическая ошибка загрузки Core модуля", exception);
        }
    }
    
    @Override
    public void onUnload()
    {
        try
        {
            unregisterCommands();

            unregisterListeners();
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка выгрузки основного модуля Core", exception);
        }
    }

    private void registerCommands()
    {
         new CoreCommand(this);
    }

    private void unregisterCommands()
    {
        CommandManager.unregisterCommands(this);
    }

    private void registerListeners()
    {
        // new PlayerJoinListener();
        // new BlockBreakHandler();
        // new EntityDeathHandler();
    }

    private void unregisterListeners()
    {
        HandlerList.unregisterAll(plugin);
    }

    private void initializeUtilities()
    {
        LoggerUtils.initialize();

        // VaultUtils.setupVault();
        // SoundUtils.initialize();
    }
}