package me.darkakyloff.core.api.database;

import me.darkakyloff.core.api.config.ConfigurationManager;
import com.zaxxer.hikari.HikariConfig;

public class DatabaseConfig
{
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String database;
    private final String databaseKey;

    public DatabaseConfig(ConfigurationManager configManager, String databaseKey)
    {
        this.databaseKey = databaseKey;

        String basePath = databaseKey;

        this.host = configManager.getString("database.yml", basePath + ".host", "localhost");
        this.port = configManager.getInt("database.yml", basePath + ".port", 3306);
        this.username = configManager.getString("database.yml", basePath + ".username", "root");
        this.password = configManager.getString("database.yml", basePath + ".password", "");
        this.database = configManager.getString("database.yml", basePath + ".database", "minecraft_core");
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDatabase() { return database; }
    public String getDatabaseKey() { return databaseKey; }

    public String getServerUrl()
    {
        String protocol = determineJdbcProtocol();
        return protocol + "://" + host + ":" + port +
                "/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" +
                "&useUnicode=true&characterEncoding=utf8&autoReconnect=true";
    }

    public String getDatabaseUrl()
    {
        String protocol = determineJdbcProtocol();
        return protocol + "://" + host + ":" + port + "/" + database +
                "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" +
                "&useUnicode=true&characterEncoding=utf8&autoReconnect=true" +
                "&cachePrepStmts=true&useServerPrepStmts=true&rewriteBatchedStatements=true";
    }

    public HikariConfig createHikariConfig()
    {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(getDatabaseUrl());
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(determineDriverClassName());

        hikariConfig.setMaximumPoolSize(15);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTimeout(20000);
        hikariConfig.setIdleTimeout(300000);
        hikariConfig.setMaxLifetime(1200000);
        hikariConfig.setLeakDetectionThreshold(60000);

        hikariConfig.setPoolName("Core-DB-Pool-" + databaseKey);

        configurePerformanceSettings(hikariConfig);

        configureStabilitySettings(hikariConfig);

        return hikariConfig;
    }

    private void configurePerformanceSettings(HikariConfig hikariConfig)
    {
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
    }

    private void configureStabilitySettings(HikariConfig hikariConfig)
    {
        hikariConfig.addDataSourceProperty("autoReconnect", "true");
        hikariConfig.addDataSourceProperty("failOverReadOnly", "false");
        hikariConfig.addDataSourceProperty("maxReconnects", "3");
        hikariConfig.addDataSourceProperty("connectTimeout", "20000");
        hikariConfig.addDataSourceProperty("socketTimeout", "30000");
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.addDataSourceProperty("validationTimeout", "5000");
    }

    private String determineJdbcProtocol()
    {
        try
        {
            Class.forName("org.mariadb.jdbc.Driver");
            return "jdbc:mariadb";
        }
        catch (ClassNotFoundException e)
        {
            try
            {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return "jdbc:mysql";
            }
            catch (ClassNotFoundException e2)
            {
                return "jdbc:mysql";
            }
        }
    }

    private String determineDriverClassName()
    {
        try
        {
            Class.forName("org.mariadb.jdbc.Driver");
            return "org.mariadb.jdbc.Driver";
        }
        catch (ClassNotFoundException e)
        {
            try
            {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return "com.mysql.cj.jdbc.Driver";
            }
            catch (ClassNotFoundException e2)
            {
                return "com.mysql.cj.jdbc.Driver";
            }
        }
    }
}