package me.darkakyloff.core.api.placeholder;

import org.bukkit.OfflinePlayer;

public interface PlaceholderHandler
{
    String onPlaceholderRequest(OfflinePlayer player, String params);
}