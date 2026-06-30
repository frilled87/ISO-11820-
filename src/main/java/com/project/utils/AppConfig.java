package com.project.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * <h1>应用配置管理 — 线程安全单例</h1>
 *
 * <p>读取 classpath 下的 {@code appsettings.json}，提供类型安全的配置项访问。
 * 支持运行时重新加载配置，所有读取操作线程安全。</p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   // 获取数据库路径
 *   String dbPath = AppConfig.getInstance().getDatabasePath();
 *
 *   // 获取仿真参数
 *   double targetTemp = AppConfig.getInstance().getTargetFurnaceTemp();
 *   double heatingRate = AppConfig.getInstance().getHeatingRatePerSecond();
 *
 *   // 运行时重载配置
 *   AppConfig.getInstance().reload();
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.1
 * @since 2026-06-30
 */
public final class AppConfig {

    // ============================================================
    //  单例
    // ============================================================

    /** 单例实例（volatile 保证可见性） */
    private static volatile AppConfig INSTANCE;

    /** 读写锁：写锁用于 reload，读锁用于所有读取操作 */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** JSON 解析器（线程安全） */
    private final ObjectMapper objectMapper;

    /** 配置文件路径（classpath 内） */
    private static final String CONFIG_RESOURCE = "appsettings.json";

    /** 外部可覆盖的配置文件路径（null 表示不使用） */
    private static volatile String externalConfigPath = null;

    /** 配置根节点 */
    private volatile JsonNode root;

    // ============================================================
    //  构造
    // ============================================================

    /**
     * 私有构造，加载 JSON 配置。
     * 加载顺序：优先外部路径 → 回退 classpath → 空配置兜底。
     */
    private AppConfig() {
        this.objectMapper = new ObjectMapper();
        loadConfiguration();
    }

