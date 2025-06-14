package me.darkakyloff.core.managers;

import me.darkakyloff.core.modules.BaseModule;

import java.util.Set;

public class ModuleInfo
{
    private final BaseModule module;
    private ModuleState state;
    private long loadTime;
    private long registerTime;
    private String lastError;

    public ModuleInfo(BaseModule module)
    {
        this.module = module;
        this.state = ModuleState.REGISTERED;
        this.registerTime = System.currentTimeMillis();
        this.loadTime = 0;
        this.lastError = null;
    }

    public BaseModule getModule()
    {
        return module;
    }

    public String getName()
    {
        return module.getName();
    }

    public String getVersion()
    {
        return module.getVersion();
    }

    public String getAuthor()
    {
        return module.getAuthor();
    }

    public String getDescription()
    {
        return module.getDescription();
    }


    public Set<String> getDependencies()
    {
        return module.getDependencies();
    }

    public ModuleState getState()
    {
        return state;
    }

    public void setState(ModuleState state)
    {
        this.state = state;
    }

    public long getRegisterTime()
    {
        return registerTime;
    }

    public long getLoadTime()
    {
        return loadTime;
    }

    public void setLoadTime(long loadTime)
    {
        this.loadTime = loadTime;
    }

    public String getLastError()
    {
        return lastError;
    }

    public void setLastError(String lastError)
    {
        this.lastError = lastError;
    }

    public long getUptime()
    {
        if (state == ModuleState.LOADED && loadTime > 0)
        {
            return System.currentTimeMillis() - loadTime;
        }
        return 0;
    }

    public boolean isLoaded()
    {
        return state == ModuleState.LOADED;
    }

    public boolean hasError()
    {
        return state == ModuleState.FAILED || lastError != null;
    }
}