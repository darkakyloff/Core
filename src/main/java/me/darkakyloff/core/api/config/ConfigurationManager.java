package me.darkakyloff.core.api.config;

import me.darkakyloff.core.utils.LoggerUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationManager
{
    private final Plugin plugin;
    private final Map<String, FileConfiguration> configCache;
    private final Map<String, File> fileCache;

    public ConfigurationManager(Plugin plugin)
    {
        this.plugin = plugin;
        this.configCache = new ConcurrentHashMap<>();
        this.fileCache = new ConcurrentHashMap<>();

        LoggerUtils.debug("ConfigurationManager инициализирован");
    }

    public boolean loadConfig(String fileName)
    {
        return loadConfig(fileName, null);
    }

    public boolean loadConfig(String fileName, String folderName)
    {
        try
        {
            File configFile = getConfigFile(fileName, folderName);

            if (!configFile.exists())
            {
                createConfigFromResource(fileName, folderName);
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            String cacheKey = createCacheKey(fileName, folderName);
            configCache.put(cacheKey, config);
            fileCache.put(cacheKey, configFile);

            LoggerUtils.debug("Конфигурация загружена: " + fileName);
            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка загрузки конфигурации: " + fileName, exception);
            return false;
        }
    }

    public boolean reloadConfig(String fileName)
    {
        return reloadConfig(fileName, null);
    }

    public boolean reloadConfig(String fileName, String folderName)
    {
        String cacheKey = createCacheKey(fileName, folderName);

        configCache.remove(cacheKey);
        fileCache.remove(cacheKey);

        boolean result = loadConfig(fileName, folderName);

        if (result)
        {
            LoggerUtils.debug("Конфигурация перезагружена: " + fileName);
        }

        return result;
    }

    public boolean saveConfig(String fileName)
    {
        return saveConfig(fileName, null);
    }

    public boolean saveConfig(String fileName, String folderName)
    {
        try
        {
            String cacheKey = createCacheKey(fileName, folderName);
            FileConfiguration config = configCache.get(cacheKey);
            File configFile = fileCache.get(cacheKey);

            if (config == null || configFile == null)
            {
                LoggerUtils.warning("Конфигурация не найдена для сохранения: " + fileName);
                return false;
            }

            config.save(configFile);
            LoggerUtils.debug("Конфигурация сохранена: " + fileName);
            return true;
        }
        catch (IOException exception)
        {
            LoggerUtils.error("Ошибка сохранения конфигурации: " + fileName, exception);
            return false;
        }
    }

    public void unloadConfig(String fileName)
    {
        unloadConfig(fileName, null);
    }

    public void unloadConfig(String fileName, String folderName)
    {
        String cacheKey = createCacheKey(fileName, folderName);
        configCache.remove(cacheKey);
        fileCache.remove(cacheKey);

        LoggerUtils.debug("Конфигурация выгружена: " + fileName);
    }

    public String getString(String fileName, String path)
    {
        return getString(fileName, path, null);
    }

    public String getString(String fileName, String path, String defaultValue)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null ? config.getString(path, defaultValue) : defaultValue;
    }

    public int getInt(String fileName, String path, int defaultValue)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null ? config.getInt(path, defaultValue) : defaultValue;
    }

    public long getLong(String fileName, String path, long defaultValue)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null ? config.getLong(path, defaultValue) : defaultValue;
    }

    public double getDouble(String fileName, String path, double defaultValue)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null ? config.getDouble(path, defaultValue) : defaultValue;
    }

    public boolean getBoolean(String fileName, String path, boolean defaultValue)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null ? config.getBoolean(path, defaultValue) : defaultValue;
    }

    public List<String> getStringList(String fileName, String path)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null ? config.getStringList(path) : List.of();
    }

    public ConfigurationSection getConfigurationSection(String fileName, String path)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null ? config.getConfigurationSection(path) : null;
    }

    public void set(String fileName, String path, Object value)
    {
        FileConfiguration config = getConfig(fileName);
        if (config != null)
        {
            config.set(path, value);
            LoggerUtils.debug("Значение установлено в конфигурации " + fileName + ": " + path + " = " + value);
        }
    }

    public boolean contains(String fileName, String path)
    {
        FileConfiguration config = getConfig(fileName);
        return config != null && config.contains(path);
    }

    public boolean isConfigLoaded(String fileName)
    {
        return isConfigLoaded(fileName, null);
    }

    public boolean isConfigLoaded(String fileName, String folderName)
    {
        String cacheKey = createCacheKey(fileName, folderName);
        return configCache.containsKey(cacheKey);
    }

    public Map<String, FileConfiguration> getAllConfigs()
    {
        return Map.copyOf(configCache);
    }

    public void unloadAllConfigs()
    {
        configCache.clear();
        fileCache.clear();
        LoggerUtils.debug("Все конфигурации выгружены");
    }

    private FileConfiguration getConfig(String fileName)
    {
        String cacheKey = createCacheKey(fileName, null);
        return configCache.get(cacheKey);
    }

    private void createConfigFromResource(String fileName, String folderName)
    {
        try
        {
            File configFile = getConfigFile(fileName, folderName);

            File parentDir = configFile.getParentFile();
            if (!parentDir.exists())
            {
                parentDir.mkdirs();
            }

            String resourcePath = folderName != null ? folderName + "/" + fileName : fileName;
            InputStream resourceStream = plugin.getResource(resourcePath);

            if (resourceStream != null)
            {
                Files.copy(resourceStream, configFile.toPath());
                LoggerUtils.debug("Файл создан из ресурсов: " + fileName);
            }
            else
            {
                configFile.createNewFile();
                LoggerUtils.debug("Создан пустой файл конфигурации: " + fileName);
            }
        }
        catch (IOException exception)
        {
            LoggerUtils.error("Ошибка создания файла конфигурации: " + fileName, exception);
        }
    }

    private File getConfigFile(String fileName, String folderName)
    {
        if (folderName != null && !folderName.isEmpty())
        {
            File folder = new File(plugin.getDataFolder(), folderName);
            return new File(folder, fileName);
        }
        else
        {
            return new File(plugin.getDataFolder(), fileName);
        }
    }


    private String createCacheKey(String fileName, String folderName)
    {
        if (folderName != null && !folderName.isEmpty())
        {
            return folderName + "/" + fileName;
        }
        return fileName;
    }
}