    /**
     * 获取单例实例（双重检查锁定）。
     *
     * @return 全局唯一的 AppConfig 实例
     */
    public static AppConfig getInstance() {
        if (INSTANCE == null) {
            synchronized (AppConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppConfig();
                }
            }
        }
        return INSTANCE;
    }

    // ============================================================
    //  加载与重载
    // ============================================================

    /**
     * 加载配置文件。优先读取外部路径，其次 classpath，均失败则使用空配置。
     */
    private void loadConfiguration() {
        try {
            // 1) 尝试外部路径
            if (externalConfigPath != null) {
                Path external = Paths.get(externalConfigPath);
                if (Files.exists(external)) {
                    this.root = objectMapper.readTree(Files.readString(external));
                    return;
                }
            }
            // 2) 回退 classpath
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
                if (is != null) {
                    this.root = objectMapper.readTree(is);
                    return;
                }
            }
            // 3) 兜底：空配置
            this.root = objectMapper.createObjectNode();
        } catch (IOException e) {
            this.root = objectMapper.createObjectNode();
            System.err.println("[AppConfig] 配置加载失败，使用空配置: " + e.getMessage());
        }
    }

    /**
     * 运行时重新加载配置文件（线程安全）。
     *
     * @return true 表示重载成功
     */
    public boolean reload() {
        lock.writeLock().lock();
        try {
            loadConfiguration();
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 设置外部配置文件路径（必须在首次调用 {@link #getInstance()} 之前调用）。
     *
     * @param path 外部配置文件绝对路径，如 {@code "D:/myapp/config.json"}
     */
    public static void setExternalConfigPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("外部配置文件路径不能为空");
        }
        if (INSTANCE != null) {
            throw new IllegalStateException(
                    "AppConfig 已初始化，请在首次 getInstance() 之前调用 setExternalConfigPath()");
        }
        externalConfigPath = path;
    }

    // ============================================================
    //  通用读取方法（消除重复 lock/unlock 模板代码）
    // ============================================================

    /**
     * 在读锁保护下执行读取操作，消除重复的 lock/try/finally 代码。
     *
     * @param supplier 读取逻辑
     * @param <T>      返回值类型
     * @return 读取结果
     */
    private <T> T readWithLock(Supplier<T> supplier) {
        lock.readLock().lock();
        try {
            return supplier.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 读取 JSON 路径下的字符串值。
     *
     * @param jsonPath  点分隔路径，如 {@code "Database.SqlitePath"}
     * @param defaultVal 默认值
     * @return 配置值或默认值
     */
    public String getString(String jsonPath, String defaultVal) {
        return readWithLock(() -> {
            JsonNode node = resolvePath(jsonPath);
            return (node != null && !node.isNull()) ? node.asText() : defaultVal;
        });
    }

    /**
     * 读取 JSON 路径下的整数值。
     *
     * @param jsonPath  点分隔路径
     * @param defaultVal 默认值
     * @return 配置值或默认值
     */
    public int getInt(String jsonPath, int defaultVal) {
        return readWithLock(() -> {
            JsonNode node = resolvePath(jsonPath);
            return (node != null && node.isNumber()) ? node.asInt() : defaultVal;
        });
    }

    /**
     * 读取 JSON 路径下的浮点数值。
     *
     * @param jsonPath  点分隔路径
     * @param defaultVal 默认值
     * @return 配置值或默认值
     */
    public double getDouble(String jsonPath, double defaultVal) {
        return readWithLock(() -> {
            JsonNode node = resolvePath(jsonPath);
            return (node != null && node.isNumber()) ? node.asDouble() : defaultVal;
        });
    }

    /**
     * 读取 JSON 路径下的布尔值。
     *
     * @param jsonPath  点分隔路径
     * @param defaultVal 默认值
     * @return 配置值或默认值
     */
    public boolean getBoolean(String jsonPath, boolean defaultVal) {
        return readWithLock(() -> {
            JsonNode node = resolvePath(jsonPath);
            return (node != null && !node.isNull()) ? node.asBoolean() : defaultVal;
        });
    }

    // ============================================================
    //  业务配置快捷方法
    // ============================================================

    // -- Database --
    public String getDatabasePath()             { return getString("Database.SqlitePath", "Data/ISO11820.db"); }

    // -- Simulation --
    public double getTargetFurnaceTemp()        { return getDouble("Simulation.TargetFurnaceTemp", 750.0); }
    public double getHeatingRatePerSecond()     { return getDouble("Simulation.HeatingRatePerSecond", 40.0); }
    public double getTempFluctuation()          { return getDouble("Simulation.TempFluctuation", 0.5); }
    public double getStableThreshold()          { return getDouble("Simulation.StableThreshold", 3.0); }
    public double getInitialFurnaceTemp()       { return getDouble("Simulation.InitialFurnaceTemp", 25.0); }
    public boolean isSimulationEnabled()        { return getBoolean("Simulation.EnableSimulation", true); }

    // -- FileStorage --
    public String getBaseDirectory()            { return getString("FileStorage.BaseDirectory", "./ISO11820_Data"); }
    public String getTestDataDirectory()        { return getString("FileStorage.TestDataDirectory", "./ISO11820_Data/TestData"); }

    // -- Report --
    public String getReportOutputDirectory()    { return getString("Report.OutputDirectory", "./ISO11820_Data/Reports"); }
    public boolean isPdfExportEnabled()         { return getBoolean("Report.EnablePdfExport", true); }
    public boolean isExcelExportEnabled()       { return getBoolean("Report.EnableExcelExport", true); }
    public boolean isCsvAutoSaveEnabled()       { return getBoolean("Report.EnableCsvAutoSave", true); }

    // -- Hardware --
    public int getDefaultConstPower()           { return getInt("Hardware.ConstPower", 2048); }
    public int getPidTemperature()              { return getInt("Hardware.PidTemperature", 750); }

    // -- Logging --
    public String getLogDirectory()             { return getString("Logging.LogDirectory", "./logs"); }
    public int getMaxHistoryDays()              { return getInt("Logging.MaxHistoryDays", 30); }

    // ============================================================
    //  内部方法
    // ============================================================

    /**
     * 按点分隔路径解析 JSON 节点。
     *
     * <pre>
     * resolvePath("Database.SqlitePath") → root.get("Database").get("SqlitePath")
     * </pre>
     *
     * @param jsonPath 点分隔的路径，不能为 null
     * @return 目标节点，路径中断时返回 null
     */
    private JsonNode resolvePath(String jsonPath) {
        if (jsonPath == null || jsonPath.isBlank()) {
            return null;
        }
        JsonNode current = root;
        for (String seg : jsonPath.split("\\.")) {
            if (current == null) return null;
            current = current.get(seg);
        }
        return current;
    }

    /**
     * 返回当前配置的 JSON 字符串表示（用于调试）。
     */
    @Override
    public String toString() {
        return readWithLock(() -> root.toPrettyString());
    }
}