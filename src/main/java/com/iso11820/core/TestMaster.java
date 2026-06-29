package com.iso11820.core;

import com.iso11820.core.simulation.SensorSimulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 试验主控制器 —— 协调状态机、仿真引擎、时序数据缓存和系统消息的顶层组件。
 * <p>
 * TestMaster 是 ISO 11820 仿真系统的核心业务编排器，负责：
 * </p>
 * <ul>
 *   <li>持有并协调 {@link TestStateMachine}（状态机）和 {@link SensorSimulator}（仿真引擎）</li>
 *   <li>通过单线程 {@link ScheduledExecutorService} 每 800ms 驱动一次仿真迭代</li>
 *   <li>在记录阶段每秒追加一条温度数据到线程安全的 {@link CopyOnWriteArrayList}</li>
 *   <li>自动检测温度稳定条件并触发状态切换（PREPARING→READY）</li>
 *   <li>自动检测温度异常并触发状态回退（READY→PREPARING）</li>
 *   <li>到达设定记录时长后自动停止试验（RECORDING→COMPLETE）</li>
 *   <li>试验完成时自动计算各通道最大值、出现时间、温升、平均温度</li>
 *   <li>通过 {@link DataChangeListener} 向 UI 层广播每次迭代的最新数据</li>
 * </ul>
 *
 * <h3>线程安全设计</h3>
 * <ul>
 *   <li>控制方法（startHeating/stopRecording 等）与调度任务（onTick）通过 {@code tickLock} 互斥</li>
 *   <li>监听器在锁外调用，避免死锁</li>
 *   <li>时序数据使用 {@link CopyOnWriteArrayList} 保证读写安全</li>
 *   <li>统计数据使用 {@code volatile} 保证可见性</li>
 * </ul>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 * TestMaster master = new TestMaster();
 * master.addDataChangeListener((data, state, duration, msg) -> {
 *     // UI 更新逻辑
 * });
 *
 * master.startHeating();           // IDLE → PREPARING
 * // ... 等待温度稳定，自动进入 READY ...
 * master.startRecording(3600);     // READY → RECORDING（标准 60 分钟）
 * // ... 到达 3600 秒自动完成，或调用 stopRecording() 手动停止 ...
 * master.stopHeating();            // 停止加热，进入 IDLE
 * }</pre>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class TestMaster {

    private static final Logger log = Logger.getLogger(TestMaster.class.getName());

    // ==================== 常量 ====================

    /** 调度器执行间隔（毫秒），对应硬件 800ms 采集周期 */
    static final long TICK_INTERVAL_MS = 800L;

    /** 默认记录时长（秒），标准 60 分钟试验 */
    public static final int STANDARD_DURATION_SECONDS = 3600;

    // ==================== 内部组件 ====================

    /** 状态机 —— 管理试验状态流转与校验 */
    private final TestStateMachine stateMachine;

    /** 仿真引擎 —— 按算法生成 5 通道温度数据 */
    private final SensorSimulator simulator;

    // ==================== 调度器 ====================

    /** 单线程定时调度器，每 800ms 执行一次仿真迭代 */
    private ScheduledExecutorService scheduler;

    /** 调度任务的 Future，用于取消 */
    private ScheduledFuture<?> scheduledFuture;

    /** 调度器是否正在运行 */
    private final AtomicBoolean schedulerRunning = new AtomicBoolean(false);

    // ==================== 线程同步 ====================

    /** 锁对象 —— 保证控制方法与 onTick 之间的互斥 */
    private final Object tickLock = new Object();

    // ==================== 时序数据缓存 ====================

    /** 记录阶段每秒追加一条温度快照，线程安全 */
    private final CopyOnWriteArrayList<SensorData> recordedData;

    // ==================== 系统消息队列 ====================

    /** 系统消息队列，存储所有状态变更和业务提示日志 */
    private final CopyOnWriteArrayList<SystemMessage> messages;

    /** 本轮迭代产生的最新消息内容（用于监听器回调），无新消息时为 null */
    private volatile String latestMessage;

    // ==================== 记录状态 ====================

    /** 目标记录时长（秒），由 {@link #startRecording(int)} 设置 */
    private volatile int targetDurationSeconds = STANDARD_DURATION_SECONDS;

    /** 已记录时长（秒），实时更新 */
    private volatile int recordedDuration = 0;

    /** 记录开始时刻（毫秒时间戳），用于计算已记录时长 */
    private volatile long recordingStartTimeMs = 0;

    /** 上次追加数据的秒序号，用于去重（每秒只追加一条） */
    private int lastAppendedSecond = -1;

    // ==================== 统计数据 ====================

    // --- 最大值及出现时间 ---
    private volatile double maxTf1, maxTf2, maxTs, maxTc;
    private volatile int maxTf1Time, maxTf2Time, maxTsTime, maxTcTime;

    // --- 最终值 ---
    private volatile double finalTf1, finalTf2, finalTs, finalTc;

    // --- 温升（最终值 - 初始值）---
    private volatile double deltaTf1, deltaTf2, deltaTs, deltaTc, deltaTf;

    // --- 平均值 ---
    private volatile double avgTf1, avgTf2, avgTs, avgTc;

    // --- 运行中的累加器（非 volatile，仅在 synchronized 块内访问）---
    private double sumTf1, sumTf2, sumTs, sumTc;
    private int dataPointCount;

    // --- 最小值（ISO 11820 统计补全）---
    private volatile double minTf1 = Double.MAX_VALUE, minTf2 = Double.MAX_VALUE,
                           minTs = Double.MAX_VALUE, minTc = Double.MAX_VALUE;

    // ==================== 保存状态保护（ISO 11820 业务规则） ====================

    /**
     * 试验保存状态标记。
     * 空字符串表示未保存，"10000000" 表示试验记录已保存（对应数据库 testmaster.flag 字段）。
     * ISO 11820: 已保存的试验允许覆盖/新建，未保存的完成试验禁止被覆盖。
     */
    private volatile String saveFlag = "";

    // ==================== 恒功率值（ISO 11820 业务规则） ====================

    /** 功率采样队列，READY 状态下每次迭代采集。最大容量 600（约 8 分钟） */
    private final ArrayList<Double> powerSamples = new ArrayList<>(600);

    /** 本次试验的恒功率基准值（kW），保留 1 位小数 */
    private volatile double constantPower = 0;

    // ==================== 温漂校验（ISO 11820 提前终止条件） ====================

    /** 上次温漂校验的时刻（分钟），用于每 5 分钟触发一次校验 */
    private int lastDriftCheckMinute = -1;

    /** 温漂计算结果（°C/10min），保留 2 位小数 */
    private volatile double tempDrift = Double.MAX_VALUE;

    // ==================== 监听器 ====================

    /** 数据变更监听器列表 */
    private final List<DataChangeListener> listeners;

    /** 监听器列表的同步锁 */
    private final Object listenerLock = new Object();

    // ==================== 构造方法 ====================

    /**
     * 默认构造 —— 使用默认仿真参数创建试验主控制器。
     * <p>
     * 仿真参数：目标炉温 750°C、升温速率 40°C/s、温度波动 0.5°C、
     * 稳定阈值 3°C、初始炉温 25°C。初始状态为 {@link TestState#IDLE}。
     * </p>
     */
    public TestMaster() {
        this.stateMachine = new TestStateMachine();
        this.simulator = new SensorSimulator();
        this.recordedData = new CopyOnWriteArrayList<>();
        this.messages = new CopyOnWriteArrayList<>();
        this.listeners = new ArrayList<>();
        resetStatistics();
    }

    /**
     * 指定随机种子的构造方法 —— 用于单元测试获得可重现的仿真结果。
     *
     * @param seed 仿真引擎随机种子
     */
    public TestMaster(long seed) {
        this.stateMachine = new TestStateMachine();
        this.simulator = new SensorSimulator(seed);
        this.recordedData = new CopyOnWriteArrayList<>();
        this.messages = new CopyOnWriteArrayList<>();
        this.listeners = new ArrayList<>();
        resetStatistics();
    }

    // ==================== 对外控制方法 ====================

    /**
     * 开始升温 —— 从 {@link TestState#IDLE} 进入 {@link TestState#PREPARING}。
     * <p>
     * 此方法会：
     * </p>
     * <ol>
     *   <li>校验当前状态是否为 IDLE</li>
     *   <li>通过状态机切换到 PREPARING</li>
     *   <li>将仿真引擎设置为 PREPARING 状态</li>
     *   <li>创建单线程调度器，每 800ms 执行一次仿真迭代</li>
     *   <li>生成系统消息 "开始升温，系统升温中"</li>
     * </ol>
     *
     * <h3>幂等性</h3>
     * 如果当前状态不是 IDLE 或调度器已在运行，返回 {@code false} 且不抛异常。
     *
     * @return {@code true} 如果成功启动升温；{@code false} 如果当前状态不允许
     */
    public boolean startHeating() {
        synchronized (tickLock) {
            // ISO 11820: 保存状态保护 —— 试验已完成但未保存，禁止新建试验
            if (stateMachine.getCurrentState() == TestState.COMPLETE && !isSaved()) {
                log.warning("startHeating 拒绝：试验已完成但未保存，请先保存试验记录");
                addMessage("试验已完成但未保存，请先保存试验记录");
                return false;
            }

            // 幂等检查：必须处于 IDLE 状态
            if (stateMachine.getCurrentState() != TestState.IDLE) {
                log.fine(() -> String.format("startHeating 拒绝：当前状态=%s，需要 IDLE",
                        stateMachine.getCurrentState().getDisplayName()));
                return false;
            }

            // 调度器已运行则拒绝
            if (schedulerRunning.get()) {
                log.warning("startHeating 拒绝：调度器已在运行中");
                return false;
            }

            // 状态切换：IDLE → PREPARING
            if (!stateMachine.transitionTo(TestState.PREPARING)) {
                log.warning("startHeating 失败：状态机切换 IDLE→PREPARING 被拒绝");
                return false;
            }

            // 仿真引擎设置为升温状态
            simulator.setCurrentState(TestState.PREPARING);

            // 创建单线程调度器（守护线程，避免阻止 JVM 退出）
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TestMaster-Scheduler");
                t.setDaemon(true);
                return t;
            });

            // 启动定时调度：初始延迟 0，每 800ms 执行一次
            scheduledFuture = scheduler.scheduleAtFixedRate(
                    this::onTick, 0, TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
            schedulerRunning.set(true);

            // 生成系统消息
            addMessage("开始升温，系统升温中");
            log.info("升温已启动，调度器运行中（间隔=" + TICK_INTERVAL_MS + "ms）");
        }

        // 锁外通知监听器
        notifyListeners(getCurrentData(), TestState.PREPARING, 0, latestMessage);
        return true;
    }

    /**
     * 开始记录 —— 从 {@link TestState#READY} 进入 {@link TestState#RECORDING}。
     * <p>
     * 此方法会：
     * </p>
     * <ol>
     *   <li>校验当前状态是否为 READY</li>
     *   <li>设置目标记录时长（秒）</li>
     *   <li>通过状态机切换到 RECORDING</li>
     *   <li>将仿真引擎设置为 RECORDING 状态</li>
     *   <li>记录开始时刻，重置统计数据累加器</li>
     *   <li>生成系统消息 "开始记录，计时开始"</li>
     * </ol>
     *
     * <h3>幂等性</h3>
     * 如果当前状态不是 READY，返回 {@code false} 且不抛异常。
     *
     * @param durationSeconds 目标记录时长（秒），标准 60 分钟试验传 {@value #STANDARD_DURATION_SECONDS}
     * @return {@code true} 如果成功开始记录；{@code false} 如果当前状态不允许
     * @throws IllegalArgumentException 如果 durationSeconds &lt;= 0
     */
    public boolean startRecording(int durationSeconds) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("记录时长必须大于 0 秒，实际: " + durationSeconds);
        }

        synchronized (tickLock) {
            // ISO 11820: 保存状态保护 —— 试验已完成但未保存，禁止开始新记录
            if (stateMachine.getCurrentState() == TestState.COMPLETE && !isSaved()) {
                log.warning("startRecording 拒绝：试验已完成但未保存，请先保存试验记录");
                addMessage("试验已完成但未保存，请先保存试验记录");
                return false;
            }

            // 幂等检查：必须处于 READY 状态
            if (stateMachine.getCurrentState() != TestState.READY) {
                log.fine(() -> String.format("startRecording 拒绝：当前状态=%s，需要 READY",
                        stateMachine.getCurrentState().getDisplayName()));
                return false;
            }

            // ISO 11820: 恒功率值计算 —— 计算 READY 阶段功率采样队列的平均值
            computeConstantPower();
            // 清空功率采样队列，准备下一轮采样
            powerSamples.clear();

            // 设置目标时长
            this.targetDurationSeconds = durationSeconds;

            // 状态切换：READY → RECORDING
            if (!stateMachine.transitionTo(TestState.RECORDING)) {
                log.warning("startRecording 失败：状态机切换 READY→RECORDING 被拒绝");
                return false;
            }

            // 仿真引擎设置为记录状态
            simulator.setCurrentState(TestState.RECORDING);

            // 记录开始时刻
            recordingStartTimeMs = System.currentTimeMillis();
            recordedDuration = 0;
            lastAppendedSecond = -1;
            lastDriftCheckMinute = -1; // ISO 11820: 重置温漂校验时刻

            // 清空上一次试验的时序数据
            recordedData.clear();

            // 重置统计数据累加器
            resetStatistics();

            // 生成系统消息
            addMessage("开始记录，计时开始");
            log.info(() -> String.format("记录已开始，目标时长=%d秒", durationSeconds));
        }

        // 锁外通知监听器
        notifyListeners(getCurrentData(), TestState.RECORDING, 0, latestMessage);
        return true;
    }

    /**
     * 手动停止记录 —— 从 {@link TestState#RECORDING} 切换到 {@link TestState#COMPLETE}。
     * <p>
     * 此方法会：
     * </p>
     * <ol>
     *   <li>校验当前状态是否为 RECORDING</li>
     *   <li>通过状态机切换到 COMPLETE</li>
     *   <li>将仿真引擎设置为 COMPLETE 状态</li>
     *   <li>计算最终统计值（最大值、温升、平均温度）</li>
     *   <li>生成系统消息 "用户手动停止记录"</li>
     * </ol>
     *
     * <h3>幂等性</h3>
     * 如果当前状态不是 RECORDING，返回 {@code false} 且不抛异常。
     *
     * @return {@code true} 如果成功停止记录；{@code false} 如果当前状态不允许
     */
    public boolean stopRecording() {
        synchronized (tickLock) {
            // 幂等检查：必须处于 RECORDING 状态
            if (stateMachine.getCurrentState() != TestState.RECORDING) {
                log.fine(() -> String.format("stopRecording 拒绝：当前状态=%s，需要 RECORDING",
                        stateMachine.getCurrentState().getDisplayName()));
                return false;
            }

            // ISO 11820 业务规则：有效数据判定 —— 累计记录时长 >= 30 秒
            if (recordedDuration < 30) {
                log.warning(() -> String.format("记录时长不足 30 秒（实际=%d秒），视为无效试验，退回 READY",
                        recordedDuration));
                recordedData.clear();
                resetStatistics();
                recordedDuration = 0;
                lastAppendedSecond = -1;
                lastDriftCheckMinute = -1;
                stateMachine.forceTransitionTo(TestState.READY);
                simulator.setCurrentState(TestState.READY);
                addMessage("记录时长不足30秒，试验无效，退回就绪状态");
                return false;
            }

            // 状态切换：RECORDING → COMPLETE
            if (!stateMachine.transitionTo(TestState.COMPLETE)) {
                log.warning("stopRecording 失败：状态机切换 RECORDING→COMPLETE 被拒绝");
                return false;
            }

            // 仿真引擎设置为完成状态
            simulator.setCurrentState(TestState.COMPLETE);

            // 计算最终统计值（含温漂）
            calculateFinalStatistics();

            // 生成系统消息
            addMessage("用户手动停止记录");
            log.info(() -> String.format("记录已手动停止，总时长=%d秒", recordedDuration));
        }

        // 锁外通知监听器
        notifyListeners(getCurrentData(), TestState.COMPLETE, recordedDuration, latestMessage);
        return true;
    }

    /**
     * 停止加热 —— 关闭调度器，强制进入 {@link TestState#IDLE}，触发降温逻辑。
     * <p>
     * 此方法会：
     * </p>
     * <ol>
     *   <li>标记调度器停止</li>
     *   <li>取消调度任务并关闭线程池（优雅关闭，等待最多 2 秒）</li>
     *   <li>强制状态机切换到 IDLE</li>
     *   <li>将仿真引擎设置为 IDLE 状态（触发降温算法）</li>
     *   <li>生成系统消息 "停止加热"</li>
     * </ol>
     *
     * <h3>幂等性</h3>
     * 可从任意状态调用，不抛异常。如果调度器已停止则跳过关闭步骤。
     */
    public void stopHeating() {
        synchronized (tickLock) {
            // 关闭调度器
            shutdownScheduler();

            // ISO 11820: 清空功率采样队列
            powerSamples.clear();

            // 强制切换到 IDLE
            if (stateMachine.getCurrentState() != TestState.IDLE) {
                stateMachine.forceTransitionTo(TestState.IDLE);
            }

            // 仿真引擎设置为 IDLE（触发降温算法）
            simulator.setCurrentState(TestState.IDLE);

            // 生成系统消息
            addMessage("停止加热");
            log.info("加热已停止，进入 IDLE 状态");
        }

        // 锁外通知监听器
        notifyListeners(getCurrentData(), TestState.IDLE, 0, latestMessage);
    }

    /**
     * 重置全部状态 —— 清空所有缓存、重置仿真器和状态机。
     * <p>
     * 此方法会：
     * </p>
     * <ol>
     *   <li>关闭调度器</li>
     *   <li>清空时序数据缓存和消息队列</li>
     *   <li>重置统计数据</li>
     *   <li>重置仿真引擎到初始温度</li>
     *   <li>重置状态机到 IDLE</li>
     * </ol>
     *
     * <h3>幂等性</h3>
     * 可从任意状态调用，不抛异常。
     */
    public void reset() {
        synchronized (tickLock) {
            // ISO 11820: 保存状态保护 —— 试验已完成但未保存，禁止重置
            if (stateMachine.getCurrentState() == TestState.COMPLETE && !isSaved()) {
                log.warning("reset 拒绝：试验已完成但未保存，请先保存试验记录");
                addMessage("试验已完成但未保存，请先保存试验记录再重置");
                return;
            }

            // 关闭调度器
            shutdownScheduler();

            // ISO 11820: 清空保存状态标记
            saveFlag = "";

            // ISO 11820: 清空功率采样队列
            powerSamples.clear();
            constantPower = 0;

            // 清空时序数据
            recordedData.clear();

            // 清空消息队列
            messages.clear();
            latestMessage = null;

            // 重置记录状态
            recordedDuration = 0;
            targetDurationSeconds = STANDARD_DURATION_SECONDS;
            recordingStartTimeMs = 0;
            lastAppendedSecond = -1;

            // 重置统计数据
            resetStatistics();

            // 重置仿真引擎
            simulator.reset();
            simulator.setCurrentState(TestState.IDLE);

            // 重置状态机
            stateMachine.reset();

            addMessage("系统已重置");
            log.info("TestMaster 已完全重置");
        }

        // 锁外通知监听器
        notifyListeners(getCurrentData(), TestState.IDLE, 0, latestMessage);
    }

    // ==================== 调度器管理 ====================

    /**
     * 关闭调度器（优雅关闭，等待最多 2 秒）。
     * <p>
     * 线程安全：仅在持有 {@link #tickLock} 时调用。
     * </p>
     */
    private void shutdownScheduler() {
        schedulerRunning.set(false);

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    log.warning("调度器未能在 2 秒内优雅关闭，已强制终止");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                log.warning("等待调度器关闭时被中断，已强制终止");
            }
            scheduler = null;
        }
    }

    // ==================== 核心仿真循环 ====================

    /**
     * 每次调度器触发的仿真迭代（每 800ms 一次）。
     * <p>
     * 在 {@link #tickLock} 保护下执行，确保与控制方法互斥。
     * 执行流程：
     * </p>
     * <ol>
     *   <li>调用仿真引擎 {@link SensorSimulator#update()} 生成新温度数据</li>
     *   <li>根据当前状态执行对应的业务逻辑（自动切换、数据追加、统计更新）</li>
     *   <li>锁外通知所有监听器</li>
     * </ol>
     */
    private void onTick() {
        SensorData data;
        TestState state;
        int duration;
        String msg;

        synchronized (tickLock) {
            // 调度器已停止，跳过本轮
            if (!schedulerRunning.get()) {
                return;
            }

            // 1. 执行一次仿真迭代
            data = simulator.update();

            // 2. 根据当前状态执行对应业务逻辑
            latestMessage = null;
            TestState currentState = stateMachine.getCurrentState();

            switch (currentState) {
                case PREPARING -> handlePreparingTick(data);
                case READY      -> handleReadyTick(data);
                case RECORDING  -> handleRecordingTick(data);
                case COMPLETE   -> { /* 完成状态，无需额外处理 */ }
                case IDLE       -> { /* 空闲/降温状态，无需额外处理 */ }
            }

            state = stateMachine.getCurrentState();
            duration = recordedDuration;
            msg = latestMessage;
        }

        // 3. 锁外通知监听器（避免死锁）
        notifyListeners(data, state, duration, msg);
    }

    // ==================== 各阶段业务逻辑 ====================

    /**
     * PREPARING 阶段的迭代逻辑。
     * <p>
     * 自动检测温度稳定条件：当 {@link SensorSimulator#isStable()} 返回 {@code true}
     * 且炉温在 745~755°C 范围内时，自动切换到 READY 状态。
     * </p>
     *
     * @param data 本轮迭代的温度数据
     */
    private void handlePreparingTick(SensorData data) {
        // 检查温度是否稳定达标
        if (simulator.isStable()) {
            if (stateMachine.transitionTo(TestState.READY)) {
                addMessage("温度已稳定，可以开始记录");
                log.info("温度稳定达标，自动切换 PREPARING→READY");
            }
        }
    }

    /**
     * READY 阶段的迭代逻辑。
     * <p>
     * 自动检测温度异常：当炉温1 跌出 745~755°C 稳定范围时，
     * 通过状态机自动回退到 PREPARING。
     * </p>
     *
     * @param data 本轮迭代的温度数据
     */
    private void handleReadyTick(SensorData data) {
        // ISO 11820: 恒功率采样 —— READY 状态下每次迭代采集功率值
        samplePower(data.getTf1());

        // ISO 11820: 炉温稳定范围校验 —— 745°C ~ 755°C
        // 检查温度是否跌出稳定范围（状态机内部处理回退逻辑）
        if (stateMachine.checkAndHandleReadiness(data.getTf1())) {
            String msg = String.format("温度跌出稳定范围（炉温1=%.1f°C），自动退回升温状态",
                    data.getTf1());
            addMessage(msg);
            log.warning(msg);
        }
    }

    /**
     * RECORDING 阶段的迭代逻辑。
     * <p>
     * 每秒追加一条温度数据到 {@link #recordedData}，实时更新统计数据，
     * 到达目标记录时长后自动切换到 COMPLETE。
     * </p>
     *
     * @param data 本轮迭代的温度数据
     */
    private void handleRecordingTick(SensorData data) {
        // 更新已记录时长
        long elapsed = System.currentTimeMillis() - recordingStartTimeMs;
        recordedDuration = (int) (elapsed / 1000L);

        // 每秒追加一条数据（去重：同一秒内只追加一条）
        int currentSecond = recordedDuration;
        if (currentSecond > lastAppendedSecond) {
            recordedData.add(new SensorData(data)); // 存储副本
            lastAppendedSecond = currentSecond;
            updateIncrementalStatistics(data, currentSecond);
        }

        // ISO 11820: 强制终止 —— 累计记录时长达到 60 分钟（3600 秒）无条件终止
        if (recordedDuration >= 3600) {
            autoComplete();
            tempDrift = calcTempDrift(); // 最终温漂记录
            latestMessage = "记录时间到达 3600 秒，试验自动结束";
            addMessage(latestMessage);
            log.info("ISO 11820 强制终止：记录时间到达 3600 秒");
            return;
        }

        // 到达自定义目标时长 → 自动停止
        if (recordedDuration >= targetDurationSeconds) {
            autoComplete();
            tempDrift = calcTempDrift();
            latestMessage = "记录时间到达 " + targetDurationSeconds + " 秒，试验自动结束";
            addMessage(latestMessage);
            log.info(() -> "记录时间到达 " + targetDurationSeconds + " 秒，自动停止");
            return;
        }

        // ISO 11820: 提前终止条件 —— 每 5 分钟校验一次温漂
        int currentMinute = recordedDuration / 60;
        if (currentMinute >= 10 && currentMinute % 5 == 0 && currentMinute > lastDriftCheckMinute) {
            lastDriftCheckMinute = currentMinute;
            double drift = calcTempDrift();
            // ISO 11820: 温漂 < 2°C/10min → 满足提前终止条件
            if (drift != Double.MAX_VALUE && Math.abs(drift) < 2.0) {
                tempDrift = drift;
                autoComplete();
                String msg = String.format(
                        "满足终止条件（温漂 %.2f ℃/10min < 2.0 ℃/10min），试验提前结束",
                        drift);
                latestMessage = msg;
                addMessage(msg);
                log.info(() -> String.format("ISO 11820 提前终止：温漂=%.2f ℃/10min", drift));
            }
        }
    }

    // ==================== 自动完成逻辑 ====================

    /**
     * 自动完成试验 —— 从 RECORDING 切换到 COMPLETE 并计算统计值。
     * <p>
     * 仅在持有 {@link #tickLock} 时调用。
     * </p>
     */
    private void autoComplete() {
        if (!stateMachine.transitionTo(TestState.COMPLETE)) {
            log.warning("autoComplete 失败：状态机切换 RECORDING→COMPLETE 被拒绝");
            return;
        }
        simulator.setCurrentState(TestState.COMPLETE);
        tempDrift = calcTempDrift(); // ISO 11820: 记录最终温漂值
        calculateFinalStatistics();
    }

    // ==================== 统计计算 ====================

    /**
     * 重置所有统计数据为初始值。
     */
    private void resetStatistics() {
        maxTf1 = maxTf2 = maxTs = maxTc = 0;
        maxTf1Time = maxTf2Time = maxTsTime = maxTcTime = 0;
        minTf1 = minTf2 = minTs = minTc = Double.MAX_VALUE; // ISO 11820: 最小值初始化为极大值
        finalTf1 = finalTf2 = finalTs = finalTc = 0;
        deltaTf1 = deltaTf2 = deltaTs = deltaTc = deltaTf = 0;
        avgTf1 = avgTf2 = avgTs = avgTc = 0;
        sumTf1 = sumTf2 = sumTs = sumTc = 0;
        dataPointCount = 0;
        tempDrift = Double.MAX_VALUE; // ISO 11820: 温漂初始值
    }

    /**
     * 记录阶段每追加一条数据时，增量更新统计数据。
     * <p>
     * 仅在持有 {@link #tickLock} 时调用。
     * </p>
     *
     * @param data   温度数据快照
     * @param second 当前秒序号
     */
    private void updateIncrementalStatistics(SensorData data, int second) {
        // 最大值及出现时间
        if (data.getTf1() > maxTf1) { maxTf1 = data.getTf1(); maxTf1Time = second; }
        if (data.getTf2() > maxTf2) { maxTf2 = data.getTf2(); maxTf2Time = second; }
        if (data.getTs()  > maxTs)  { maxTs  = data.getTs();  maxTsTime  = second; }
        if (data.getTc()  > maxTc)  { maxTc  = data.getTc();  maxTcTime  = second; }

        // ISO 11820: 最小值
        if (data.getTf1() < minTf1) { minTf1 = data.getTf1(); }
        if (data.getTf2() < minTf2) { minTf2 = data.getTf2(); }
        if (data.getTs()  < minTs)  { minTs  = data.getTs(); }
        if (data.getTc()  < minTc)  { minTc  = data.getTc(); }

        // 累加用于平均值计算
        sumTf1 += data.getTf1();
        sumTf2 += data.getTf2();
        sumTs  += data.getTs();
        sumTc  += data.getTc();
        dataPointCount++;
    }

    /**
     * 试验完成时计算最终统计值（最终值、温升、平均值）。
     * <p>
     * 仅在持有 {@link #tickLock} 时调用。
     * </p>
     */
    private void calculateFinalStatistics() {
        if (recordedData.isEmpty()) {
            log.warning("无记录数据，跳过统计计算");
            return;
        }

        SensorData last = recordedData.get(recordedData.size() - 1);
        SensorData first = recordedData.get(0);

        // 最终值
        finalTf1 = last.getTf1();
        finalTf2 = last.getTf2();
        finalTs  = last.getTs();
        finalTc  = last.getTc();

        // 温升（最终值 - 初始值）
        deltaTf1 = finalTf1 - first.getTf1();
        deltaTf2 = finalTf2 - first.getTf2();
        deltaTs  = finalTs  - first.getTs();
        deltaTc  = finalTc  - first.getTc();
        deltaTf  = deltaTs; // 文档规定：综合温升取表面温升

        // 平均值
        if (dataPointCount > 0) {
            avgTf1 = roundOneDecimal(sumTf1 / dataPointCount);
            avgTf2 = roundOneDecimal(sumTf2 / dataPointCount);
            avgTs  = roundOneDecimal(sumTs  / dataPointCount);
            avgTc  = roundOneDecimal(sumTc  / dataPointCount);
        }

        log.info(() -> String.format(
                "统计计算完成：maxTf1=%.1f°C@%ds, deltaTf=%.1f°C, avgTf1=%.1f°C, 数据点=%d",
                maxTf1, maxTf1Time, deltaTf, avgTf1, dataPointCount));
    }

    /** 四舍五入到 1 位小数 */
    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // ==================== 消息管理 ====================

    /**
     * 添加一条系统消息到消息队列，并设置为最新消息。
     *
     * @param msg 消息内容
     */
    private void addMessage(String msg) {
        SystemMessage systemMsg = SystemMessage.now(msg);
        messages.add(systemMsg);
        latestMessage = msg;
        log.fine(() -> "系统消息: " + systemMsg);
    }

    // ==================== 监听器管理 ====================

    /**
     * 注册数据变更监听器。
     * <p>
     * 监听器在每次仿真迭代完成后（锁外）被同步调用。
     * 同一个监听器重复注册只会保留一份。
     * </p>
     *
     * @param listener 数据变更回调，不可为 null
     * @throws NullPointerException 如果 listener 为 null
     */
    public void addDataChangeListener(DataChangeListener listener) {
        Objects.requireNonNull(listener, "监听器不能为 null");
        synchronized (listenerLock) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * 移除已注册的数据变更监听器。
     *
     * @param listener 要移除的监听器
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        synchronized (listenerLock) {
            listeners.remove(listener);
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
     * 通知所有已注册的监听器。
     * <p>
     * 使用不可变副本遍历，防止监听器回调中修改列表导致并发修改异常。
     * 单个监听器抛异常不会影响其他监听器的执行。
     * </p>
     *
     * @param data             当前温度数据
     * @param state            当前状态
     * @param recordedDuration 已记录时长
     * @param latestMessage    最新消息
     */
    private void notifyListeners(SensorData data, TestState state,
                                  int recordedDuration, String latestMessage) {
        List<DataChangeListener> snapshot;
        synchronized (listenerLock) {
            if (listeners.isEmpty()) {
                return;
            }
            snapshot = List.copyOf(listeners);
        }

        for (DataChangeListener listener : snapshot) {
            try {
                listener.onDataChanged(data, state, recordedDuration, latestMessage);
            } catch (Exception e) {
                log.log(Level.SEVERE, "数据变更监听器执行异常", e);
            }
        }
    }

    // ==================== 公开查询方法 ====================

    /** @return 当前试验状态 */
    public TestState getCurrentState() {
        return stateMachine.getCurrentState();
    }

    /** @return 当前温度数据的只读副本 */
    public SensorData getCurrentData() {
        return simulator.getCurrentData();
    }

    /** @return 记录阶段采集的时序温度数据（不可变列表视图） */
    public List<SensorData> getRecordedData() {
        return List.copyOf(recordedData);
    }

    /** @return 记录阶段采集的数据点数 */
    public int getRecordedDataCount() {
        return recordedData.size();
    }

    /** @return 已记录时长（秒），非 RECORDING 状态下为 0 */
    public int getRecordedDuration() {
        return recordedDuration;
    }

    /** @return 目标记录时长（秒） */
    public int getTargetDurationSeconds() {
        return targetDurationSeconds;
    }

    /** @return 系统消息队列（不可变列表视图） */
    public List<SystemMessage> getMessages() {
        return List.copyOf(messages);
    }

    /** @return 系统消息数量 */
    public int getMessageCount() {
        return messages.size();
    }

    /** @return 最新一条系统消息内容，无消息时为 null */
    public String getLatestMessage() {
        return latestMessage;
    }

    /** @return 调度器是否正在运行 */
    public boolean isSchedulerRunning() {
        return schedulerRunning.get();
    }

    // --- 统计数据 Getter ---

    /** @return 炉温1 最大值（°C） */
    public double getMaxTf1() { return maxTf1; }
    /** @return 炉温2 最大值（°C） */
    public double getMaxTf2() { return maxTf2; }
    /** @return 表面温 最大值（°C） */
    public double getMaxTs()  { return maxTs; }
    /** @return 中心温 最大值（°C） */
    public double getMaxTc()  { return maxTc; }

    /** @return 炉温1 最大值出现时间（秒） */
    public int getMaxTf1Time() { return maxTf1Time; }
    /** @return 炉温2 最大值出现时间（秒） */
    public int getMaxTf2Time() { return maxTf2Time; }
    /** @return 表面温 最大值出现时间（秒） */
    public int getMaxTsTime()  { return maxTsTime; }
    /** @return 中心温 最大值出现时间（秒） */
    public int getMaxTcTime()  { return maxTcTime; }

    /** @return 炉温1 最终值（°C） */
    public double getFinalTf1() { return finalTf1; }
    /** @return 炉温2 最终值（°C） */
    public double getFinalTf2() { return finalTf2; }
    /** @return 表面温 最终值（°C） */
    public double getFinalTs()  { return finalTs; }
    /** @return 中心温 最终值（°C） */
    public double getFinalTc()  { return finalTc; }

    /** @return 炉温1 温升（°C） */
    public double getDeltaTf1() { return deltaTf1; }
    /** @return 炉温2 温升（°C） */
    public double getDeltaTf2() { return deltaTf2; }
    /** @return 表面温 温升（°C） */
    public double getDeltaTs()  { return deltaTs; }
    /** @return 中心温 温升（°C） */
    public double getDeltaTc()  { return deltaTc; }
    /** @return 综合温升（°C），当前取表面温升 */
    public double getDeltaTf()  { return deltaTf; }

    /** @return 炉温1 平均值（°C） */
    public double getAvgTf1() { return avgTf1; }
    /** @return 炉温2 平均值（°C） */
    public double getAvgTf2() { return avgTf2; }
    /** @return 表面温 平均值（°C） */
    public double getAvgTs()  { return avgTs; }
    /** @return 中心温 平均值（°C） */
    public double getAvgTc()  { return avgTc; }

    /** @return 内部状态机实例（供高级用例使用） */
    public TestStateMachine getStateMachine() {
        return stateMachine;
    }

    /** @return 内部仿真引擎实例（供高级用例使用） */
    public SensorSimulator getSimulator() {
        return simulator;
    }

    // ==================== 保存状态保护（ISO 11820 业务规则） ====================

    /**
     * 查询试验是否已保存。
     * <p>
     * ISO 11820: 试验记录保存后标记为 "10000000"（对应数据库 testmaster.flag），
     * 防止未保存的完成试验被覆盖。
     * </p>
     *
     * @return {@code true} 如果 saveFlag 等于 "10000000"
     */
    public boolean isSaved() {
        return "10000000".equals(saveFlag);
    }

    /**
     * 标记试验为已保存状态。
     * <p>
     * ISO 11820: 将 saveFlag 置为 "10000000"，表示本次试验记录已持久化。
     * 标记后允许新建试验或重置。
     * </p>
     */
    public void markSaved() {
        this.saveFlag = "10000000";
        log.info("试验已标记为保存状态（saveFlag=10000000）");
    }

    // ==================== 恒功率值（ISO 11820 业务规则） ====================

    /**
     * 获取本次试验的恒功率基准值。
     * <p>
     * ISO 11820: 恒功率值在 startRecording() 时由 READY 阶段的功率采样队列平均值计算得出。
     * </p>
     *
     * @return 恒功率值（kW），保留 1 位小数；未开始记录时返回 0
     */
    public double getConstantPower() {
        return constantPower;
    }

    /**
     * 计算恒功率值 —— 取功率采样队列的平均值。
     * <p>
     * 仅在持有 {@link #tickLock} 时调用。
     * ISO 11820: 进入 Ready 状态后持续采集 PID 输出值，开始记录时取平均值作为恒功率基准。
     * </p>
     */
    private void computeConstantPower() {
        if (powerSamples.isEmpty()) {
            constantPower = 0;
            return;
        }
        double sum = 0;
        for (double p : powerSamples) {
            sum += p;
        }
        constantPower = roundOneDecimal(sum / powerSamples.size());
        log.info(() -> String.format("恒功率计算完成：%.1f kW（采样数=%d）",
                constantPower, powerSamples.size()));
    }

    /**
     * 采集功率值到采样队列。
     * <p>
     * ISO 11820: READY 状态下每次迭代采集当前仿真功率值。
     * 队列最大容量 600（约 8 分钟采样数据）。
     * </p>
     *
     * @param tf1 当前炉温1（°C），用于推导功率值
     */
    private void samplePower(double tf1) {
        double power = derivePower(tf1);
        // ISO 11820: 功率采样队列最大 600 个
        if (powerSamples.size() >= 600) {
            powerSamples.remove(0);
        }
        powerSamples.add(power);
    }

    /**
     * 基于炉温与升温速率推导仿真功率值。
     * <p>
     * ISO 11820: 仿真模式下功率值基于炉温和升温速率推导（单位 kW）。
     * 公式：基础功率 50kW + 升温速率 × 2.0 × (1 + 温差比例 × 0.5)。
     * 炉温越接近目标，功率越低。
     * </p>
     *
     * @param tf1 当前炉温1（°C）
     * @return 推导功率值（kW）
     */
    private double derivePower(double tf1) {
        double targetTemp = simulator.getTargetTemp();
        double heatingRate = simulator.getHeatingRatePerSecond();
        double basePower = 50.0;
        double tempFactor = Math.max(0, (targetTemp - tf1) / targetTemp);
        return basePower + heatingRate * 2.0 * (1.0 + tempFactor * 0.5);
    }

    // ==================== 温漂计算（ISO 11820 提前终止条件） ====================

    /**
     * 使用最小二乘法线性回归计算最近 10 分钟炉温 tf1 的变化斜率。
     * <p>
     * ISO 11820 业务规则：
     * 对最近 10 分钟（600 个数据点）的炉温序列做线性回归，
     * 斜率即为温漂（°C/10min）。数据不足 10 分钟时返回 {@link Double#MAX_VALUE}，
     * 视为不满足提前终止条件。
     * </p>
     *
     * <h3>算法</h3>
     * <pre>
     *   slope = (n * Σ(xy) - Σx * Σy) / (n * Σ(x²) - (Σx)²)
     *   温漂 = slope × 600  （°C/10min）
     * </pre>
     *
     * @return 温漂值（°C/10min），保留 2 位小数；数据不足时返回 {@link Double#MAX_VALUE}
     */
    private double calcTempDrift() {
        int total = recordedData.size();
        int windowSize = Math.min(600, total); // 10 分钟 = 600 秒

        // ISO 11820: 数据不足 10 分钟，返回 MAX_VALUE 视为不满足条件
        if (windowSize < 60) {
            return Double.MAX_VALUE;
        }

        int startIdx = total - windowSize;
        int n = windowSize;

        // 最小二乘法：x 为秒序号 [0, 1, ..., n-1]，y 为炉温1
        // 利用等差数列求和公式优化计算
        double sumX   = n * (n - 1.0) / 2.0;
        double sumX2  = n * (n - 1.0) * (2.0 * n - 1.0) / 6.0;
        double sumY   = 0;
        double sumXY  = 0;

        for (int i = 0; i < n; i++) {
            double y = recordedData.get(startIdx + i).getTf1();
            sumY  += y;
            sumXY += i * y;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            return Double.MAX_VALUE; // 避免除零
        }

        // 斜率 °C/s
        double slopePerSecond = (n * sumXY - sumX * sumY) / denominator;

        // 转换为 °C/10min
        double driftPer10Min = slopePerSecond * 600.0;

        // 保留 2 位小数
        return Math.round(driftPer10Min * 100.0) / 100.0;
    }

    /**
     * 获取最近一次计算的温漂值。
     *
     * @return 温漂值（°C/10min），保留 2 位小数；未计算时返回 {@link Double#MAX_VALUE}
     */
    public double getTempDrift() {
        return tempDrift;
    }

    // ==================== 最小值查询（ISO 11820 统计补全） ====================

    /** @return 炉温1 最小值（°C） */
    public double getMinTf1() { return minTf1 == Double.MAX_VALUE ? 0 : minTf1; }
    /** @return 炉温2 最小值（°C） */
    public double getMinTf2() { return minTf2 == Double.MAX_VALUE ? 0 : minTf2; }
    /** @return 表面温 最小值（°C） */
    public double getMinTs()  { return minTs  == Double.MAX_VALUE ? 0 : minTs; }
    /** @return 中心温 最小值（°C） */
    public double getMinTc()  { return minTc  == Double.MAX_VALUE ? 0 : minTc; }

    // ==================== 试验结果封装（ISO 11820 统计补全） ====================

    /**
     * 试验结果 —— 封装试验完成后的全量统计数据。
     * <p>
     * ISO 11820: 试验进入 COMPLETE 状态后自动计算各通道最大值、最小值、最终值、
     * 最大值出现时间、总温升、平均温度、总记录时长、恒功率值、温漂值。
     * </p>
     *
     * @param maxTf1          炉温1 最大值（°C）
     * @param maxTf1Time      炉温1 最大值出现时间（秒）
     * @param maxTf2          炉温2 最大值（°C）
     * @param maxTf2Time      炉温2 最大值出现时间（秒）
     * @param maxTs           表面温 最大值（°C）
     * @param maxTsTime       表面温 最大值出现时间（秒）
     * @param maxTc           中心温 最大值（°C）
     * @param maxTcTime       中心温 最大值出现时间（秒）
     * @param minTf1          炉温1 最小值（°C）
     * @param minTf2          炉温2 最小值（°C）
     * @param minTs           表面温 最小值（°C）
     * @param minTc           中心温 最小值（°C）
     * @param finalTf1        炉温1 最终值（°C）
     * @param finalTf2        炉温2 最终值（°C）
     * @param finalTs         表面温 最终值（°C）
     * @param finalTc         中心温 最终值（°C）
     * @param deltaTf1        炉温1 温升（°C）
     * @param deltaTf2        炉温2 温升（°C）
     * @param deltaTs         表面温 温升（°C）
     * @param deltaTc         中心温 温升（°C）
     * @param deltaTf         综合温升（°C），取表面温升
     * @param avgTf1          炉温1 平均值（°C）
     * @param avgTf2          炉温2 平均值（°C）
     * @param avgTs           表面温 平均值（°C）
     * @param avgTc           中心温 平均值（°C）
     * @param totalRecordTime 总记录时长（秒）
     * @param constantPower   恒功率值（kW）
     * @param tempDrift       温漂值（°C/10min）
     */
    public record TestResult(
            double maxTf1, int maxTf1Time,
            double maxTf2, int maxTf2Time,
            double maxTs,  int maxTsTime,
            double maxTc,  int maxTcTime,
            double minTf1, double minTf2, double minTs, double minTc,
            double finalTf1, double finalTf2, double finalTs, double finalTc,
            double deltaTf1, double deltaTf2, double deltaTs, double deltaTc, double deltaTf,
            double avgTf1, double avgTf2, double avgTs, double avgTc,
            int totalRecordTime,
            double constantPower,
            double tempDrift
    ) {
        /**
         * 生成可读的统计摘要字符串。
         *
         * @return 格式化的统计摘要
         */
        public String toSummaryString() {
            return String.format(
                    "maxTf1=%.1f°C@%ds, deltaTf=%.1f°C, avgTf1=%.1f°C, "
                            + "duration=%ds, constPower=%.1fkW, drift=%.2f°C/10min",
                    maxTf1, maxTf1Time, deltaTf, avgTf1,
                    totalRecordTime, constantPower, tempDrift
            );
        }
    }

    /**
     * 获取试验结果封装对象。
     * <p>
     * ISO 11820: 试验进入 COMPLETE 状态后，可通过此方法获取全量统计结果。
     * 非 COMPLETE 状态下返回 {@code null}。
     * </p>
     *
     * @return 试验结果对象，如果试验尚未完成则返回 {@code null}
     */
    public TestResult getTestResult() {
        if (stateMachine.getCurrentState() != TestState.COMPLETE) {
            return null;
        }
        return new TestResult(
                maxTf1, maxTf1Time, maxTf2, maxTf2Time, maxTs, maxTsTime, maxTc, maxTcTime,
                getMinTf1(), getMinTf2(), getMinTs(), getMinTc(),
                finalTf1, finalTf2, finalTs, finalTc,
                deltaTf1, deltaTf2, deltaTs, deltaTc, deltaTf,
                avgTf1, avgTf2, avgTs, avgTc,
                recordedData.size(),
                constantPower,
                tempDrift
        );
    }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return String.format(
                "TestMaster[state=%s, scheduler=%s, recorded=%ds/%ds, dataPoints=%d, messages=%d, listeners=%d]",
                getCurrentState().getDisplayName(),
                isSchedulerRunning() ? "running" : "stopped",
                recordedDuration, targetDurationSeconds,
                getRecordedDataCount(), getMessageCount(), getListenerCount()
        );
    }
}