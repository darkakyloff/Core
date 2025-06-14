package me.darkakyloff.core.api.message;

import me.darkakyloff.core.api.config.ConfigurationManager;
import me.darkakyloff.core.utils.LoggerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageManager
{
    private final ConfigurationManager configManager;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    private final Map<String, Component> messageCache;
    private final Map<String, List<Component>> messageListCache;

    private String localizationFile;
    private boolean cacheEnabled;
    private boolean debugMode;

    public MessageManager(ConfigurationManager configManager)
    {
        this.configManager = configManager;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacySection();

        this.messageCache = new ConcurrentHashMap<>();
        this.messageListCache = new ConcurrentHashMap<>();

        this.localizationFile = "localization.yml";
        this.cacheEnabled = true;
        this.debugMode = false;

        LoggerUtils.debug("MessageManager инициализирован");
    }

    public void initialize()
    {
        loadSettings();
        clearCache();

        LoggerUtils.debug("Система сообщений инициализирована");
    }

    private void loadSettings()
    {
        this.localizationFile = configManager.getString("settings.yml", "localization.file", "localization.yml");
        this.cacheEnabled = configManager.getBoolean("settings.yml", "localization.cache-enabled", true);
        this.debugMode = configManager.getBoolean("settings.yml", "localization.debug-mode", false);

        LoggerUtils.debug("Настройки локализации загружены: файл=" + localizationFile +
                ", кеш=" + cacheEnabled + ", отладка=" + debugMode);
    }

    public void sendMessage(CommandSender recipient, String messageKey, Object... placeholders)
    {
        Component message = getMessage(messageKey, placeholders);

        if (message == null)
        {
            LoggerUtils.warning("Сообщение не найдено: " + messageKey);
            return;
        }

        if (recipient instanceof Player)
        {
            Player player = (Player) recipient;
            player.sendMessage(message);
        }
        else
        {
            String legacyMessage = legacySerializer.serialize(message);
            recipient.sendMessage(legacyMessage);
        }

        if (debugMode)
        {
            LoggerUtils.debug("Сообщение отправлено " + recipient.getName() + ": " + messageKey);
        }
    }

    public void sendMessageList(CommandSender recipient, String messageKey, Object... placeholders)
    {
        List<Component> messages = getMessageList(messageKey, placeholders);

        if (messages.isEmpty())
        {
            LoggerUtils.warning("Список сообщений не найден: " + messageKey);
            return;
        }

        for (Component message : messages)
        {
            if (recipient instanceof Player)
            {
                Player player = (Player) recipient;
                player.sendMessage(message);
            }
            else
            {
                String legacyMessage = legacySerializer.serialize(message);
                recipient.sendMessage(legacyMessage);
            }
        }

        if (debugMode)
        {
            LoggerUtils.debug("Список сообщений отправлен " + recipient.getName() + ": " + messageKey);
        }
    }

    public Component getMessage(String messageKey, Object... placeholders)
    {
        String cacheKey = createCacheKey(messageKey, placeholders);

        if (cacheEnabled && messageCache.containsKey(cacheKey))
        {
            return messageCache.get(cacheKey);
        }

        String rawMessage = configManager.getString(localizationFile, messageKey);

        if (rawMessage == null)
        {
            LoggerUtils.warning("Сообщение не найдено в конфигурации: " + messageKey);
            return Component.text("Сообщение не найдено: " + messageKey);
        }

        TagResolver resolver = createTagResolver(placeholders);

        String processedMessage = convertLegacyToMiniMessage(rawMessage);

        Component component = miniMessage.deserialize(processedMessage, resolver);

        if (cacheEnabled)
        {
            messageCache.put(cacheKey, component);
        }

        return component;
    }

    public List<Component> getMessageList(String messageKey, Object... placeholders)
    {
        String cacheKey = createCacheKey(messageKey, placeholders);

        if (cacheEnabled && messageListCache.containsKey(cacheKey))
        {
            return messageListCache.get(cacheKey);
        }

        List<String> rawMessages = configManager.getStringList(localizationFile, messageKey);

        if (rawMessages.isEmpty())
        {
            LoggerUtils.warning("Список сообщений не найден в конфигурации: " + messageKey);
            return List.of();
        }

        List<Component> components = new ArrayList<>();
        TagResolver resolver = createTagResolver(placeholders);

        for (String rawMessage : rawMessages)
        {
            String processedMessage = convertLegacyToMiniMessage(rawMessage);
            Component component = miniMessage.deserialize(processedMessage, resolver);
            components.add(component);
        }

        if (cacheEnabled)
        {
            messageListCache.put(cacheKey, components);
        }

        return components;
    }

    public String getMessageAsString(String messageKey, Object... placeholders)
    {
        Component component = getMessage(messageKey, placeholders);
        return component != null ? legacySerializer.serialize(component) : "Сообщение не найдено: " + messageKey + "";
    }

    public boolean hasMessage(String messageKey)
    {
        return configManager.contains(localizationFile, messageKey);
    }

    public void setLocalizationFile(String fileName)
    {
        this.localizationFile = fileName;
        clearCache();
        LoggerUtils.debug("Файл локализации изменен на: " + fileName);
    }

    public void setCacheEnabled(boolean enabled)
    {
        this.cacheEnabled = enabled;
        if (!enabled)
        {
            clearCache();
        }
        LoggerUtils.debug("Кеширование сообщений " + (enabled ? "включено" : "отключено"));
    }

    public void setDebugMode(boolean enabled)
    {
        this.debugMode = enabled;
        LoggerUtils.debug("Режим отладки сообщений " + (enabled ? "включен" : "отключен"));
    }

    public void clearCache()
    {
        messageCache.clear();
        messageListCache.clear();
        LoggerUtils.debug("Кеш сообщений очищен");
    }

    public void reload()
    {
        configManager.reloadConfig(localizationFile);
        clearCache();
        loadSettings();
        LoggerUtils.debug("Система сообщений перезагружена");
    }

    private TagResolver createTagResolver(Object... placeholders)
    {
        if (placeholders.length == 0)
        {
            return TagResolver.empty();
        }

        TagResolver.Builder builder = TagResolver.builder();

        for (int i = 0; i < placeholders.length; i += 2)
        {
            if (i + 1 < placeholders.length)
            {
                String key = String.valueOf(placeholders[i]);
                String value = String.valueOf(placeholders[i + 1]);

                builder.resolver(Placeholder.parsed(key, value));
            }
        }

        return builder.build();
    }

    private String convertLegacyToMiniMessage(String message)
    {
        return message
                .replaceAll("&0", "<black>")
                .replaceAll("&1", "<dark_blue>")
                .replaceAll("&2", "<dark_green>")
                .replaceAll("&3", "<dark_aqua>")
                .replaceAll("&4", "<dark_red>")
                .replaceAll("&5", "<dark_purple>")
                .replaceAll("&6", "<gold>")
                .replaceAll("&7", "<gray>")
                .replaceAll("&8", "<dark_gray>")
                .replaceAll("&9", "<blue>")
                .replaceAll("&a", "<green>")
                .replaceAll("&b", "<aqua>")
                .replaceAll("&c", "<red>")
                .replaceAll("&d", "<light_purple>")
                .replaceAll("&e", "<yellow>")
                .replaceAll("&f", "<white>")
                .replaceAll("&k", "<obfuscated>")
                .replaceAll("&l", "<bold>")
                .replaceAll("&m", "<strikethrough>")
                .replaceAll("&n", "<underlined>")
                .replaceAll("&o", "<italic>")
                .replaceAll("&r", "<reset>");
    }

    private String createCacheKey(String messageKey, Object... placeholders)
    {
        if (placeholders.length == 0)
        {
            return messageKey;
        }

        StringBuilder builder = new StringBuilder(messageKey);
        builder.append("|");

        for (Object placeholder : placeholders)
        {
            builder.append(placeholder).append("|");
        }

        return builder.toString();
    }
}