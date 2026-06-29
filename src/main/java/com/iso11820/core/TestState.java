package com.iso11820.core;

/**
 * 试验状态枚举
 * <p>
 * 定义 ISO 11820 建筑材料不燃性试验仿真系统的 5 个核心状态，
 * 按标准流程顺序流转：IDLE → PREPARING → READY → RECORDING → COMPLETE → PREPARING
 * </p>
 *
 * <h3>状态流转规则</h3>
 * <pre>
 * IDLE（空闲）
 *   ↓ 用户点击「开始升温」
 * PREPARING（升温中）
 *   ↓ 温度达到 745~755°C 且稳定计数器 &gt; 3 次 tick（自动判定）
 *   ↓ 或用户点击「停止加热」→ 回到 IDLE
 * READY（就绪）
 *   ↓ 用户点击「开始记录」
 *   ↓ 或温度跌出稳定范围 → 自动回退到 PREPARING
 *   ↓ 或用户点击「停止加热」→ 回到 IDLE
 * RECORDING（记录中）
 *   ↓ 固定时长到达 / 标准模式到 3600 秒 / 用户手动停止
 *   ↓ 有有效记录 → COMPLETE；无有效记录 → PREPARING
 * COMPLETE（完成）
 *   ↓ UI 保存试验记录后，系统保持恒温 → PREPARING（等待下次试验）
 * </pre>
 *
 * <h3>线程安全</h3>
 * 枚举本身是不可变的，多线程安全。状态切换的原子性由
 * {@link TestStateMachine} 中的 {@link java.util.concurrent.atomic.AtomicReference} 保证。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public enum TestState {

    /** 空闲 —— 初始状态，等待用户新建试验并开始升温 */
    IDLE("空闲"),

    /** 升温中 —— 炉温正在从室温/初始温度升至目标温度 750°C */
    PREPARING("升温中"),

    /** 就绪 —— 温度已稳定在 745~755°C 区间，可以开始记录数据 */
    READY("就绪"),

    /** 记录中 —— 正在逐秒记录 5 通道温度数据，计时器运行中 */
    RECORDING("记录中"),

    /** 完成 —— 试验记录阶段结束，等待用户保存现象记录和试验后质量 */
    COMPLETE("完成");

    /** 中文显示名称，用于 UI 界面展示 */
    private final String displayName;

    /**
     * 构造状态枚举实例
     *
     * @param displayName 中文显示名称
     */
    TestState(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取状态的中文显示名称
     *
     * @return 中文名称，如 "空闲"、"升温中" 等
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 判断从当前状态是否允许切换到目标状态。
     * <p>
     * 仅允许文档规定的状态流转路径：
     * </p>
     * <ul>
     *   <li>IDLE → PREPARING（开始升温）</li>
     *   <li>PREPARING → READY（温度稳定，自动）| IDLE（停止加热，手动）</li>
     *   <li>READY → RECORDING（开始记录）| PREPARING（温度不稳，自动回退）| IDLE（停止加热）</li>
     *   <li>RECORDING → COMPLETE（试验完成）| PREPARING（无有效记录样本时回退）</li>
     *   <li>COMPLETE → PREPARING（保存完成，保持恒温等待下次试验）</li>
     * </ul>
     *
     * @param target 目标状态
     * @return {@code true} 如果允许切换；{@code false} 如果目标为 null 或不允许切换
     */
    public boolean canTransitionTo(TestState target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case IDLE      -> target == PREPARING;
            case PREPARING -> target == READY || target == IDLE;
            case READY     -> target == RECORDING || target == PREPARING || target == IDLE;
            case RECORDING -> target == COMPLETE || target == PREPARING;
            case COMPLETE  -> target == PREPARING;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}