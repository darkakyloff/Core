package me.darkakyloff.core.modules;

import me.darkakyloff.core.CorePlugin;
import me.darkakyloff.core.api.database.DatabaseManager;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseModule
{
    protected final Plugin plugin;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final Set<String> dependencies;

    public BaseModule(Plugin plugin, String name)
    {
        this(plugin, name, "1.0", "darkakyloff", "Описание модуля не указано");
    }

    public BaseModule(Plugin plugin, String name, String version, String author, String description)
    {
        this.plugin = plugin;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.dependencies = new HashSet<>();

        initializeDependencies();
    }

    protected void initializeDependencies()
    {
    }

    protected void addDependency(String dependency)
    {
        dependencies.add(dependency);
    }

    public abstract void onLoad();

    public abstract void onUnload();

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }

    public String getAuthor()
    {
        return author;
    }

    public String getDescription()
    {
        return description;
    }

    public Set<String> getDependencies()
    {
        return new HashSet<>(dependencies);
    }

    public boolean hasDependency(String moduleName)
    {
        return dependencies.contains(moduleName);
    }

    public CorePlugin getCorePlugin()
    {
        return CorePlugin.getInstance();
    }

    @SuppressWarnings("unchecked")
    protected <T extends BaseModule> T getModule(String moduleName)
    {
        return (T) getCorePlugin().getModuleManager().getModule(moduleName);
    }

    protected <T extends BaseModule> T getModule(Class<T> moduleClass)
    {
        return getCorePlugin().getModuleManager().getModule(moduleClass);
    }

    protected boolean isModuleLoaded(String moduleName)
    {
        return getCorePlugin().getModuleManager().isModuleLoaded(moduleName);
    }

    public void logInfo(String message)
    {
        plugin.getLogger().info("[" + name + "] " + message);
    }

    public void logWarning(String message)
    {
        plugin.getLogger().warning("[" + name + "] " + message);
    }

    public void logError(String message)
    {
        plugin.getLogger().severe("[" + name + "] " + message);
    }

    public void logError(String message, Throwable throwable)
    {
        plugin.getLogger().severe("[" + name + "] " + message);
        throwable.printStackTrace();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        BaseModule that = (BaseModule) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
}