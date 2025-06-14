package me.darkakyloff.core.api.message;

public enum MessageKeys implements MessageKey
{
    NO_PERMISSION("NO_PERMISSION"),
    ONLY_PLAYER("ONLY_PLAYER"),
    PLAYER_OFFLINE("PLAYER_OFFLINE"),
    SELF_USAGE("SELF_USAGE"),
    ERROR_COMMAND_USAGE("ERROR_COMMAND_USAGE"),
    ERROR_UNKNOWN("ERROR_UNKNOWN"),
    REQUEST_TIMEOUT("REQUEST_TIMEOUT");

    private final String key;

    MessageKeys(String key)
    {
        this.key = key;
    }

    public String getKey()
    {
        return key;
    }

    @Override
    public String toString()
    {
        return key;
    }

    public static MessageKeys fromString(String key)
    {
        for (MessageKeys messageKey : values())
        {
            if (messageKey.getKey().equals(key))
            {
                return messageKey;
            }
        }
        return null;
    }

    public static boolean exists(String key)
    {
        return fromString(key) != null;
    }
}