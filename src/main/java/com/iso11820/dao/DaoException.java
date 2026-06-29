package com.iso11820.dao;

/**
 * 数据访问层自定义运行时异常。
 * <p>
 * 所有 DAO 层的 SQL 异常统一封装为此异常，携带原始异常信息，
 * 方便上层调用方统一捕获和排查问题。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * try {
 *     ...
 * } catch (SQLException e) {
 *     throw new DaoException("查询失败: " + sql, e);
 * }
 * }</pre>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class DaoException extends RuntimeException {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /**
     * 构造方法 —— 仅包含错误消息。
     *
     * @param message 错误描述
     */
    public DaoException(String message) {
        super(message);
    }

    /**
     * 构造方法 —— 包含错误消息和原始异常。
     *
     * @param message 错误描述
     * @param cause   原始异常（如 SQLException）
     */
    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造方法 —— 仅包含原始异常。
     *
     * @param cause 原始异常
     */
    public DaoException(Throwable cause) {
        super(cause);
    }
}