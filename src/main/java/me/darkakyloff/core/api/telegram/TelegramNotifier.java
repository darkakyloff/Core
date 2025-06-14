package me.darkakyloff.core.api.telegram;

import me.darkakyloff.core.CorePlugin;
import me.darkakyloff.core.utils.LoggerUtils;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TelegramNotifier
{
    private final long chatId;
    private final TelegramService telegramService;

    public TelegramNotifier(long chatId)
    {
        this.chatId = chatId;
        this.telegramService = CorePlugin.getInstance().getTelegramService();

        if (telegramService == null)
        {
            LoggerUtils.warning("TelegramService не инициализирован");
        }
    }

    public CompletableFuture<Integer> sendNotification(String message)
    {
        if (telegramService == null)
        {
            return CompletableFuture.completedFuture(null);
        }

        return telegramService.sendMessage(chatId, message);
    }

    public CompletableFuture<Integer> sendNotification(String message, Object... replacements)
    {
        if (telegramService == null)
        {
            return CompletableFuture.completedFuture(null);
        }

        return telegramService.sendMessage(chatId, message, replacements);
    }

    public CompletableFuture<Integer> sendToThread(int threadId, String message)
    {
        if (telegramService == null)
        {
            return CompletableFuture.completedFuture(null);
        }

        return telegramService.sendMessageToThread(chatId, threadId, message);
    }

    public CompletableFuture<Integer> sendToThread(int threadId, String message, Object... replacements)
    {
        if (telegramService == null)
        {
            return CompletableFuture.completedFuture(null);
        }

        return telegramService.sendMessageToThread(chatId, threadId, message, replacements);
    }

    public CompletableFuture<Integer> sendSuccess(String message)
    {
        return sendNotification("✅ " + message);
    }

    public CompletableFuture<Integer> sendError(String message)
    {
        return sendNotification("❌ " + message);
    }

    public CompletableFuture<Integer> sendWarning(String message)
    {
        return sendNotification("⚠️ " + message);
    }

    public CompletableFuture<Integer> sendInfo(String message)
    {
        return sendNotification("ℹ️ " + message);
    }

    public CompletableFuture<Integer> sendCommandNotification(String playerName, String command, boolean success)
    {
        String icon = success ? "✅" : "❌";
        String status = success ? "выполнена" : "отклонена";

        String message = String.format("%s <b>%s</b> - команда <code>%s</code> %s",
                icon, playerName, command, status);

        return sendNotification(message);
    }

    public CompletableFuture<Integer> sendPlayerJoinLeave(String playerName, boolean joined)
    {
        String icon = joined ? "🟢" : "🔴";
        String action = joined ? "подключился" : "отключился";

        String message = String.format("%s Игрок <b>%s</b> %s", icon, playerName, action);

        return sendNotification(message);
    }

    public CompletableFuture<Integer> sendAdminAction(String adminName, String action, String target)
    {
        String message = String.format("🛡️ Администратор <b>%s</b> выполнил действие: <i>%s</i>",
                adminName, action);

        if (target != null && !target.isEmpty())
        {
            message += String.format(" для <b>%s</b>", target);
        }

        return sendNotification(message);
    }

    public CompletableFuture<Integer> sendWithButton(String message, String buttonText, String callbackData)
    {
        if (telegramService == null)
        {
            return CompletableFuture.completedFuture(null);
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton(buttonText).callbackData(callbackData)
        );

        return telegramService.sendMessage(chatId, message, keyboard);
    }

    public CompletableFuture<Integer> sendWithButtons(String message, String... buttons)
    {
        if (telegramService == null || buttons.length % 2 != 0)
        {
            return CompletableFuture.completedFuture(null);
        }

        InlineKeyboardButton[] keyboardButtons = new InlineKeyboardButton[buttons.length / 2];

        for (int i = 0; i < buttons.length; i += 2)
        {
            String text = buttons[i];
            String callbackData = buttons[i + 1];
            keyboardButtons[i / 2] = new InlineKeyboardButton(text).callbackData(callbackData);
        }

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(keyboardButtons);

        return telegramService.sendMessage(chatId, message, keyboard);
    }

    public CompletableFuture<Integer> sendStats(String title, String... stats)
    {
        StringBuilder message = new StringBuilder();
        message.append("📊 <b>").append(title).append("</b>\n\n");

        for (int i = 0; i < stats.length; i += 2)
        {
            if (i + 1 < stats.length)
            {
                String key = stats[i];
                String value = stats[i + 1];
                message.append("▫️ <i>").append(key).append(":</i> <code>").append(value).append("</code>\n");
            }
        }

        return sendNotification(message.toString());
    }

    public CompletableFuture<Integer> sendSystemNotification(String level, String system, String message)
    {
        String icon = getIconForLevel(level);
        String formattedMessage = String.format("%s <b>[%s]</b> %s: %s",
                icon, level, system, message);

        return sendNotification(formattedMessage);
    }

    public CompletableFuture<Integer> sendServerEvent(String event, String details)
    {
        String icon = getIconForEvent(event);
        String message = String.format("%s <b>Сервер %s</b>", icon, event.toLowerCase());

        if (details != null && !details.isEmpty())
        {
            message += "\n" + details;
        }

        return sendNotification(message);
    }

    public CompletableFuture<Integer> sendErrorReport(String errorType, String errorMessage, String location)
    {
        StringBuilder message = new StringBuilder();
        message.append("🚨 <b>Критическая ошибка!</b>\n\n");
        message.append("🔸 <b>Тип:</b> <code>").append(errorType).append("</code>\n");
        message.append("🔸 <b>Место:</b> <code>").append(location).append("</code>\n");
        message.append("🔸 <b>Сообщение:</b>\n<pre>").append(errorMessage).append("</pre>");

        return sendNotification(message.toString());
    }

    public CompletableFuture<Boolean> editMessage(int messageId, String newMessage)
    {
        if (telegramService == null)
        {
            return CompletableFuture.completedFuture(false);
        }

        return telegramService.editMessage(chatId, messageId, newMessage);
    }

    public CompletableFuture<Boolean> editMessage(int messageId, String newMessage, Object... replacements)
    {
        if (telegramService == null)
        {
            return CompletableFuture.completedFuture(false);
        }

        String processedMessage = replaceInternalPlaceholders(newMessage, replacements);
        return telegramService.editMessage(chatId, messageId, processedMessage);
    }

    public boolean isAvailable()
    {
        return telegramService != null && telegramService.isRunning();
    }

    public long getChatId()
    {
        return chatId;
    }

    public long getThreads(String thread)
    {
        return new CorePlugin().getConfigurationManager().getLong("settings.yml", "telegram.threads." + thread, 0);
    }

    public TelegramNotifier forChat(long newChatId)
    {
        return new TelegramNotifier(newChatId);
    }

    private String getIconForLevel(String level)
    {
        switch (level.toUpperCase())
        {
            case "INFO": return "ℹ️";
            case "SUCCESS": return "✅";
            case "WARN": case "WARNING": return "⚠️";
            case "ERROR": case "SEVERE": return "❌";
            case "DEBUG": return "🐛";
            default: return "📝";
        }
    }

    private String getIconForEvent(String event)
    {
        switch (event.toUpperCase())
        {
            case "START": case "STARTUP": return "🟢";
            case "STOP": case "SHUTDOWN": return "🔴";
            case "RELOAD": case "RESTART": return "🔄";
            case "CRASH": return "💥";
            default: return "🔧";
        }
    }

    private String replaceInternalPlaceholders(String message, Object... replacements)
    {
        if (message == null || replacements == null || replacements.length == 0)
        {
            return message;
        }

        if (replacements.length % 2 != 0)
        {
            LoggerUtils.warning("Неправильное количество аргументов для замены плейсхолдеров");
            return message;
        }

        String result = message;

        for (int i = 0; i < replacements.length; i += 2)
        {
            String placeholder = "{" + replacements[i] + "}";
            String value = replacements[i + 1] != null ? replacements[i + 1].toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }
}