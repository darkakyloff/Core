package me.darkakyloff.core.utils;

import me.darkakyloff.core.CorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;

public class MetaUtils
{
    private static final Map<Player, Map<String, String>> taskMap = new HashMap<>();

    public static void setTempMeta(Player player, boolean notify, String data, String value, long seconds)
    {
        if (player == null || data == null) return;

        if (player.hasMetadata(data))
        {
            LoggerUtils.debug("Игрок " + player.getName() + " уже имеет метаданные: " + data);
            return;
        }

        player.setMetadata(data, new FixedMetadataValue(CorePlugin.getInstance(), value));

        String taskId = CorePlugin.getInstance().getTaskScheduler().scheduleAsync(
            "meta-remove-" + player.getName() + "-" + data,
            () -> removeMeta(player, data, notify),
            seconds * 20
        );

        taskMap.computeIfAbsent(player, task -> new HashMap<>()).put(data, taskId);
        
        LoggerUtils.debug("Установлены временные метаданные для " + player.getName() + ": " + data + " на " + seconds + " секунд");
    }

    public static void removeMeta(Player player, String data, boolean notify)
    {
        if (player == null || data == null) return;

        if (!player.hasMetadata(data)) return;

        player.removeMetadata(data, CorePlugin.getInstance());

        if (notify)
        {
            CorePlugin.getInstance().getMessageManager().sendMessage(player, "REQUEST_TIMEOUT");
            SoundUtils.playError(player);
        }

        Map<String, String> playerTasks = taskMap.get(player);

        if (playerTasks != null)
        {
            playerTasks.remove(data);

            if (playerTasks.isEmpty())
            {
                taskMap.remove(player);
            }
        }

        LoggerUtils.debug("Удалены метаданные для " + player.getName() + ": " + data);
    }

    public static void stopTempMetaTimer(Player player, String data)
    {
        if (player == null || data == null) return;

        Map<String, String> playerTasks = taskMap.get(player);

        if (playerTasks == null) return;

        String taskId = playerTasks.get(data);

        if (taskId == null) return;

        CorePlugin.getInstance().getTaskScheduler().cancelTask(taskId);

        playerTasks.remove(data);

        if (playerTasks.isEmpty())
        {
            taskMap.remove(player);
        }

        LoggerUtils.debug("Остановлен таймер метаданных для " + player.getName() + ": " + data);
    }

    public static void setMeta(Player player, String data, String value)
    {
        if (player == null || data == null) return;

        player.setMetadata(data, new FixedMetadataValue(CorePlugin.getInstance(), value));
        LoggerUtils.debug("Установлены метаданные для " + player.getName() + ": " + data + " = " + value);
    }

    public static String getMeta(Player player, String data, String defaultValue)
    {
        if (player == null || data == null) return defaultValue;

        if (player.hasMetadata(data))
        {
            return player.getMetadata(data).get(0).asString();
        }

        return defaultValue;
    }

    public static boolean hasMeta(Player player, String data)
    {
        if (player == null || data == null) return false;
        return player.hasMetadata(data);
    }

    public static void clearAllMeta(Player player)
    {
        if (player == null) return;

        Map<String, String> playerTasks = taskMap.remove(player);

        if (playerTasks != null)
        {
            for (String taskId : playerTasks.values())
            {
                CorePlugin.getInstance().getTaskScheduler().cancelTask(taskId);
            }
        }

        LoggerUtils.debug("Очищены все метаданные для " + player.getName());
    }
}