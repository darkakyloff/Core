package me.darkakyloff.core.api.telegram;

import me.darkakyloff.core.utils.LoggerUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class TelegramService
{
    private final String botToken;
    private TelegramBot bot;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    private final Map<String, TelegramEventHandler> eventHandlers = new ConcurrentHashMap<>();

    private boolean debugMode = false;

    public TelegramService(String botToken)
    {
        this.botToken = botToken;
        LoggerUtils.debug("TelegramService создан");
    }

    public boolean initialize()
    {
        if (isRunning.get())
        {
            LoggerUtils.warning("Telegram сервис уже запущен");
            return true;
        }
        
        if (botToken == null || botToken.trim().isEmpty())
        {
            LoggerUtils.error("Токен Telegram бота не указан");
            return false;
        }
        
        try
        {
            LoggerUtils.debug("Инициализация Telegram сервиса...");

            bot = new TelegramBot(botToken);

            setupUpdatesListener();

            if (testConnection())
            {
                isRunning.set(true);
                LoggerUtils.debug("Telegram сервис инициализирован");
                return true;
            }
            else
            {
                LoggerUtils.error("Не удалось подключиться к Telegram API");
                return false;
            }
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка инициализации Telegram сервиса", exception);
            return false;
        }
    }

    private void setupUpdatesListener()
    {
        bot.setUpdatesListener(updates -> 
        {
            if (isShuttingDown.get())
            {
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }
            
            try
            {
                for (Update update : updates)
                {
                    handleUpdate(update);
                }
            }
            catch (Exception exception)
            {
                LoggerUtils.error("Ошибка обработки Telegram обновлений", exception);
            }
            
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
        
        LoggerUtils.debug("Слушатель Telegram обновлений настроен");
    }

    private void handleUpdate(Update update)
    {
        if (debugMode)
        {
            LoggerUtils.debug("Получено Telegram обновление: " + update.updateId());
        }
        
        try
        {
            if (update.message() != null)
            {
                TelegramEventHandler handler = eventHandlers.get("message");
                if (handler != null)
                {
                    handler.handle(update);
                }
            }

            if (update.callbackQuery() != null)
            {
                TelegramEventHandler handler = eventHandlers.get("callback");
                if (handler != null)
                {
                    handler.handle(update);
                }
            }
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка обработки Telegram обновления", exception);
        }
    }
    private boolean testConnection()
    {
        try
        {
            var getMe = bot.execute(new com.pengrad.telegrambot.request.GetMe());
            return getMe.isOk();
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка тестирования Telegram соединения", exception);
            return false;
        }
    }

    public CompletableFuture<Integer> sendMessage(long chatId, String message)
    {
        return sendMessage(chatId, message, null, null);
    }

    public CompletableFuture<Integer> sendMessage(long chatId, String message, Object... replacements)
    {
        String processedMessage = replacePlaceholders(message, replacements);
        return sendMessage(chatId, processedMessage, null, null);
    }

    public CompletableFuture<Integer> sendMessage(long chatId, String message, InlineKeyboardMarkup keyboard)
    {
        return sendMessage(chatId, message, keyboard, null);
    }

    public CompletableFuture<Integer> sendMessageToThread(long chatId, int threadId, String message)
    {
        return sendMessage(chatId, message, null, threadId);
    }

    public CompletableFuture<Integer> sendMessageToThread(long chatId, int threadId, String message, Object... replacements)
    {
        String processedMessage = replacePlaceholders(message, replacements);
        return sendMessage(chatId, processedMessage, null, threadId);
    }

    private CompletableFuture<Integer> sendMessage(long chatId, String message, InlineKeyboardMarkup keyboard, Integer threadId)
    {
        if (!isRunning.get())
        {
            LoggerUtils.warning("Telegram сервис не запущен");
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> 
        {
            try
            {
                SendMessage request = new SendMessage(chatId, message)
                        .parseMode(ParseMode.HTML)
                        .disableWebPagePreview(true);
                
                if (keyboard != null)
                {
                    request.replyMarkup(keyboard);
                }
                
                if (threadId != null)
                {
                    request.messageThreadId(threadId);
                }
                
                if (debugMode)
                {
                    LoggerUtils.debug("Отправка Telegram сообщения в чат " + chatId + 
                                    (threadId != null ? " поток " + threadId : ""));
                }
                
                SendResponse response = bot.execute(request);
                
                if (response.isOk())
                {
                    int messageId = response.message().messageId();
                    
                    if (debugMode)
                    {
                        LoggerUtils.debug("Telegram сообщение отправлено: ID " + messageId);
                    }
                    
                    return messageId;
                }
                else
                {
                    LoggerUtils.error("Ошибка отправки Telegram сообщения: " + response.description());
                    return null;
                }
            }
            catch (Exception exception)
            {
                LoggerUtils.error("Исключение при отправке Telegram сообщения", exception);
                return null;
            }
        });
    }

    public CompletableFuture<Boolean> editMessage(long chatId, int messageId, String newMessage)
    {
        return editMessage(chatId, messageId, newMessage, null);
    }

    public CompletableFuture<Boolean> editMessage(long chatId, int messageId, String newMessage, InlineKeyboardMarkup keyboard)
    {
        if (!isRunning.get())
        {
            LoggerUtils.warning("Telegram сервис не запущен");
            return CompletableFuture.completedFuture(false);
        }
        
        return CompletableFuture.supplyAsync(() -> 
        {
            try
            {
                EditMessageText request = new EditMessageText(chatId, messageId, newMessage)
                        .parseMode(ParseMode.HTML)
                        .disableWebPagePreview(true);
                
                if (keyboard != null)
                {
                    request.replyMarkup(keyboard);
                }
                
                if (debugMode)
                {
                    LoggerUtils.debug("Редактирование Telegram сообщения " + messageId + " в чате " + chatId);
                }
                
                BaseResponse response = bot.execute(request);
                
                if (response.isOk())
                {
                    if (debugMode)
                    {
                        LoggerUtils.debug("Telegram сообщение отредактировано: ID " + messageId);
                    }
                    
                    return true;
                }
                else
                {
                    LoggerUtils.error("Ошибка редактирования Telegram сообщения: " + response.description());
                    return false;
                }
            }
            catch (Exception exception)
            {
                LoggerUtils.error("Исключение при редактировании Telegram сообщения", exception);
                return false;
            }
        });
    }

    public void registerEventHandler(String eventType, TelegramEventHandler handler)
    {
        eventHandlers.put(eventType, handler);
        LoggerUtils.debug("Зарегистрирован обработчик Telegram событий: " + eventType);
    }

    public void unregisterEventHandler(String eventType)
    {
        eventHandlers.remove(eventType);
        LoggerUtils.debug("Удален обработчик Telegram событий: " + eventType);
    }

    private String replacePlaceholders(String message, Object... replacements)
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

    public boolean isRunning()
    {
        return isRunning.get();
    }

    public TelegramBot getBot()
    {
        return bot;
    }

    public void shutdown()
    {
        if (!isRunning.get())
        {
            LoggerUtils.warning("Telegram сервис не запущен");
            return;
        }
        
        LoggerUtils.debug("Остановка Telegram сервиса...");
        
        try
        {
            isShuttingDown.set(true);

            if (bot != null)
            {
                bot.setUpdatesListener(null);

                Thread.sleep(100);
            }

            eventHandlers.clear();
            
            isRunning.set(false);
            isShuttingDown.set(false);
            
            LoggerUtils.debug("Telegram сервис остановлен");
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка остановки Telegram сервиса", exception);
        }
        finally
        {
            bot = null;
        }
    }
}