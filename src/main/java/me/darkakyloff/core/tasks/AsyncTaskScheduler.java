package me.darkakyloff.core.tasks;

import me.darkakyloff.core.CorePlugin;
import me.darkakyloff.core.api.config.ConfigurationManager;
import me.darkakyloff.core.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncTaskScheduler
{
    private final Plugin plugin;
    private final Map<String, BukkitTask> registeredTasks;
    private final AtomicInteger taskIdCounter;

    private boolean isRunning = false;

    private int maxThreads = 8;
    private int cacheCleanupInterval = 300;
    private int healthCheckInterval = 120;
    private int statsCollectionInterval = 600;

    public AsyncTaskScheduler(Plugin plugin)
    {
        this.plugin = plugin;
        this.registeredTasks = new ConcurrentHashMap<>();
        this.taskIdCounter = new AtomicInteger(0);

        loadTaskSettings();

        LoggerUtils.debug("AsyncTaskScheduler инициализирован");
    }

    private void loadTaskSettings()
    {
        try
        {
            ConfigurationManager configManager = CorePlugin.getInstance().getConfigurationManager();


            maxThreads = configManager.getInt("settings.yml", "tasks.max-threads", 8);
            cacheCleanupInterval = configManager.getInt("settings.yml", "tasks.system-tasks.cache-cleanup", 300);
            healthCheckInterval = configManager.getInt("settings.yml", "tasks.system-tasks.health-check", 120);
            statsCollectionInterval = configManager.getInt("settings.yml", "tasks.system-tasks.stats-collection", 600);

            LoggerUtils.debug("Настройки планировщика загружены: потоков=" + maxThreads);
        }
        catch (Exception exception)
        {
            LoggerUtils.warning("Ошибка загрузки настроек планировщика, используются дефолтные");

            maxThreads = 8;
            cacheCleanupInterval = 300;
            healthCheckInterval = 120;
            statsCollectionInterval = 600;
        }
    }

    public void startTasks()
    {
        if (isRunning)
        {
            LoggerUtils.warning("Планировщик задач уже запущен");
            return;
        }

        LoggerUtils.debug("Запуск планировщика задач...");

        startSystemTasks();

        isRunning = true;
        LoggerUtils.debug("Планировщик задач запущен");
    }

    public void stopTasks()
    {
        if (!isRunning) return;


        LoggerUtils.debug("Остановка планировщика задач...");

        for (Map.Entry<String, BukkitTask> entry : registeredTasks.entrySet())
        {
            String taskName = entry.getKey();
            BukkitTask task = entry.getValue();

            if (task != null && !task.isCancelled())
            {
                task.cancel();

                LoggerUtils.debug("Задача отменена: " + taskName);
            }
        }

        registeredTasks.clear();
        isRunning = false;

        LoggerUtils.debug("Планировщик задач остановлен");
    }

    private void startSystemTasks()
    {
        scheduleRepeating("cache-cleanup", () ->
        {
            LoggerUtils.debug("Выполнена очистка кешей");
        }, 20L * 60 * 5, 20L * cacheCleanupInterval);

        scheduleRepeating("health-check", () ->
        {
            LoggerUtils.debug("Выполнена проверка состояния системы");
        }, 20L * 30, 20L * healthCheckInterval);

        scheduleRepeating("stats-collection", () ->
        {
            LoggerUtils.debug("Выполнен сбор статистики");
        }, 20L * 60, 20L * statsCollectionInterval);

        LoggerUtils.debug("Системные задачи запущены");
    }

    public String scheduleAsync(String taskName, Runnable runnable, long delay)
    {
        if (!isRunning) return null;

        try
        {
            String uniqueTaskName = generateUniqueTaskName(taskName);

            BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () ->
            {
                try
                {
                    runnable.run();

                    LoggerUtils.debug("Задача выполнена: " + uniqueTaskName);
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка выполнения задачи: " + uniqueTaskName, exception);
                }
                finally
                {
                    registeredTasks.remove(uniqueTaskName);
                }
            }, delay);

            registeredTasks.put(uniqueTaskName, task);

            LoggerUtils.debug("Асинхронная задача запланирована: " + uniqueTaskName + " (задержка: " + delay + " тиков)");
            return uniqueTaskName;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка планирования асинхронной задачи: " + taskName, exception);
            return null;
        }
    }

    public String scheduleRepeating(String taskName, Runnable runnable, long delay, long period)
    {
        if (!isRunning) return null;

        try
        {
            String uniqueTaskName = generateUniqueTaskName(taskName);

            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () ->
            {
                try
                {
                    runnable.run();
                    LoggerUtils.debug("Повторяющаяся задача выполнена: " + uniqueTaskName);
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка выполнения повторяющейся задачи: " + uniqueTaskName, exception);
                }
            }, delay, period);

            registeredTasks.put(uniqueTaskName, task);

            LoggerUtils.debug("Повторяющаяся задача запланирована: " + uniqueTaskName +
                    " (задержка: " + delay + ", период: " + period + " тиков)");
            return uniqueTaskName;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка планирования повторяющейся задачи: " + taskName, exception);
            return null;
        }
    }

    public String scheduleSync(String taskName, Runnable runnable, long delay)
    {
        if (!isRunning) return null;

        try
        {
            String uniqueTaskName = generateUniqueTaskName(taskName);

            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () ->
            {
                try
                {
                    runnable.run();

                    LoggerUtils.debug("Синхронная задача выполнена: " + uniqueTaskName);
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка выполнения синхронной задачи: " + uniqueTaskName, exception);
                }
                finally
                {
                    registeredTasks.remove(uniqueTaskName);
                }
            }, delay);

            registeredTasks.put(uniqueTaskName, task);

            LoggerUtils.debug("Синхронная задача запланирована: " + uniqueTaskName + " (задержка: " + delay + " тиков)");
            return uniqueTaskName;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка планирования синхронной задачи: " + taskName, exception);
            return null;
        }
    }

    public String scheduleSyncRepeating(String taskName, Runnable runnable, long delay, long period)
    {
        if (!isRunning) return null;

        try
        {
            String uniqueTaskName = generateUniqueTaskName(taskName);

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () ->
            {
                try
                {
                    runnable.run();
                    LoggerUtils.debug("Повторяющаяся синхронная задача выполнена: " + uniqueTaskName);
                }
                catch (Exception exception)
                {
                    LoggerUtils.error("Ошибка выполнения повторяющейся синхронной задачи: " + uniqueTaskName, exception);
                }
            }, delay, period);

            registeredTasks.put(uniqueTaskName, task);

            LoggerUtils.debug("Повторяющаяся синхронная задача запланирована: " + uniqueTaskName +
                    " (задержка: " + delay + ", период: " + period + " тиков)");
            return uniqueTaskName;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка планирования повторяющейся синхронной задачи: " + taskName, exception);
            return null;
        }
    }

    public boolean cancelTask(String taskName)
    {
        BukkitTask task = registeredTasks.get(taskName);

        if (task == null)
        {
            LoggerUtils.warning("Задача не найдена: " + taskName);
            return false;
        }

        if (task.isCancelled())
        {
            LoggerUtils.warning("Задача уже отменена: " + taskName);
            registeredTasks.remove(taskName);
            return false;
        }

        try
        {
            task.cancel();
            registeredTasks.remove(taskName);

            LoggerUtils.debug("Задача отменена: " + taskName);
            return true;
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка отмены задачи: " + taskName, exception);
            return false;
        }
    }

    public boolean hasTask(String taskName)
    {
        return registeredTasks.containsKey(taskName);
    }

    public boolean isTaskActive(String taskName)
    {
        BukkitTask task = registeredTasks.get(taskName);
        return task != null && !task.isCancelled();
    }

    public int getActiveTaskCount()
    {
        return (int) registeredTasks.values().stream()
                .filter(task -> task != null && !task.isCancelled())
                .count();
    }

    public java.util.Set<String> getActiveTaskNames()
    {
        return registeredTasks.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isCancelled())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    public BukkitRunnable createRunnable(Runnable runnable)
    {
        return new BukkitRunnable()
        {
            @Override
            public void run()
            {
                runnable.run();
            }
        };
    }

    private String generateUniqueTaskName(String baseName)
    {
        String uniqueName = baseName;
        int counter = 1;

        while (registeredTasks.containsKey(uniqueName))
        {
            uniqueName = baseName + "-" + counter++;
        }

        return uniqueName;
    }

    public boolean isRunning()
    {
        return isRunning;
    }
}