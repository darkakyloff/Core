package me.darkakyloff.core.utils;

import me.darkakyloff.core.CorePlugin;
import me.darkakyloff.core.api.config.ConfigurationManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoundUtils
{
    private static final Map<String, Sound> soundCache = new ConcurrentHashMap<>();

    private static boolean soundsEnabled = true;
    private static float defaultVolume = 1.0f;
    private static float defaultPitch = 1.0f;

    private static Sound successSound = Sound.BLOCK_NOTE_BLOCK_BELL;
    private static Sound errorSound = Sound.ENTITY_VILLAGER_NO;
    private static Sound infoSound = Sound.BLOCK_NOTE_BLOCK_COW_BELL;
    private static Sound warningSound = Sound.BLOCK_NOTE_BLOCK_BASS;
    private static Sound clickSound = Sound.BLOCK_STONE_BUTTON_CLICK_ON;
    private static Sound openSound = Sound.BLOCK_CHEST_OPEN;
    private static Sound closeSound = Sound.BLOCK_CHEST_CLOSE;

    public static void initialize()
    {
        loadSoundSettings();
        LoggerUtils.debug("SoundUtils инициализированы");
    }

    private static void loadSoundSettings()
    {
        try
        {
            ConfigurationManager configManager = CorePlugin.getInstance().getConfigurationManager();

            soundsEnabled = configManager.getBoolean("settings.yml", "sounds.enabled", true);

            String successSoundName = configManager.getString("settings.yml", "sounds.types.success", "BLOCK_NOTE_BLOCK_BELL");
            Sound sound = getSound(successSoundName);
            if (sound != null) successSound = sound;

            String errorSoundName = configManager.getString("settings.yml", "sounds.types.error", "ENTITY_VILLAGER_NO");
            sound = getSound(errorSoundName);
            if (sound != null) errorSound = sound;

            String infoSoundName = configManager.getString("settings.yml", "sounds.types.info", "BLOCK_NOTE_BLOCK_COW_BELL");
            sound = getSound(infoSoundName);
            if (sound != null) infoSound = sound;

            String warningSoundName = configManager.getString("settings.yml", "sounds.types.warning", "BLOCK_NOTE_BLOCK_BASS");
            sound = getSound(warningSoundName);
            if (sound != null) warningSound = sound;

            String clickSoundName = configManager.getString("settings.yml", "sounds.types.click", "BLOCK_STONE_BUTTON_CLICK_ON");
            sound = getSound(clickSoundName);
            if (sound != null) clickSound = sound;

            String openSoundName = configManager.getString("settings.yml", "sounds.types.open", "BLOCK_CHEST_OPEN");
            sound = getSound(openSoundName);
            if (sound != null) openSound = sound;

            String closeSoundName = configManager.getString("settings.yml", "sounds.types.close", "BLOCK_CHEST_CLOSE");
            sound = getSound(closeSoundName);
            if (sound != null) closeSound = sound;

            LoggerUtils.debug("Настройки звуков загружены из конфигурации");
        }
        catch (Exception exception)
        {
            LoggerUtils.warning("Ошибка загрузки настроек звуков, используются дефолтные");
        }
    }

    public static void play(CommandSender sender, Sound sound)
    {
        play(sender, sound, defaultVolume, defaultPitch);
    }

    public static void play(CommandSender sender, Sound sound, float volume, float pitch)
    {
        if (!soundsEnabled || sound == null)
        {
            return;
        }

        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public static void play(Location location, Sound sound)
    {
        play(location, sound, defaultVolume, defaultPitch);
    }

    public static void play(Location location, Sound sound, float volume, float pitch)
    {
        if (!soundsEnabled || sound == null || location == null || location.getWorld() == null)
        {
            return;
        }

        location.getWorld().playSound(location, sound, volume, pitch);
    }

    public static void broadcast(Sound sound)
    {
        broadcast(sound, defaultVolume, defaultPitch);
    }

    public static void broadcast(Sound sound, float volume, float pitch)
    {
        if (!soundsEnabled || sound == null) return;

        for (Player player : Bukkit.getOnlinePlayers())
        {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    public static void playSuccess(CommandSender sender)
    {
        play(sender, successSound);
    }

    public static void playError(CommandSender sender)
    {
        play(sender, errorSound);
    }

    public static void playInfo(CommandSender sender)
    {
        play(sender, infoSound);
    }

    public static void playWarning(CommandSender sender)
    {
        play(sender, warningSound);
    }

    public static void playClick(CommandSender sender)
    {
        play(sender, clickSound);
    }

    public static void playOpen(CommandSender sender)
    {
        play(sender, openSound);
    }

    public static void playClose(CommandSender sender)
    {
        play(sender, closeSound);
    }

    public static Sound getSound(String soundName)
    {
        if (soundName == null || soundName.isEmpty()) return null;

        Sound cachedSound = soundCache.get(soundName.toUpperCase());

        if (cachedSound != null) return cachedSound;

        try
        {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            soundCache.put(soundName.toUpperCase(), sound);
            return sound;
        }
        catch (IllegalArgumentException exception)
        {
            LoggerUtils.warning("Звук не найден: " + soundName);
            return null;
        }
    }

    public static boolean soundExists(String soundName)
    {
        return getSound(soundName) != null;
    }

    public static void setSoundsEnabled(boolean enabled)
    {
        soundsEnabled = enabled;
        LoggerUtils.debug("Звуки " + (enabled ? "включены" : "отключены"));
    }

    public static boolean areSoundsEnabled()
    {
        return soundsEnabled;
    }

    public static void setDefaultVolume(float volume)
    {
        defaultVolume = Math.max(0.0f, Math.min(1.0f, volume));
        LoggerUtils.debug("Громкость по умолчанию установлена: " + defaultVolume);
    }

    public static float getDefaultVolume()
    {
        return defaultVolume;
    }

    public static void setDefaultPitch(float pitch)
    {
        defaultPitch = Math.max(0.5f, Math.min(2.0f, pitch));
        LoggerUtils.debug("Высота звука по умолчанию установлена: " + defaultPitch);
    }

    public static float getDefaultPitch()
    {
        return defaultPitch;
    }

    public static void clearCache()
    {
        soundCache.clear();
        LoggerUtils.debug("Кеш звуков очищен");
    }
}