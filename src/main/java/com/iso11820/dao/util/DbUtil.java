package com.iso11820.dao.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import com.iso11820.dao.DaoException;

/**
 * SQLite 数据库连接工具类。
 * <p>
 * 提供数据库连接获取、建表初始化、资源关闭等通用功能。
 * 使用纯 JDBC 操作 SQLite，不依赖任何 ORM 框架。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 首次调用 getConnection() 会自动执行建表初始化
 * try (Connection conn = DbUtil.getConnection()) {
 *     // 执行数据库操作
 * }
 * }</pre>
 *
 * <h3>数据库路径</h3>
 * 默认路径为 {@code Data/ISO11820.db}，可通过 {@link #setDbPath(String)} 覆盖。
 * 后续可改造为从配置文件读取。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public final class DbUtil {

    /** 数据库文件默认路径（相对于工作目录） */
    private static String dbPath = "Data/ISO11820.db";

    /** 建表脚本类路径 */
    private static final String SCHEMA_SQL_PATH = "sql/schema.sql";

    /** 目标数据库版本号 */
    private static final String TARGET_DB_VERSION = "1.0";

    /** 初始化标记：确保只执行一次 */
    private static volatile boolean initialized = false;

    /** 初始化锁 */
    private static final Object INIT_LOCK = new Object();

    /** 线程绑定的连接（用于批量操作/事务内复用） */
    private static final ThreadLocal<Connection> THREAD_CONNECTION = new ThreadLocal<>();

    /** 数据库 URL 缓存 */
    private static volatile String cachedUrl = null;

    // ==================== 私有构造（工具类） ====================

    private DbUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== 路径配置 ====================

    /**
     * 设置数据库文件路径。
     * <p>
     * 需在首次调用 {@link #getConnection()} 之前设置，否则不会生效。
     * 后续可改造为从 {@code appsettings.json} 或系统属性读取。
     * </p>
     *
     * @param path 数据库文件路径，如 "Data/ISO11820.db"
     */
    public static void setDbPath(String path) {
        if (path != null && !path.isBlank()) {
            dbPath = path;
            // 重置缓存
            initialized = false;
            cachedUrl = null;
        }
    }

    /**
     * 获取当前数据库文件路径。
     *
     * @return 数据库文件路径
     */
    public static String getDbPath() {
        return dbPath;
    }

    /**
     * 获取缓存的数据库 JDBC URL。
     */
    private static String getDbUrl() {
        if (cachedUrl == null) {
            Path dbFile = Paths.get(dbPath);
            cachedUrl = "jdbc:sqlite:" + dbFile.toAbsolutePath().toString().replace('\\', '/');
        }
        return cachedUrl;
    }

    // ==================== 连接管理 ====================

    /**
     * 获取 SQLite 数据库连接。
     * <p>
     * 首次调用时自动执行建表初始化（幂等操作，重复调用不会重复建表）。
     * 调用方应使用 try-with-resources 确保连接被正确关闭。
     * </p>
     *
     * @return 数据库连接，不会返回 null
     * @throws RuntimeException 如果数据库连接失败
     */
    public static Connection getConnection() {
        // 首次调用时执行初始化
        if (!initialized) {
            synchronized (INIT_LOCK) {
                if (!initialized) {
                    initDatabase();
                    initialized = true;
                }
            }
        }

        try {
            // 确保数据目录存在
            Path dbFile = Paths.get(dbPath);
            Path parentDir = dbFile.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 加载 SQLite 驱动并建立连接
            Class.forName("org.sqlite.JDBC");
            String url = getDbUrl();
            Connection conn = DriverManager.getConnection(url);

            // 开启外键约束（SQLite 默认关闭）
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }

            return conn;
        } catch (ClassNotFoundException e) {
            throw new DaoException("SQLite JDBC 驱动未找到，请检查 sqlite-jdbc 依赖", e);
        } catch (SQLException e) {
            throw new DaoException("获取数据库连接失败: " + dbPath, e);
        } catch (IOException e) {
            throw new DaoException("创建数据库目录失败: " + dbPath, e);
        }
    }

    // ==================== 事务管理 ====================

    /**
     * 开启事务并返回绑定了事务的数据库连接。
     * <p>
     * SQLite 事务为单连接级别，事务内所有操作必须复用同一个 Connection。
     * 调用方负责在 finally 块中调用 {@link #closeTransactionConnection(Connection)}
     * 关闭连接。事务内禁止使用 try-with-resources 自动关闭连接。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * Connection conn = null;
     * try {
     *     conn = DbUtil.beginTransaction();
     *     // 执行多个数据库操作...
     *     DbUtil.commitTransaction(conn);
     * } catch (Exception e) {
     *     DbUtil.rollbackTransaction(conn);
     *     throw new DaoException("事务执行失败", e);
     * } finally {
     *     DbUtil.closeTransactionConnection(conn);
     * }
     * }</pre>
     *
     * @return 已开启事务的数据库连接（autoCommit = false）
     * @throws DaoException 如果获取连接或开启事务失败
     */
    public static Connection beginTransaction() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            throw new DaoException("开启事务失败", e);
        }
    }

    /**
     * 提交事务。
     *
     * @param conn 事务连接，不可为 null
     * @throws DaoException 如果提交失败
     */
    public static void commitTransaction(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.commit();
        } catch (SQLException e) {
            throw new DaoException("提交事务失败", e);
        }
    }

    /**
     * 回滚事务。
     * <p>
     * 回滚失败仅打印警告，不抛出异常，确保不会覆盖原始业务异常。
     * </p>
     *
     * @param conn 事务连接，可为 null
     */
    public static void rollbackTransaction(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException e) {
            System.err.println("[DbUtil] 事务回滚失败: " + e.getMessage());
        }
    }

    /**
     * 关闭事务连接。
     * <p>
     * 关闭前强制回滚未提交的事务，确保连接安全归还。
     * </p>
     *
     * @param conn 事务连接，可为 null
     */
    public static void closeTransactionConnection(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            if (!conn.isClosed()) {
                // 关闭前回滚未提交的事务
                try {
                    if (!conn.getAutoCommit()) {
                        conn.rollback();
                    }
                } catch (SQLException ignored) {
                    // 忽略回滚异常
                }
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("[DbUtil] 恢复 autoCommit 失败: " + e.getMessage());
        }
        closeResource(conn);
    }

    // ==================== 线程绑定连接（批量/事务优化） ====================

    /**
     * 获取当前线程绑定的数据库连接。
     * <p>
     * 批量操作或事务内，同一线程多次调用此方法返回同一个连接，
     * 避免频繁创建/销毁连接的开销。首次调用时自动创建新连接。
     * 调用方应在 finally 中调用 {@link #releaseThreadConnection()} 释放。
     * </p>
     *
     * @return 线程绑定的数据库连接
     * @throws DaoException 如果获取连接失败
     */
    public static Connection getThreadConnection() {
        Connection conn = THREAD_CONNECTION.get();
        if (conn == null) {
            conn = getConnection();
            THREAD_CONNECTION.set(conn);
        }
        return conn;
    }

    /**
     * 释放当前线程绑定的数据库连接。
     * <p>
     * 关闭连接并从 ThreadLocal 中移除，防止内存泄漏。
     * 多次调用安全。
     * </p>
     */
    public static void releaseThreadConnection() {
        Connection conn = THREAD_CONNECTION.get();
        if (conn != null) {
            closeResource(conn);
            THREAD_CONNECTION.remove();
        }
    }

    // ==================== 数据库初始化 ====================

    /**
     * 执行数据库初始化：从类路径读取 {@code sql/schema.sql} 建表脚本并执行。
     * <p>
     * 使用 {@code CREATE TABLE IF NOT EXISTS} 和 {@code INSERT ... WHERE NOT EXISTS}
     * 保证幂等性——重复调用不会重复建表或重复插入数据。
     * </p>
     *
     * @throws RuntimeException 如果脚本读取或执行失败
     */
    private static void initDatabase() {
        String schemaSql = readSchemaSql();
        if (schemaSql == null || schemaSql.isBlank()) {
            throw new DaoException("建表脚本 sql/schema.sql 内容为空");
        }

        // 按分号分割 SQL 语句，过滤空语句和纯注释行
        String[] statements = schemaSql.split(";");

        try (Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:" + Paths.get(dbPath).toAbsolutePath().toString().replace('\\', '/'));
             Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                try {
                    stmt.execute(trimmed);
                } catch (SQLException e) {
                    // 单条语句失败不中断整体初始化（IF NOT EXISTS 已处理大部分幂等场景）
                    System.err.println("[DbUtil] SQL 执行警告: " + e.getMessage());
                    System.err.println("[DbUtil] SQL 片段: " +
                            trimmed.substring(0, Math.min(80, trimmed.length())) + "...");
                }
            }
            System.out.println("[DbUtil] 数据库初始化完成: " + dbPath);
            // 执行版本检查和升级
            checkAndUpgrade(conn);
        } catch (SQLException e) {
            throw new DaoException("数据库初始化失败: " + dbPath, e);
        }
    }

    /**
     * 从类路径读取建表脚本内容。
     *
     * @return 脚本全文，读取失败返回 null
     */
    private static String readSchemaSql() {
        try (InputStream is = DbUtil.class.getClassLoader().getResourceAsStream(SCHEMA_SQL_PATH)) {
            if (is == null) {
                // 回退：尝试从工作目录直接读取
                Path filePath = Paths.get(SCHEMA_SQL_PATH);
                if (Files.exists(filePath)) {
                    return Files.readString(filePath);
                }
                throw new DaoException("建表脚本未找到: " + SCHEMA_SQL_PATH
                        + "，请确保 sql/schema.sql 在 classpath 或工作目录下");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new DaoException("读取建表脚本失败: " + SCHEMA_SQL_PATH, e);
        }
    }

    /**
     * 强制重新初始化数据库（用于测试或重置场景）。
     * <p>
     * 调用后下次 {@link #getConnection()} 会重新执行建表脚本。
     * </p>
     */
    public static void resetInitialization() {
        synchronized (INIT_LOCK) {
            initialized = false;
        }
    }

    // ==================== 数据库版本管理 ====================

    /**
     * 读取当前数据库版本号。
     *
     * @param conn 数据库连接
     * @return 当前版本号，未初始化时返回 "0.0"
     */
    private static String getCurrentDbVersion(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT config_value FROM system_config WHERE config_key = 'db_version'")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            // system_config 表可能尚不存在，返回默认版本
            System.err.println("[DbUtil] 读取数据库版本失败: " + e.getMessage());
        }
        return "0.0";
    }

    /**
     * 更新数据库版本号。
     *
     * @param conn    数据库连接
     * @param version 新版本号
     */
    private static void updateDbVersion(Connection conn, String version) {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO system_config (config_key, config_value) VALUES ('db_version', ?)")) {
            pstmt.setString(1, version);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DbUtil] 更新数据库版本失败: " + e.getMessage());
        }
    }

    /**
     * 检查并执行数据库版本升级。
     * <p>
     * 从当前版本按顺序逐级升级到目标版本。
     * 升级脚本幂等执行，重复调用不会报错。
     * 后续新增字段或表时，在此方法中添加对应的升级方法调用即可。
     * </p>
     *
     * @param conn 数据库连接
     */
    private static void checkAndUpgrade(Connection conn) {
        String currentVersion = getCurrentDbVersion(conn);
        System.out.println("[DbUtil] 当前数据库版本: " + currentVersion
                + "，目标版本: " + TARGET_DB_VERSION);

        if (TARGET_DB_VERSION.equals(currentVersion)) {
            return;
        }

        // 逐级升级（后续新增版本在此处添加对应的升级方法调用）
        if (compareVersion(currentVersion, "1.0") < 0) {
            upgradeTo_1_0(conn);
        }
        // 预留：后续新增字段/表时在此处扩展
        // if (compareVersion(currentVersion, "1.1") < 0) {
        //     upgradeTo_1_1(conn);
        // }

        updateDbVersion(conn, TARGET_DB_VERSION);
        System.out.println("[DbUtil] 数据库升级完成: " + currentVersion + " → " + TARGET_DB_VERSION);
    }

    /**
     * 升级到版本 1.0（创建 system_config 表，使用建表脚本中已包含的 CREATE TABLE IF NOT EXISTS）。
     * <p>
     * 幂等操作：system_config 表已在 schema.sql 中通过 IF NOT EXISTS 创建，
     * 此处仅确保版本号记录存在。
     * </p>
     *
     * @param conn 数据库连接
     */
    private static void upgradeTo_1_0(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 确保 system_config 表存在（schema.sql 中已通过 IF NOT EXISTS 创建）
            stmt.execute("CREATE TABLE IF NOT EXISTS system_config ("
                    + "config_key TEXT PRIMARY KEY, config_value TEXT)");
            System.out.println("[DbUtil] 升级到 v1.0: system_config 表已就绪");
        } catch (SQLException e) {
            throw new DaoException("数据库升级到 v1.0 失败", e);
        }
    }

    /**
     * 比较两个版本号。
     *
     * @param v1 版本号1
     * @param v2 版本号2
     * @return 负数如果 v1 < v2，0 如果相等，正数如果 v1 > v2
     */
    private static int compareVersion(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) {
                return n1 - n2;
            }
        }
        return 0;
    }

    // ==================== 资源关闭 ====================

    /**
     * 安全关闭数据库资源，逐个关闭，单个失败不影响后续关闭。
     * <p>
     * 支持 {@link Connection}、{@link Statement}、{@link ResultSet}
     * 等所有 {@link AutoCloseable} 资源。
     * </p>
     *
     * @param resources 需要关闭的资源列表，可变参数
     */
    public static void closeResource(AutoCloseable... resources) {
        if (resources == null) {
            return;
        }
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("[DbUtil] 关闭资源异常: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 安全关闭 {@link Connection}。
     *
     * @param conn 数据库连接，可为 null
     */
    public static void closeConnection(Connection conn) {
        closeResource(conn);
    }

    /**
     * 安全关闭 {@link Statement}。
     *
     * @param stmt Statement 对象，可为 null
     */
    public static void closeStatement(Statement stmt) {
        closeResource(stmt);
    }

    /**
     * 安全关闭 {@link ResultSet}。
     *
     * @param rs ResultSet 对象，可为 null
     */
    public static void closeResultSet(ResultSet rs) {
        closeResource(rs);
    }
}