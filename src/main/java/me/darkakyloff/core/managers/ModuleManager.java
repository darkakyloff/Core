package me.darkakyloff.core.managers;

import me.darkakyloff.core.modules.BaseModule;
import me.darkakyloff.core.modules.core.CoreModule;
import me.darkakyloff.core.modules.economy.EconomyModule;
import me.darkakyloff.core.utils.LoggerUtils;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class ModuleManager
{
    private final Plugin plugin;
    private final Map<String, BaseModule> modules;
    private final Map<String, ModuleInfo> moduleInfos;
    private final List<BaseModule> loadOrder;

    private boolean isInitialized = false;
    private boolean isShuttingDown = false;

    public ModuleManager(Plugin plugin)
    {
        this.plugin = plugin;
        this.modules = new ConcurrentHashMap<>();
        this.moduleInfos = new ConcurrentHashMap<>();
        this.loadOrder = new CopyOnWriteArrayList<>();

        LoggerUtils.debug("ModuleManager инициализирован");
    }

    public void loadAllModules()
    {
        if (isInitialized)
        {
            LoggerUtils.warning("Модульная система уже инициализирована");
            return;
        }

        try
        {
            registerAllModules();

            loadModulesInOrder();

            isInitialized = true;

            LoggerUtils.debug("Модульная система инициализирована");
            LoggerUtils.stats("Статистика модулей", getModuleStats());
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Критическая ошибка при загрузке модулей", exception);
            throw new RuntimeException("Не удалось инициализировать модульную систему", exception);
        }
    }

    private void registerAllModules()
    {
        LoggerUtils.debug("Регистрация модулей...");

        registerCoreModules();

        registerUserModules();

        LoggerUtils.debug("Зарегистрировано модулей: " + moduleInfos.size());
    }

    private void registerCoreModules()
    {
        registerModule(new CoreModule(plugin, "Core"));
    }

    private void registerUserModules()
    {
        registerModule(new EconomyModule(plugin, "Economy"));
    }

    public void registerModule(BaseModule module)
    {
        String moduleName = module.getName();

        if (moduleInfos.containsKey(moduleName))
        {
            LoggerUtils.warning("Модуль '" + moduleName + "' уже зарегистрирован");
            return;
        }
        ModuleInfo info = new ModuleInfo(module);
        moduleInfos.put(moduleName, info);

        LoggerUtils.debug("Модуль зарегистрирован: " + moduleName);
    }

    private void loadModulesInOrder()
    {
        LoggerUtils.debug("Определение порядка загрузки модулей...");

        List<String> loadOrder = calculateLoadOrder();

        LoggerUtils.debug("Порядок загрузки: " + String.join(" -> ", loadOrder));

        for (String moduleName : loadOrder)
        {
            loadModule(moduleName);
        }
    }

    private List<String> calculateLoadOrder()
    {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String moduleName : moduleInfos.keySet())
        {
            if (!visited.contains(moduleName))
            {
                if (!topologicalSort(moduleName, visited, visiting, result))
                {
                    throw new RuntimeException("Обнаружена циклическая зависимость в модулях");
                }
            }
        }

        return result;
    }

    private boolean topologicalSort(String moduleName, Set<String> visited,
                                    Set<String> visiting, List<String> result)
    {
        if (visiting.contains(moduleName))
        {
            LoggerUtils.error("Циклическая зависимость обнаружена в модуле: " + moduleName);
            return false;
        }

        if (visited.contains(moduleName)) return true;

        visiting.add(moduleName);

        ModuleInfo info = moduleInfos.get(moduleName);
        if (info != null)
        {
            for (String dependency : info.getDependencies())
            {
                if (!topologicalSort(dependency, visited, visiting, result)) return false;
            }
        }

        visiting.remove(moduleName);
        visited.add(moduleName);
        result.add(moduleName);

        return true;
    }

    public void loadModule(String moduleName)
    {
        if (isShuttingDown)
        {
            LoggerUtils.warning("Система в процессе остановки, загрузка модуля отменена: " + moduleName);
            return;
        }

        ModuleInfo info = moduleInfos.get(moduleName);
        if (info == null)
        {
            LoggerUtils.error("Модуль не найден: " + moduleName);
            return;
        }

        if (info.getState() == ModuleState.LOADED)
        {
            LoggerUtils.warning("Модуль уже загружен: " + moduleName);
            return;
        }

        try
        {
            LoggerUtils.debug("Загрузка модуля: " + moduleName);

            if (!checkDependencies(moduleName))
            {
                LoggerUtils.error("Не удалось загрузить зависимости для модуля: " + moduleName);
                info.setState(ModuleState.FAILED);
                return;
            }

            info.setState(ModuleState.LOADING);

            BaseModule module = info.getModule();
            module.onLoad();

            modules.put(moduleName, module);
            loadOrder.add(module);

            info.setState(ModuleState.LOADED);
            info.setLoadTime(System.currentTimeMillis());

            LoggerUtils.module(moduleName, "загружен");
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка загрузки модуля: " + moduleName, exception);
            info.setState(ModuleState.FAILED);
            info.setLastError(exception.getMessage());
        }
    }

    public void unloadModule(String moduleName)
    {
        ModuleInfo info = moduleInfos.get(moduleName);
        if (info == null)
        {
            LoggerUtils.warning("Модуль не найден: " + moduleName);
            return;
        }

        if (info.getState() != ModuleState.LOADED)
        {
            LoggerUtils.warning("Модуль не загружен: " + moduleName);
            return;
        }

        try
        {
            LoggerUtils.debug("Выгрузка модуля: " + moduleName);

            List<String> dependentModules = findDependentModules(moduleName);
            if (!dependentModules.isEmpty())
            {
                LoggerUtils.warning("Выгрузка зависящих модулей: " + String.join(", ", dependentModules));

                for (String dependentModule : dependentModules)
                {
                    unloadModule(dependentModule);
                }
            }

            info.setState(ModuleState.UNLOADING);

            BaseModule module = modules.get(moduleName);

            if (module != null) module.onUnload();

            modules.remove(moduleName);
            loadOrder.remove(module);

            info.setState(ModuleState.UNLOADED);

            LoggerUtils.module(moduleName, "выгружен");
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка выгрузки модуля: " + moduleName, exception);
            info.setState(ModuleState.FAILED);
            info.setLastError(exception.getMessage());
        }
    }

    public void reloadModule(String moduleName)
    {
        LoggerUtils.debug("Перезагрузка модуля: " + moduleName);

        unloadModule(moduleName);
        loadModule(moduleName);

        LoggerUtils.module(moduleName, "перезагружен");
    }

    public void unloadAllModules()
    {
        if (!isInitialized)
        {
            LoggerUtils.warning("Модульная система не инициализирована");
            return;
        }

        isShuttingDown = true;

        LoggerUtils.separator("ВЫГРУЗКА МОДУЛЕЙ");

        List<BaseModule> modulesToUnload = new ArrayList<>(loadOrder);
        Collections.reverse(modulesToUnload);

        for (BaseModule module : modulesToUnload)
        {
            unloadModule(module.getName());
        }

        modules.clear();
        loadOrder.clear();
        moduleInfos.clear();

        isInitialized = false;
        isShuttingDown = false;

        LoggerUtils.debug("Все модули выгружены");
    }

    private boolean checkDependencies(String moduleName)
    {
        ModuleInfo info = moduleInfos.get(moduleName);
        if (info == null) return false;

        for (String dependency : info.getDependencies())
        {
            ModuleInfo depInfo = moduleInfos.get(dependency);
            if (depInfo == null)
            {
                LoggerUtils.error("Зависимость не найдена: " + dependency + " (требуется для " + moduleName + ")");
                return false;
            }

            if (depInfo.getState() != ModuleState.LOADED)
            {
                loadModule(dependency);

                if (depInfo.getState() != ModuleState.LOADED)
                {
                    LoggerUtils.error("Не удалось загрузить зависимость: " + dependency);
                    return false;
                }
            }
        }

        return true;
    }

    private List<String> findDependentModules(String moduleName)
    {
        List<String> dependentModules = new ArrayList<>();

        for (ModuleInfo info : moduleInfos.values())
        {
            if (info.getDependencies().contains(moduleName) &&
                    info.getState() == ModuleState.LOADED)
            {
                dependentModules.add(info.getModule().getName());
            }
        }

        return dependentModules;
    }

    public BaseModule getModule(String moduleName)
    {
        return modules.get(moduleName);
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseModule> T getModule(Class<T> moduleClass)
    {
        for (BaseModule module : modules.values())
        {
            if (moduleClass.isInstance(module))
            {
                return (T) module;
            }
        }
        return null;
    }

    public boolean hasModule(String moduleName)
    {
        return moduleInfos.containsKey(moduleName);
    }

    public boolean isModuleLoaded(String moduleName)
    {
        ModuleInfo info = moduleInfos.get(moduleName);
        return info != null && info.getState() == ModuleState.LOADED;
    }

    public ModuleState getModuleState(String moduleName)
    {
        ModuleInfo info = moduleInfos.get(moduleName);
        return info != null ? info.getState() : null;
    }

    public List<String> getAllModuleNames()
    {
        return new ArrayList<>(moduleInfos.keySet());
    }

    public List<BaseModule> getLoadedModules()
    {
        return new ArrayList<>(modules.values());
    }

    public ModuleInfo getModuleInfo(String moduleName)
    {
        return moduleInfos.get(moduleName);
    }

    public String[] getModuleStats()
    {
        int total = moduleInfos.size();
        long loaded = moduleInfos.values().stream()
                .mapToLong(info -> info.getState() == ModuleState.LOADED ? 1 : 0)
                .sum();
        long failed = moduleInfos.values().stream()
                .mapToLong(info -> info.getState() == ModuleState.FAILED ? 1 : 0)
                .sum();

        return new String[]
                {
                        "Всего модулей", String.valueOf(total),
                        "Загружено", String.valueOf(loaded),
                        "Ошибок", String.valueOf(failed),
                };
    }

    public boolean isInitialized()
    {
        return isInitialized;
    }

    public boolean isShuttingDown()
    {
        return isShuttingDown;
    }
}