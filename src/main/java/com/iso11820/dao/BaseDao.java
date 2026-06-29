package com.iso11820.dao;

import com.iso11820.dao.util.DbUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用 CRUD 基类 —— 提供基于反射的 JDBC 查询/更新封装。
 * <p>
 * 所有 DAO 实现类继承此类，自动获得通用查询、更新、单行查询、标量查询能力。
 * 子类只需提供具体的 SQL 语句和参数即可。
 * </p>
 *
 * <h3>核心约定</h3>
 * <ul>
 *   <li>数据库列名和实体字段名<b>完全一致</b>，不需要驼峰转下划线</li>
 *   <li>实体类必须有<b>无参构造方法</b></li>
 *   <li>全部使用 {@link PreparedStatement} 预编译 SQL，绝对禁止字符串拼接</li>
 *   <li>字段映射使用 {@link ResultSet#getObject(String)} 自动类型转换</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class OperatorDaoImpl extends BaseDao<Operators> implements OperatorDao {
 *     public OperatorDaoImpl() {
 *         super();
 *     }
 *
 *     public Operators getByUsername(String username) {
 *         return queryOne("SELECT * FROM operators WHERE username = ?", username);
 *     }
 * }
 * }</pre>
 *
 * @param <T> 实体类型，需有无参构造方法
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public abstract class BaseDao<T> {

    /** 实体类型，通过反射解析泛型参数获取 */
    protected final Class<T> entityClass;

    /** 字段反射缓存：Class → (columnName → Field)，避免每次查询都重新反射查找 */
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    /** 字段缓存初始化标记 */
    private volatile boolean fieldCacheInitialized = false;

    /**
     * 构造方法 —— 通过反射解析泛型参数获取实体类型。
     * <p>
     * 子类无需显式传递 Class 对象，自动从泛型声明中推断。
     * 例如：{@code class OperatorDaoImpl extends BaseDao<Operators>}
     * 会自动推断出 {@code entityClass = Operators.class}。
     * </p>
     *
     * @throws IllegalStateException 如果无法解析泛型参数
     */
    @SuppressWarnings("unchecked")
    protected BaseDao() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                this.entityClass = (Class<T>) typeArgs[0];
                return;
            }
        }
        throw new IllegalStateException("无法解析泛型参数，请确保 DAO 实现类正确继承 BaseDao<EntityType>");
    }

    // ==================== 通用查询 ====================

    /**
     * 执行查询并返回实体对象列表。
     * <p>
     * 使用反射将 {@link ResultSet} 每一行映射为实体对象。
     * 数据库列名和实体字段名必须完全一致。
     * </p>
     *
     * @param sql    预编译 SQL 语句，使用 ? 占位符
     * @param params 参数值数组，按 ? 顺序传入
     * @return 实体对象列表，无结果时返回空列表（不会返回 null）
     * @throws RuntimeException 如果 SQL 执行或反射映射失败
     */
    protected List<T> executeQuery(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DbUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            return mapResultSet(rs);
        } catch (SQLException e) {
            throw new DaoException("查询执行失败: " + sql, e);
        } finally {
            DbUtil.closeResource(rs, pstmt, conn);
        }
    }

    /**
     * 执行查询并返回单个实体对象。
     * <p>
     * 如果查询结果超过一行，只返回第一行；无结果时返回 null。
     * </p>
     *
     * @param sql    预编译 SQL 语句
     * @param params 参数值数组
     * @return 单个实体对象，无结果时返回 null
     * @throws RuntimeException 如果 SQL 执行失败
     */
    protected T queryOne(String sql, Object... params) {
        List<T> results = executeQuery(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 执行标量查询（COUNT、MAX、MIN、AVG 等聚合函数）。
     *
     * @param sql    预编译 SQL 语句
     * @param params 参数值数组
     * @return 查询结果（通常为 Integer、Long、Double 等），无结果时返回 null
     * @throws RuntimeException 如果 SQL 执行失败
     */
    protected Object queryScalar(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DbUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getObject(1);
            }
            return null;
        } catch (SQLException e) {
            throw new DaoException("标量查询执行失败: " + sql, e);
        } finally {
            DbUtil.closeResource(rs, pstmt, conn);
        }
    }

    // ==================== 通用更新 ====================

    /**
     * 执行更新操作（INSERT、UPDATE、DELETE）。
     *
     * @param sql    预编译 SQL 语句
     * @param params 参数值数组
     * @return 受影响的行数
     * @throws DaoException 如果 SQL 执行失败
     */
    protected int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DbUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("更新执行失败: " + sql, e);
        } finally {
            DbUtil.closeResource(pstmt, conn);
        }
    }

    /**
     * 批量执行同结构的 SQL 语句。
     * <p>
     * 使用 {@link PreparedStatement#addBatch()} 和 {@link PreparedStatement#executeBatch()}
     * 批量提交，大幅提升大量同构操作（如批量更新传感器温度值）的性能。
     * 所有批次共享同一个数据库连接和事务上下文。
     * </p>
     *
     * <h3>使用示例</h3>
     * <pre>{@code
     * String sql = "UPDATE sensors SET outputvalue = ?, inputvalue = ? WHERE sensorid = ?";
     * List<Object[]> paramsList = new ArrayList<>();
     * for (Sensors s : sensorList) {
     *     paramsList.add(new Object[]{s.getOutputvalue(), s.getInputvalue(), s.getSensorid()});
     * }
     * int[] results = executeBatch(sql, paramsList);
     * }</pre>
     *
     * @param sql        预编译 SQL 语句，使用 ? 占位符
     * @param paramsList 参数列表，每个元素为一行参数数组
     * @return 每条语句受影响的行数数组（与 paramsList 顺序对应）
     * @throws DaoException 如果批量执行失败
     */
    protected int[] executeBatch(String sql, List<Object[]> paramsList) {
        if (paramsList == null || paramsList.isEmpty()) {
            return new int[0];
        }
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DbUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            for (Object[] params : paramsList) {
                setParameters(pstmt, params);
                pstmt.addBatch();
            }
            return pstmt.executeBatch();
        } catch (SQLException e) {
            throw new DaoException("批量执行失败: " + sql, e);
        } finally {
            DbUtil.closeResource(pstmt, conn);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 为 PreparedStatement 设置参数。
     * <p>
     * 参数从索引 1 开始依次设置，与 SQL 中的 ? 占位符一一对应。
     * </p>
     *
     * @param pstmt  PreparedStatement 对象
     * @param params 参数值数组
     * @throws SQLException 如果参数设置失败
     */
    private void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * 将 ResultSet 映射为实体对象列表。
     * <p>
     * 使用反射创建实体实例，遍历 ResultSet 列元数据，
     * 将每列的值通过字段名反射写入实体对象。
     * </p>
     *
     * @param rs 查询结果集
     * @return 实体对象列表
     * @throws SQLException 如果结果集读取失败
     * @throws RuntimeException 如果反射操作失败
     */
    private List<T> mapResultSet(ResultSet rs) throws SQLException {
        List<T> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            T entity = createEntity();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);
                setFieldValue(entity, columnName, value);
            }
            results.add(entity);
        }
        return results;
    }

    /**
     * 通过反射创建实体实例（调用无参构造）。
     *
     * @return 实体实例
     * @throws RuntimeException 如果实例化失败
     */
    private T createEntity() {
        try {
            Constructor<T> constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new DaoException("创建实体实例失败: " + entityClass.getName()
                    + "，请确保有无参构造方法", e);
        }
    }

    /**
     * 通过反射为实体字段赋值。
     * <p>
     * 如果实体类中不存在对应字段，静默跳过（ResultSet 可能包含
     * 实体类不需要的列，不视为错误）。
     * </p>
     *
     * @param entity     实体对象
     * @param fieldName  字段名（与数据库列名一致）
     * @param value      字段值
     */
    private void setFieldValue(T entity, String fieldName, Object value) {
        try {
            Field field = findField(fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, value);
            }
        } catch (Exception e) {
            // 字段赋值失败不中断整个映射，记录警告
            System.err.println("[BaseDao] 字段赋值失败: " + entityClass.getSimpleName()
                    + "." + fieldName + " = " + value + " (" + e.getMessage() + ")");
        }
    }

    /**
     * 在当前实体类及其父类中查找字段。
     * <p>
     * 自动适配多种命名约定：
     * </p>
     * <ol>
     *   <li>精确匹配（如 {@code username} → {@code username}）</li>
     *   <li>首字母小写（如 {@code CalibrationDate} → {@code calibrationDate}）</li>
     *   <li>下划线转驼峰（如 {@code lostweight_per} → {@code lostweightPer}）</li>
     * </ol>
     * <p>
     * 支持继承场景——如果实体类继承自父类，也会在父类中查找字段。
     * </p>
     *
     * @param columnName 数据库列名
     * @return Field 对象，未找到返回 null
     */
    private Field findField(String columnName) {
        // 确保字段缓存已初始化
        ensureFieldCache();

        Map<String, Field> cache = FIELD_CACHE.get(entityClass);
        if (cache == null) {
            return null;
        }

        // 1. 精确匹配（缓存命中）
        Field field = cache.get(columnName);
        if (field != null) return field;

        // 2. 首字母小写（PascalCase → camelCase）
        String camelCase = lowercaseFirst(columnName);
        if (!camelCase.equals(columnName)) {
            field = cache.get(camelCase);
            if (field != null) return field;
        }

        // 3. 下划线转驼峰（snake_case → camelCase）
        String underscoreToCamel = underscoreToCamelCase(columnName);
        if (!underscoreToCamel.equals(columnName) && !underscoreToCamel.equals(camelCase)) {
            field = cache.get(underscoreToCamel);
            if (field != null) return field;
        }

        return null;
    }

    /**
     * 初始化字段缓存 —— 将当前实体类的所有字段以字段名为键存入缓存。
     * <p>
     * 仅在首次查询时执行一次（volatile 双重检查），后续查询直接从缓存获取。
     * 避免每次 ResultSet 映射都重新反射查找字段，提升批量查询性能。
     * </p>
     */
    private void ensureFieldCache() {
        if (fieldCacheInitialized) {
            return;
        }
        synchronized (FIELD_CACHE) {
            if (FIELD_CACHE.containsKey(entityClass)) {
                fieldCacheInitialized = true;
                return;
            }
            Map<String, Field> cache = new ConcurrentHashMap<>();
            Class<?> clazz = entityClass;
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    // 跳过 serialVersionUID 等特殊字段
                    if (!f.getName().startsWith("serial")) {
                        f.setAccessible(true);
                        cache.putIfAbsent(f.getName(), f);
                    }
                }
                clazz = clazz.getSuperclass();
            }
            FIELD_CACHE.put(entityClass, cache);
            fieldCacheInitialized = true;
        }
    }

    /**
     * 在类继承体系中查找字段（已废弃，由缓存版本替代）。
     *
     * @deprecated 使用 {@link #findField(String)} 缓存版本
     */
    @Deprecated
    private Field findFieldInHierarchy(String fieldName) {
        Class<?> clazz = entityClass;
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 将首字母转为小写。
     */
    private String lowercaseFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * 将下划线命名转为驼峰命名。
     * <p>
     * 例如：{@code lostweight_per} → {@code lostweightPer}，
     * {@code maxtf1_time} → {@code maxtf1Time}。
     * </p>
     */
    private String underscoreToCamelCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                result.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 查询所有记录。
     * <p>
     * 便捷方法，等价于 {@code executeQuery("SELECT * FROM " + tableName)}。
     * 子类可覆盖以实现更复杂的查询逻辑。
     * </p>
     *
     * @param tableName 表名
     * @return 实体对象列表
     */
    protected List<T> listAll(String tableName) {
        return executeQuery("SELECT * FROM " + tableName);
    }
}