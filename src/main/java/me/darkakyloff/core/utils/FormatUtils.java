    package me.darkakyloff.core.utils;

public class FormatUtils
{
    public static String formatTime(int seconds)
    {
        if (seconds < 60) 
        {
            return seconds + " " + getSecondsForm(seconds);
        }
        else if (seconds < 3600)
        {
            int minutes = seconds / 60;
            return minutes + " " + getMinutesForm(minutes);
        }
        else
        {
            int hours = seconds / 3600;
            return hours + " " + getHoursForm(hours);
        }
    }

    private static String getSecondsForm(int number)
    {
        int lastTwoDigits = number % 100;
        int lastDigit = number % 10;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) return "секунд";
        else if (lastDigit == 1) return "секунда";
        else if (lastDigit >= 2 && lastDigit <= 4) return "секунды";
        else return "секунд";
    }

    private static String getMinutesForm(int number)
    {
        int lastTwoDigits = number % 100;
        int lastDigit = number % 10;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) return "минут";
        else if (lastDigit == 1) return "минута";
        else if (lastDigit >= 2 && lastDigit <= 4) return "минуты";
        else return "минут";
    }

    private static String getHoursForm(int number)
    {
        int lastTwoDigits = number % 100;
        int lastDigit = number % 10;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) return "часов";
        else if (lastDigit == 1) return "час";
        else if (lastDigit >= 2 && lastDigit <= 4) return "часа";
        else return "часов";
    }
}