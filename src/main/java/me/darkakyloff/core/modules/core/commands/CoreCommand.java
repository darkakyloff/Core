package me.darkakyloff.core.modules.core.commands;

import me.darkakyloff.core.api.command.BaseCommand;
import me.darkakyloff.core.api.command.annotations.Command;
import me.darkakyloff.core.api.command.annotations.TabComplete;
import me.darkakyloff.core.api.message.MessageKeys;
import me.darkakyloff.core.modules.BaseModule;
import org.bukkit.command.CommandSender;

public class CoreCommand
extends BaseCommand
{
    public CoreCommand(BaseModule module)
    {
        super(module, "test");
    }

    @Command(name = "test", permission = "core.test", minArgs = 1)
    @TabComplete(argumentIndex = 0, suggestions = {"option1", "option2"})
    public boolean handleCommand(CommandSender sender, String label, String[] args)
    {
        messageManager.sendMessage(sender, MessageKeys.ERROR_UNKNOWN.getKey());

        return true;
    }
}
