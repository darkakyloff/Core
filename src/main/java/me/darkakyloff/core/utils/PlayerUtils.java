package me.darkakyloff.core.utils;

import me.darkakyloff.core.CorePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;

public class PlayerUtils
{
    private static final HashMap<UUID, BukkitTask> hidePlayers = new HashMap<>();

    public static void teleport(Player player, Location location, int seconds)
    {
        if (player == null || location == null) return;

        if (player.hasPermission("core.teleport.bypass"))
        {
            SoundUtils.playSuccess(player);
            player.teleport(location);
            return;
        }

        CorePlugin.getInstance().getTaskScheduler().scheduleAsync("teleport-" + player.getName(), new Runnable()
        {
            int timeLeft = seconds;

            @Override
            public void run()
            {
                if (!player.isOnline()) return;

                if (timeLeft <= 0)
                {
                    CorePlugin.getInstance().getTaskScheduler().scheduleSync("teleport-execute-" + player.getName(), () ->
                    {
                        if (player.isOnline())
                        {
                            player.teleport(location);
                            SoundUtils.playSuccess(player);
                        }
                    }, 1L);
                }
                else
                {
                    CorePlugin.getInstance().getMessageManager().sendMessage(player, "TELEPORT_WAIT", 
                        "time", FormatUtils.formatTime(timeLeft).toUpperCase());
                    SoundUtils.play(player, Sound.BLOCK_NOTE_BLOCK_COW_BELL);

                    timeLeft--;

                    CorePlugin.getInstance().getTaskScheduler().scheduleAsync("teleport-countdown-" + player.getName(), this, 20L);
                }
            }
        }, 0L);
    }

    public static boolean isHide(Player player)
    {
        if (player == null) return false;
        return hidePlayers.containsKey(player.getUniqueId());
    }

    public static void show(Player player)
    {
        if (player == null) return;

        UUID uuid = player.getUniqueId();
        BukkitTask task = hidePlayers.get(uuid);

        if (task != null)
        {
            task.cancel();
            hidePlayers.remove(uuid);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
        {
            onlinePlayer.showPlayer(CorePlugin.getInstance(), player);
        }

        player.sendActionBar(Component.empty());
    }

    public static void hide(Player player)
    {
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        if (isHide(player)) return;

        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimerAsynchronously(CorePlugin.getInstance(), () -> 
        {
            if (player.isOnline())
            {
                String vanishMessage = CorePlugin.getInstance().getConfigurationManager()
                    .getString("localization.yml", "ESSENTIALS_VANISH", "§eВы в режиме невидимости");
                player.sendActionBar(Component.text(vanishMessage));
            }
        }, 1L, 40L);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers())
        {
            if (!onlinePlayer.hasPermission("core.vanish.bypass"))
            {
                onlinePlayer.hidePlayer(CorePlugin.getInstance(), player);
            }
        }

        hidePlayers.put(uuid, actionBarTask);
    }

    public static String getPlayerNameWithUUID(UUID uuid)
    {
        if (uuid == null) return "Неизвестный";

        try
        {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) return player.getName();

            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name != null ? name : "Неизвестный";
        }
        catch (Exception e)
        {
            return "Неизвестный";
        }
    }
}