package me.darkakyloff.core.api.telegram;

import com.pengrad.telegrambot.model.Update;

public interface TelegramEventHandler
{
    void handle(Update update) throws Exception;
}