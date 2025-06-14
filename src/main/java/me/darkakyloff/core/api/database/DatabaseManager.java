package me.darkakyloff.core.api.database;

import me.darkakyloff.core.api.config.ConfigurationManager;
import me.darkakyloff.core.utils.LoggerUtils;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DatabaseManager
{
    private final ConfigurationManager configManager;
    private final Map<String, HikariDataSource> dataSources;
    private final Executor databaseExecutor;

    private boolean debugMode = false;

    public DatabaseManager(ConfigurationManager configManager)
    {
        this.configManager = configManager;
        this.dataSources = new ConcurrentHashMap<>();

        this.databaseExecutor = Executors.newFixedThreadPool(8, runnable ->
        {
            Thread thread = new Thread(runnable, "Database-Worker");
            thread.setDaemon(true);
            return thread;
        });

        LoggerUtils.debug("DatabaseManager инициализирован");
    }

    public boolean connectToDatabase(String databaseKey)
    {
        try
        {
            DatabaseConfig config = new DatabaseConfig(configManager, databaseKey);

            if (!loadDatabaseDriver())
            {
                LoggerUtils.error("Драйвер базы данных не найден!");
                return false;
            }

            createDatabaseIfNotExists(config);

            HikariDataSource dataSource = new HikariDataSource(config.createHikariConfig());

            if (testConnection(dataSource))
            {
                dataSources.put(databaseKey, dataSource);
                return true;
            }
            else
            {
                dataSource.close();
                return false;
            }
        }
        catch (Exception exception)
        {
            LoggerUtils.error("Ошибка подключения к базе данных " + databaseKey, exception);
            return false;
        }
    }

    private boolean loadDatabaseDriver()
    {
        try
        {
            Class.forName("org.mariadb.jdbc.Driver");
            LoggerUtils.debug("MariaDB драйвер загружен");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            try
            {
                Class.forName("com.mysql.cj.jdbc.Driver");
                LoggerUtils.debug("MySQL драйвер загружен");
                return true;
            }
            catch (ClassNotFoundException e2)
            {
                LoggerUtils.error("Драйвер базы данных не найден! Добавьте mariadb-java-client.jar в папку plugins");
                return false;
            }
        }
    }

    private void createDatabaseIfNotExists(DatabaseConfig config)
    {
        try (Connection connection = DriverManager.getConnection(
                config.getServerUrl(), config.getUsername(), config.getPassword()))
        {
            if (!databaseExists(connection, config.getDatabase()))
            {
                LoggerUtils.debug("Создание базы данных: " + config.getDatabase());
                createDatabase(connection, config.getDatabase());
                LoggerUtils.debug("База данных создана: " + config.getDatabase());
            }
        }
        catch (SQLException exception)
        {
            LoggerUtils.error("Ошибка создания базы данных: " + config.getDatabase(), exception);
        }
    }

    private boolean databaseExists(Connection connection, String databaseName) throws SQLException
    {
        String query = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(query))
        {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery())
            {
                return resultSet.next();
            }
        }
    }

    private void createDatabase(Connection connection, String databaseName) throws SQLException
    {
        String query = "CREATE DATABASE `" + databaseName + "` " +
                "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        try (Statement statement = connection.createStatement())
        {
            statement.executeUpdate(query);
        }
    }

    private boolean testConnection(HikariDataSource dataSource)
    {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1"))
        {
            return resultSet.next() && resultSet.getInt(1) == 1;
        }
        catch (SQLException exception)
        {
            LoggerUtils.error("Тест соединения с БД провален", exception);
            return false;
        }
    }

    public void executeUpdateAsync(String databaseKey, String query, Consumer<Boolean> callback, Object... params)
    {
        CompletableFuture.supplyAsync(() ->
        {
            HikariDataSource dataSource = dataSources.get(databaseKey);
            if (dataSource == null)
            {
                LoggerUtils.error("База данных не найдена: " + databaseKey);
                return false;
            }

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query))
            {
                setParameters(statement, params);

                if (debugMode)
                {
                    LoggerUtils.debug("Выполнение UPDATE: " + query);
                }

                statement.executeUpdate();
                return true;
            }
            catch (SQLException exception)
            {
                LoggerUtils.error("Ошибка выполнения UPDATE: " + query, exception);
                return false;
            }
        }, databaseExecutor).thenAccept(callback);
    }

    public void executeQueryAsync(String databaseKey, String query, Consumer<ResultSet> callback, Object... params)
    {
        CompletableFuture.runAsync(() ->
        {
            HikariDataSource dataSource = dataSources.get(databaseKey);
            if (dataSource == null)
            {
                LoggerUtils.error("База данных не найдена: " + databaseKey);
                return;
            }

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query))
            {
                setParameters(statement, params);

                if (debugMode)
                {
                    LoggerUtils.debug("Выполнение SELECT: " + query);
                }

                try (ResultSet resultSet = statement.executeQuery())
                {
                    callback.accept(resultSet);
                }
            }
            catch (SQLException exception)
            {
                LoggerUtils.error("Ошибка выполнения SELECT: " + query, exception);
            }
        }, databaseExecutor);
    }

    public boolean executeUpdateSync(String databaseKey, String query, Object... params)
    {
        HikariDataSource dataSource = dataSources.get(databaseKey);
        if (dataSource == null)
        {
            LoggerUtils.error("База данных не найдена: " + databaseKey);
            return false;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query))
        {
            setParameters(statement, params);

            if (debugMode)
            {
                LoggerUtils.debug("Выполнение SYNC UPDATE: " + query);
            }

            statement.executeUpdate();
            return true;
        }
        catch (SQLException exception)
        {
            LoggerUtils.error("Ошибка выполнения SYNC UPDATE: " + query, exception);
            return false;
        }
    }

    public void executeBatch(String databaseKey, String query, List<Object[]> paramsList, Consumer<Boolean> callback)
    {
        CompletableFuture.supplyAsync(() ->
        {
            HikariDataSource dataSource = dataSources.get(databaseKey);
            if (dataSource == null)
            {
                LoggerUtils.error("База данных не найдена: " + databaseKey);
                return false;
            }

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query))
            {
                connection.setAutoCommit(false);

                if (debugMode)
                {
                    LoggerUtils.debug("Выполнение BATCH: " + query + " (" + paramsList.size() + " операций)");
                }

                for (Object[] params : paramsList)
                {
                    setParameters(statement, params);
                    statement.addBatch();
                }

                statement.executeBatch();
                connection.commit();
                return true;
            }
            catch (SQLException exception)
            {
                LoggerUtils.error("Ошибка выполнения BATCH: " + query, exception);
                return false;
            }
        }, databaseExecutor).thenAccept(callback);
    }

    public void createTable(String databaseKey, String tableName, Map<String, String> columns, Consumer<Boolean> callback)
    {
        StringBuilder queryBuilder = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        queryBuilder.append("`").append(tableName).append("` (");

        boolean first = true;
        for (Map.Entry<String, String> entry : columns.entrySet())
        {
            if (!first)
            {
                queryBuilder.append(", ");
            }
            queryBuilder.append("`").append(entry.getKey()).append("` ").append(entry.getValue());
            first = false;
        }

        queryBuilder.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        executeUpdateAsync(databaseKey, queryBuilder.toString(), success ->
        {
            if (success)
            {
                LoggerUtils.debug("Таблица создана или уже существует: " + tableName);
            }
            else
            {
                LoggerUtils.error("Не удалось создать таблицу: " + tableName);
            }
            callback.accept(success);
        });
    }

    public void shutdown()
    {
        LoggerUtils.debug("Остановка системы базы данных...");

        for (Map.Entry<String, HikariDataSource> entry : dataSources.entrySet())
        {
            String key = entry.getKey();
            HikariDataSource dataSource = entry.getValue();

            try
            {
                dataSource.close();
                LoggerUtils.debug("Пул соединений '" + key + "' закрыт");
            }
            catch (Exception exception)
            {
                LoggerUtils.error("Ошибка закрытия пула соединений: " + key, exception);
            }
        }

        dataSources.clear();

        if (databaseExecutor instanceof java.util.concurrent.ExecutorService)
        {
            ((java.util.concurrent.ExecutorService) databaseExecutor).shutdown();
        }

        LoggerUtils.debug("Система базы данных остановлена");
    }

    private void setParameters(PreparedStatement statement, Object... params) throws SQLException
    {
        for (int i = 0; i < params.length; i++)
        {
            Object param = params[i];
            if (param == null)
            {
                statement.setNull(i + 1, Types.NULL);
            }
            else
            {
                statement.setObject(i + 1, param);
            }
        }
    }
}