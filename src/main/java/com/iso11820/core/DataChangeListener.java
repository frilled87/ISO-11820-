package com.iso11820.core;

/**
 * 数据变更监听器 —— 试验主控制器与 UI 层之间的解耦回调接口。
 * <p>
 * 每次仿真迭代完成后，{@link TestMaster} 在调度线程内同步调用所有已注册的监听器。
 * 核心层不做任何 UI 线程切换，由上层 UI 自行处理跨线程刷新（如 JavaFX 的
 * {@code Platform.runLater()} 或 Swing 的 {@code SwingUtilities.invokeLater()}）。
 * </p>
 *
 * <h3>回调参数</h3>
 * <ul>
 *   <li><b>data</b>：当前 5 通道温度快照（{@link SensorData} 副本）</li>
 *   <li><b>state</b>：当前试验状态</li>
 *   <li><b>recordedDuration</b>：已记录时长（秒），非 RECORDING 状态下为 0</li>
 *   <li><b>latestMessage</b>：本轮迭代产生的最新系统消息内容，无新消息时为 {@code null}</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * testMaster.addDataChangeListener((data, state, duration, msg) -> {
 *     Platform.runLater(() -> {
 *         updateTemperatureDisplay(data);
 *         updateStatusLabel(state);
 *         if (msg != null) appendLog(msg);
 *     });
 * });
 * }</pre>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
@FunctionalInterface
public interface DataChangeListener {

    /**
     * 仿真迭代完成后的回调。
     *
     * @param data             当前温度数据快照（不可变副本）
     * @param state            当前试验状态
     * @param recordedDuration 已记录时长（秒），非 RECORDING 状态下为 0
     * @param latestMessage    本轮迭代产生的最新系统消息，无新消息时为 {@code null}
     */
    void onDataChanged(SensorData data, TestState state, int recordedDuration, String latestMessage);
}