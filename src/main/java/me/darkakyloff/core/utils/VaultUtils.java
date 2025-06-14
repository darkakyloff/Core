package me.darkakyloff.core.utils;

import me.darkakyloff.core.utils.LoggerUtils;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class VaultUtils
{
    public static Chat chat = null;
    public static Permission permission = null;

    public static void setupVault()
    {
        RegisteredServiceProvider<Chat> chatRsp = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        if (chatRsp != null) 
        {
            chat = chatRsp.getProvider();
            LoggerUtils.debug("Vault Chat подключен");
        }

        RegisteredServiceProvider<Permission> permissionRsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionRsp != null) 
        {
            permission = permissionRsp.getProvider();
            LoggerUtils.debug("Vault Permission подключен");
        }
    }

    public static String getPlayerPrefix(Player player)
    {
        if (chat == null || player == null) return "";
        
        try
        {
            return chat.getPlayerPrefix(player);
        }
        catch (Exception e)
        {
            LoggerUtils.error("Ошибка получения префикса игрока: " + player.getName(), e);
            return "";
        }
    }

    public static String getPlayerPrefix(String uuid)
    {
        if (chat == null || uuid == null) return "";

        try
        {
            Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(uuid));
            if (onlinePlayer != null) 
            {
                return chat.getPlayerPrefix(onlinePlayer);
            }
            else
            {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                return chat.getPlayerPrefix(null, offlinePlayer);
            }
        }
        catch (Exception e)
        {
            LoggerUtils.error("Ошибка получения префикса по UUID: " + uuid, e);
            return "";
        }
    }

    public static String getPlayerSuffix(Player player)
    {
        if (chat == null || player == null) return "";
        
        try
        {
            return chat.getPlayerSuffix(player);
        }
        catch (Exception e)
        {
            LoggerUtils.error("Ошибка получения суффикса игрока: " + player.getName(), e);
            return "";
        }
    }

    public static String getPlayerGroup(Player player)
    {
        if (permission == null || player == null) return null;

        try
        {
            String[] groups = permission.getPlayerGroups(player);
            return (groups.length > 0) ? groups[0] : null;
        }
        catch (Exception e)
        {
            LoggerUtils.error("Ошибка получения группы игрока: " + player.getName(), e);
            return null;
        }
    }
}