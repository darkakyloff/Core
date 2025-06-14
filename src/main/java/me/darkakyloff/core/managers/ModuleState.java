package me.darkakyloff.core.managers;


public enum ModuleState
{
    REGISTERED("Зарегистрирован"),

    LOADING("Загружается"),

    LOADED("Загружен"),

    UNLOADING("Выгружается"),

    UNLOADED("Выгружен"),

    FAILED("Ошибка");

    private final String displayName;

    ModuleState(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public boolean isActive()
    {
        return this == LOADED;
    }

    public boolean isTransitional()
    {
        return this == LOADING || this == UNLOADING;
    }

    public boolean isFinal()
    {
        return this == UNLOADED || this == FAILED;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}