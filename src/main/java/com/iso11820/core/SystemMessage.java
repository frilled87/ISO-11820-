package com.iso11820.core;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 系统消息 —— 带时间戳的状态变更和业务提示日志条目。
 * <p>
 * 消息由 {@link TestMaster} 在试验过程中自动生成，包括：
 * </p>
 * <ul>
 *   <li>系统初始化</li>
 *   <li>状态切换（IDLE→PREPARING→READY→RECORDING→COMPLETE）</li>
 *   <li>温度稳定通知</li>
 *   <li>自动回退通知</li>
 *   <li>试验完成通知</li>
 *   <li>手动停止记录</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * 本记录是不可变的（{@code record} 类型），天然线程安全。
 *
 * @param time    消息时间，格式 HH:mm:ss，如 "18:28:14"
 * @param message 消息内容，如 "温度已稳定，可以开始记录"
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public record SystemMessage(String time, String message) {

    /** 时间格式化器：HH:mm:ss */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 创建带有当前时间的系统消息。
     *
     * @param message 消息内容，不可为 null
     * @throws NullPointerException 如果 message 为 null
     */
    public SystemMessage {
        Objects.requireNonNull(message, "消息内容不能为 null");
    }

    /**
     * 便捷构造 —— 使用当前时间自动生成时间戳。
     *
     * @param message 消息内容
     * @return 新的 SystemMessage 实例
     */
    public static SystemMessage now(String message) {
        return new SystemMessage(LocalTime.now().format(TIME_FORMATTER), message);
    }

    @Override
    public String toString() {
        return time + "  " + message;
    }
}