package com.iso11820.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 试验状态机 —— 管理 ISO 11820 试验全生命周期的状态流转。
 * <p>
 * 状态机是试验控制器的核心组件，负责：
 * </p>
 * <ul>
 *   <li>维护当前状态的线程安全变量</li>
 *   <li>校验状态切换是否合法（仅允许文档规定的流转路径）</li>
 *   <li>执行状态切换并通知所有已注册的监听器</li>
 *   <li>处理异常状态回退（READY 温度不达标自动退回 PREPARING）</li>
 * </ul>
 *
 * <h3>状态流转图</h3>
 * <pre>
 *                    ┌──────────────────────────────────┐
 *                    │  (停止加热)                       │
 *                    ▼                                  │
 *   IDLE ──→ PREPARING ──→ READY ──→ RECORDING ──→ COMPLETE
 *    ▲          │             ▲          │                │
 *    │          │ (停止加热)   │ 温度不稳  │                │
 *    │          ▼             │          ▼                │
 *    └──────────┘             └── PREPARING ←────────────┘
 *                                  (无有效样本时)
 *   COMPLETE ──→ PREPARING（保存后保持恒温）
 * </pre>
 *
 * <h3>线程安全</h3>
 * 当前状态使用 {@link AtomicReference} 存储，保证原子性更新。<br>
 * 状态切换的核心逻辑使用 {@code synchronized} 保证 check-and-set 的原子性。<br>
 * 监听器列表使用 {@code synchronized} 包装的不可变副本，防止并发修改异常。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * TestStateMachine sm = new TestStateMachine();
 * sm.setOnStateChanged(event -> {
 *     System.out.println("状态切换: " + event.oldState() + " → " + event.newState());
 * });
 *
 * sm.transitionTo(TestState.PREPARING);  // IDLE → PREPARING ✓
 * sm.transitionTo(TestState.READY);      // PREPARING → READY ✓
 * sm.transitionTo(TestState.RECORDING);  // 非法，当前为 READY 但不满足条件
 * }</pre>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class TestStateMachine {

    private static final Logger log = Logger.getLogger(TestStateMachine.class.getName());

    /**
     * 炉温稳定范围下限（°C），READY 状态要求炉温不低于此值。
     * 对应文档中 "745~755°C" 的稳定判定区间下限。
     */
    public static final double STABLE_TEMP_MIN = 745.0;

    /**
     * 炉温稳定范围上限（°C），READY 状态要求炉温不高于此值。
     * 对应文档中 "745~755°C" 的稳定判定区间上限。
     */
    public static final double STABLE_TEMP_MAX = 755.0;

    // ==================== 核心字段 ====================

    /** 当前状态，使用 AtomicReference 保证线程安全的原子更新 */
    private final AtomicReference<TestState> currentState;

    /** 状态变更监听器列表（线程安全的不可变副本模式） */
    private final List<Consumer<StateChangeEvent>> listeners;

    /** 用于同步监听器列表的锁对象 */
    private final Object listenerLock = new Object();

    // ==================== 构造方法 ====================

    /**
     * 默认构造 —— 初始状态为 {@link TestState#IDLE}。
     */
    public TestStateMachine() {
        this.currentState = new AtomicReference<>(TestState.IDLE);
        this.listeners = new ArrayList<>();
    }

    /**
     * 指定初始状态的构造方法。
     *
     * @param initialState 初始状态，不可为 null
     * @throws NullPointerException 如果 initialState 为 null
     */
    public TestStateMachine(TestState initialState) {
        Objects.requireNonNull(initialState, "初始状态不能为 null");
        this.currentState = new AtomicReference<>(initialState);
        this.listeners = new ArrayList<>();
    }

    // ==================== 状态查询 ====================

    /**
     * 获取当前状态（线程安全）。
     *
     * @return 当前试验状态
     */
    public TestState getCurrentState() {
        return currentState.get();
    }

    /**
     * 判断当前是否处于指定状态。
     *
     * @param state 要判断的状态
     * @return true 如果当前状态等于指定状态
     */
    public boolean isState(TestState state) {
        return currentState.get() == state;
    }

    /**
     * 判断当前是否为可操作的活跃状态（非 IDLE、非 COMPLETE）。
     *
     * @return true 如果当前正在进行试验
     */
    public boolean isActive() {
        TestState state = currentState.get();
        return state != TestState.IDLE && state != TestState.COMPLETE;
    }

    // ==================== 状态切换 ====================

    /**
     * 尝试将状态切换到目标状态。
     * <p>
     * 切换前会校验是否允许从当前状态切换到目标状态（参见 {@link TestState#canTransitionTo(TestState)}）。
     * 校验通过后原子更新状态，并在锁外通知所有监听器。
     * </p>
     *
     * @param target 目标状态，不可为 null
     * @return true 如果切换成功；false 如果目标为 null 或不允许切换
     * @throws NullPointerException 如果 target 为 null
     */
    public boolean transitionTo(TestState target) {
        Objects.requireNonNull(target, "目标状态不能为 null");

        TestState oldState;
        TestState newState;

        synchronized (this) {
            oldState = currentState.get();

            // 相同状态无需切换
            if (oldState == target) {
                log.fine(String.format("状态切换忽略：当前已是 %s，无需切换",
                        target.getDisplayName()));
                return true;
            }

            // 校验是否允许切换
            if (!oldState.canTransitionTo(target)) {
                log.warning(String.format("状态切换拒绝：不允许从 %s 切换到 %s",
                        oldState.getDisplayName(), target.getDisplayName()));
                return false;
            }

            // 原子更新状态
            currentState.set(target);
            newState = target;
        }

        // 在锁外通知监听器，避免死锁
        log.info(String.format("状态切换成功：%s → %s",
                oldState.getDisplayName(), newState.getDisplayName()));
        notifyListeners(new StateChangeEvent(oldState, newState, System.currentTimeMillis()));

        return true;
    }

    /**
     * 强制切换状态（跳过流转规则校验）。
     * <p>
     * ⚠️ <b>仅用于异常恢复场景</b>（如系统重置、紧急停止），
     * 正常业务流程请使用 {@link #transitionTo(TestState)}。
     * </p>
     *
     * @param target 目标状态，不可为 null
     * @throws NullPointerException 如果 target 为 null
     */
    public void forceTransitionTo(TestState target) {
        Objects.requireNonNull(target, "目标状态不能为 null");

        TestState oldState;
        synchronized (this) {
            oldState = currentState.get();
            if (oldState == target) {
                return;
            }
            currentState.set(target);
        }

        log.warning(String.format("强制状态切换：%s → %s",
                oldState.getDisplayName(), target.getDisplayName()));
        notifyListeners(new StateChangeEvent(oldState, target, System.currentTimeMillis()));
    }

    /**
     * 重置状态机到初始状态 {@link TestState#IDLE}。
     * 此为强制操作，不经过流转规则校验。
     */
    public void reset() {
        forceTransitionTo(TestState.IDLE);
    }

    // ==================== 异常回退逻辑 ====================

    /**
     * 检查炉温是否处于稳定范围内（745~755°C）。
     * <p>
     * 用于 READY 状态的持续判定：如果当前状态为 READY 但炉温跌出稳定范围，
     * 调用 {@link #checkAndHandleReadiness(double)} 会自动回退到 PREPARING。
     * </p>
     *
     * @param tf1 当前炉温1的值（°C）
     * @return true 如果炉温在稳定范围内
     */
    public boolean isTemperatureStable(double tf1) {
        return tf1 >= STABLE_TEMP_MIN && tf1 <= STABLE_TEMP_MAX;
    }

    /**
     * READY 状态温度稳定性检查与自动回退。
     * <p>
     * 如果当前状态为 {@link TestState#READY}，且炉温1不在稳定范围（745~755°C）内，
     * 则自动触发状态回退到 {@link TestState#PREPARING}。
     * 该方法应由后台仿真线程在每次温度更新后调用。
     * </p>
     *
     * @param tf1 当前炉温1的值（°C）
     * @return true 如果触发了自动回退；false 如果无需回退或当前不在 READY 状态
     */
    public boolean checkAndHandleReadiness(double tf1) {
        if (currentState.get() != TestState.READY) {
            return false;
        }

        if (!isTemperatureStable(tf1)) {
            log.warning(String.format(
                    "READY 状态温度不达标（炉温1=%.1f°C，要求 %.1f-%.1f°C），自动回退到 PREPARING",
                    tf1, STABLE_TEMP_MIN, STABLE_TEMP_MAX));
            return transitionTo(TestState.PREPARING);
        }

        return false;
    }

    // ==================== 监听器管理 ====================

    /**
     * 注册状态变更监听器。
     * <p>
     * 监听器在状态切换成功后（锁外）被调用，因此监听器内部可以安全地调用
     * 状态机的查询方法而不会死锁。同一个监听器重复注册只会保留一份。
     * </p>
     *
     * @param listener 状态变更回调，不可为 null
     * @throws NullPointerException 如果 listener 为 null
     */
    public void addStateChangeListener(Consumer<StateChangeEvent> listener) {
        Objects.requireNonNull(listener, "监听器不能为 null");
        synchronized (listenerLock) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * 移除已注册的状态变更监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeStateChangeListener(Consumer<StateChangeEvent> listener) {
        synchronized (listenerLock) {
            listeners.remove(listener);
        }
    }

    /**
     * 移除所有已注册的监听器。
     */
    public void clearListeners() {
        synchronized (listenerLock) {
            listeners.clear();
        }
    }

    /**
     * 获取当前注册的监听器数量。
     *
     * @return 监听器数量
     */
    public int getListenerCount() {
        synchronized (listenerLock) {
            return listeners.size();
        }
    }

    /**
     * 通知所有已注册的监听器状态已变更。
     * <p>
     * 使用不可变副本遍历，防止监听器在回调中修改监听器列表导致并发修改异常。
     * 单个监听器抛异常不会影响其他监听器的执行。
     * </p>
     *
     * @param event 状态变更事件
     */
    private void notifyListeners(StateChangeEvent event) {
        List<Consumer<StateChangeEvent>> snapshot;
        synchronized (listenerLock) {
            if (listeners.isEmpty()) {
                return;
            }
            snapshot = List.copyOf(listeners);
        }

        for (Consumer<StateChangeEvent> listener : snapshot) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                log.log(Level.SEVERE, "状态变更监听器执行异常", e);
            }
        }
    }

    // ==================== 内部类 ====================

    /**
     * 状态变更事件 —— 不可变记录，包含状态切换的完整信息。
     * <p>
     * 使用 Java 17 的 {@code record} 类型，自动生成构造器、getter、equals、hashCode、toString。
     * </p>
     *
     * @param oldState  切换前的状态
     * @param newState  切换后的状态
     * @param timestamp 切换时间戳（毫秒，System.currentTimeMillis()）
     */
    public record StateChangeEvent(
            TestState oldState,
            TestState newState,
            long timestamp
    ) {
        /**
         * 判断是否为状态回退（新状态在流转顺序上早于旧状态）。
         *
         * @return true 如果是回退（如 READY → PREPARING、RECORDING → PREPARING）
         */
        public boolean isRollback() {
            return newState.ordinal() < oldState.ordinal();
        }

        /**
         * 判断是否为正常前进（新状态在流转顺序上晚于旧状态）。
         *
         * @return true 如果是正常前进（如 IDLE → PREPARING → READY → RECORDING → COMPLETE）
         */
        public boolean isForward() {
            return newState.ordinal() > oldState.ordinal();
        }

        @Override
        public String toString() {
            return oldState.getDisplayName() + " → " + newState.getDisplayName()
                    + " (timestamp=" + timestamp + ")";
        }
    }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return "TestStateMachine[current=" + currentState.get().getDisplayName()
                + ", listeners=" + getListenerCount() + "]";
    }
}