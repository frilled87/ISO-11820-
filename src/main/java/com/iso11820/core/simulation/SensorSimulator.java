package com.iso11820.core.simulation;

import com.iso11820.core.SensorData;
import com.iso11820.core.TestState;

import java.util.Objects;
import java.util.Random;

/**
 * 温度仿真引擎 —— 严格按 ISO 11820 文档算法生成 5 通道温度数据。
 * <p>
 * 每调用一次 {@link #update()} 执行一次仿真迭代（对应硬件 800ms 采集周期），
 * 根据当前试验状态和炉温自动选择升温/稳定/记录/降温四个阶段之一的算法。
 * </p>
 *
 * <h3>三个阶段算法（文档原文）</h3>
 *
 * <pre>
 * 【升温阶段】炉温1 &lt; TargetTemp - StableThreshold（即 &lt; 747°C）：
 *   TF1  += HeatingRatePerSecond × 0.8 + 随机噪声
 *   TF2  += HeatingRatePerSecond × 0.8 + 随机噪声（独立噪声）
 *   TS    = TF1 × 0.3 + 随机噪声
 *   TC    = TF1 × 0.25 + 随机噪声
 *   TCal  = TF1 + 随机噪声 × 2
 *
 * 【稳定阶段】炉温1 &gt;= 747°C：
 *   TF1 = 750 + 随机噪声   （直接钳位到目标温度）
 *   TF2 = 750 + 随机噪声
 *   稳定计数器++，当计数器 &gt; 3 时 IsStable = true
 *
 * 【记录阶段】RECORDING 状态：
 *   surfaceTarget = min(TF1 × 0.95, 800)
 *   TS += (surfaceTarget - TS) × 0.02 + 随机噪声   （指数接近，慢速上升）
 *   centerTarget  = min(TF1 × 0.85, 750)
 *   TC += (centerTarget - TC) × 0.01 + 随机噪声    （比表面温更慢）
 *
 * 【降温阶段】停止升温后（IDLE 状态且炉温 &gt; 初始温度）：
 *   TF1 -= 0.5 + 随机噪声 × 0.1
 *   TF2 -= 0.5 + 随机噪声 × 0.1
 * </pre>
 *
 * <h3>随机噪声公式</h3>
 * <pre>
 *   随机噪声 = (Random.nextDouble() × 2 - 1) × TempFluctuation
 *   即 [-TempFluctuation, +TempFluctuation] 区间均匀分布，默认 ±0.5°C
 * </pre>
 *
 * <h3>线程安全</h3>
 * 可配置参数使用 {@code volatile} 保证多线程可见性。<br>
 * {@link #update()} 方法使用 {@code synchronized} 保证每次迭代的原子性。<br>
 * 返回的 {@link SensorData} 是内部数据的副本，外部修改不影响仿真状态。
 *
 * <h3>单元测试要点</h3>
 * 可通过设置固定的 Random seed 获得可重现的仿真结果，用于验证算法正确性。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class SensorSimulator {

    // ==================== 算法常量 ====================

    /** 每次迭代对应的时间间隔（秒），即硬件采集周期 800ms */
    static final double UPDATE_INTERVAL_SECONDS = 0.8;

    /** 稳定判定范围下限（°C），对应 CheckStartCriteria 中的 745°C */
    static final double STABLE_RANGE_MIN = 745.0;

    /** 稳定判定范围上限（°C），对应 CheckStartCriteria 中的 755°C */
    static final double STABLE_RANGE_MAX = 755.0;

    /** 稳定计数器阈值，超过此次数后标记为稳定（约 3.2 秒） */
    static final int STABLE_COUNT_THRESHOLD = 3;

    /** 表面温指数逼近系数（记录阶段） */
    static final double SURFACE_APPROACH_RATE = 0.02;

    /** 中心温指数逼近系数（记录阶段，比表面温慢一半） */
    static final double CENTER_APPROACH_RATE = 0.01;

    /** 表面温目标上限（°C） */
    static final double SURFACE_TARGET_MAX = 800.0;

    /** 中心温目标上限（°C） */
    static final double CENTER_TARGET_MAX = 750.0;

    /** 降温速率（°C/次），对应文档中每次减 0.5°C */
    static final double COOLING_RATE = 0.5;

    /** 降温噪声系数，对应文档中随机噪声 × 0.1 */
    static final double COOLING_NOISE_FACTOR = 0.1;

    // ==================== 可配置参数（volatile 保证线程可见性） ====================

    /** 目标炉温（°C），默认 750°C */
    private volatile double targetTemp = 750.0;

    /** 升温速率（°C/s），默认 40°C/s */
    private volatile double heatingRatePerSecond = 40.0;

    /** 温度波动系数（°C），默认 0.5°C */
    private volatile double tempFluctuation = 0.5;

    /** 稳定阈值（°C），默认 3°C。升温阶段结束条件：TF1 >= targetTemp - stableThreshold = 747°C */
    private volatile double stableThreshold = 3.0;

    /** 初始/环境炉温（°C），默认 25°C */
    private volatile double initialTemp = 25.0;

    // ==================== 内部状态 ====================

    /** 当前 5 通道温度数据（仅在 synchronized 块内修改） */
    private final SensorData currentData;

    /**
     * 稳定计数器 —— 连续处于稳定阶段的迭代次数。
     * 仅在稳定阶段递增，进入升温或降温阶段时重置。
     */
    private volatile int stableCounter = 0;

    /**
     * 稳定标记 —— 当 stableCounter > STABLE_COUNT_THRESHOLD 时置为 true。
     * 进入升温或降温阶段时重置为 false。
     */
    private volatile boolean stableFlag = false;

    /** 当前试验状态，决定仿真算法分支 */
    private volatile TestState currentState = TestState.IDLE;

    /** 随机数生成器（线程安全：仅在 synchronized 块内使用） */
    private final Random random;

    // ==================== 构造方法 ====================

    /**
     * 默认构造 —— 使用文档规定的默认参数，随机种子。
     * <ul>
     *   <li>目标炉温：750°C</li>
     *   <li>升温速率：40°C/s</li>
     *   <li>温度波动：0.5°C</li>
     *   <li>稳定阈值：3°C</li>
     *   <li>初始炉温：25°C</li>
     * </ul>
     */
    public SensorSimulator() {
        this.currentData = new SensorData();
        this.random = new Random();
        resetToInitial();
    }

    /**
     * 指定随机种子的构造方法 —— 用于单元测试，获得可重现的仿真结果。
     *
     * @param seed 随机数生成器种子
     */
    public SensorSimulator(long seed) {
        this.currentData = new SensorData();
        this.random = new Random(seed);
        resetToInitial();
    }

    /**
     * 全参数构造方法。
     *
     * @param targetTemp          目标炉温（°C）
     * @param heatingRatePerSecond 升温速率（°C/s）
     * @param tempFluctuation      温度波动系数（°C）
     * @param stableThreshold      稳定阈值（°C）
     * @param initialTemp          初始炉温（°C）
     * @param seed                 随机种子
     */
    public SensorSimulator(double targetTemp, double heatingRatePerSecond,
                           double tempFluctuation, double stableThreshold,
                           double initialTemp, long seed) {
        this.targetTemp = targetTemp;
        this.heatingRatePerSecond = heatingRatePerSecond;
        this.tempFluctuation = tempFluctuation;
        this.stableThreshold = stableThreshold;
        this.initialTemp = initialTemp;
        this.currentData = new SensorData();
        this.random = new Random(seed);
        resetToInitial();
    }

    // ==================== 参数 Getter / Setter ====================

    /** @return 目标炉温（°C） */
    public double getTargetTemp() { return targetTemp; }

    /** @param targetTemp 目标炉温（°C） */
    public void setTargetTemp(double targetTemp) { this.targetTemp = targetTemp; }

    /** @return 升温速率（°C/s） */
    public double getHeatingRatePerSecond() { return heatingRatePerSecond; }

    /** @param heatingRatePerSecond 升温速率（°C/s） */
    public void setHeatingRatePerSecond(double heatingRatePerSecond) { this.heatingRatePerSecond = heatingRatePerSecond; }

    /** @return 温度波动系数（°C） */
    public double getTempFluctuation() { return tempFluctuation; }

    /** @param tempFluctuation 温度波动系数（°C） */
    public void setTempFluctuation(double tempFluctuation) { this.tempFluctuation = tempFluctuation; }

    /** @return 稳定阈值（°C） */
    public double getStableThreshold() { return stableThreshold; }

    /** @param stableThreshold 稳定阈值（°C） */
    public void setStableThreshold(double stableThreshold) { this.stableThreshold = stableThreshold; }

    /** @return 初始/环境炉温（°C） */
    public double getInitialTemp() { return initialTemp; }

    /** @param initialTemp 初始/环境炉温（°C） */
    public void setInitialTemp(double initialTemp) { this.initialTemp = initialTemp; }

    // ==================== 状态控制 ====================

    /**
     * 设置当前试验状态，影响仿真算法分支选择。
     *
     * @param state 当前试验状态，不可为 null
     * @throws NullPointerException 如果 state 为 null
     */
    public void setCurrentState(TestState state) {
        Objects.requireNonNull(state, "试验状态不能为 null");
        this.currentState = state;
    }

    /**
     * 获取当前试验状态。
     *
     * @return 当前试验状态
     */
    public TestState getCurrentState() {
        return currentState;
    }

    /**
     * 获取当前温度的只读副本。
     *
     * @return 当前 SensorData 的快照副本
     */
    public SensorData getCurrentData() {
        return new SensorData(currentData);
    }

    // ==================== 核心仿真算法 ====================

    /**
     * 执行一次仿真迭代（对应 800ms 硬件采集周期）。
     * <p>
     * 根据当前试验状态和炉温自动选择算法分支：
     * </p>
     * <ol>
     *   <li><b>IDLE</b>：如果炉温 &gt; 初始温度，执行降温；否则维持初始温度</li>
     *   <li><b>PREPARING</b>：炉温1 &lt; 747°C → 升温阶段；炉温1 &gt;= 747°C → 稳定阶段</li>
     *   <li><b>READY</b>：维持稳定阶段（钳位在目标温度 ± 噪声）</li>
     *   <li><b>RECORDING</b>：炉温1/炉温2 维持稳定，表面温/中心温 指数逼近目标</li>
     *   <li><b>COMPLETE</b>：维持稳定阶段</li>
     * </ol>
     *
     * @return 本次迭代后的 SensorData 快照副本（线程安全）
     */
    public synchronized SensorData update() {
        final TestState state = this.currentState;

        switch (state) {
            case IDLE:
                updateIdlePhase();
                break;
            case PREPARING:
                updatePreparingPhase();
                break;
            case READY:
                updateStablePhase();
                break;
            case RECORDING:
                updateRecordingPhase();
                break;
            case COMPLETE:
                updateStablePhase();
                break;
            default:
                // 未知状态，不做任何更新
                break;
        }

        return new SensorData(currentData);
    }

    // ==================== 各阶段算法实现 ====================

    /**
     * IDLE 阶段 —— 停止加热后的降温。
     * <p>
     * 算法（文档原文）：
     * <pre>
     * 降温阶段（停止升温后）：
     *   TF1 -= 0.5 + 随机噪声 × 0.1
     *   TF2 -= 0.5 + 随机噪声 × 0.1
     * </pre>
     * 表面温和中心温按比例同步降温。温度降至初始温度以下时钳位到初始温度。
     * 进入降温时重置稳定计数器和稳定标记。
     * </p>
     */
    private void updateIdlePhase() {
        double tf1 = currentData.getTf1();
        double tf2 = currentData.getTf2();
        double ts  = currentData.getTs();
        double tc  = currentData.getTc();

        // 重置稳定状态
        resetStableState();

        // 如果炉温已降至或低于初始温度，维持初始温度
        if (tf1 <= initialTemp) {
            currentData.setAll(initialTemp, initialTemp, initialTemp, initialTemp, initialTemp + generateNoise() * 2);
            return;
        }

        // 降温：炉温1 / 炉温2
        // TF1 -= 0.5 + 随机噪声 × 0.1
        // TF2 -= 0.5 + 随机噪声 × 0.1
        double noise1 = generateNoise();
        double noise2 = generateNoise();
        tf1 = tf1 - COOLING_RATE + noise1 * COOLING_NOISE_FACTOR;
        tf2 = tf2 - COOLING_RATE + noise2 * COOLING_NOISE_FACTOR;

        // 钳位到不低于初始温度
        tf1 = Math.max(tf1, initialTemp);
        tf2 = Math.max(tf2, initialTemp);

        // 表面温和中心温按比例同步降温
        ts = ts - COOLING_RATE * 0.3 + noise1 * COOLING_NOISE_FACTOR;
        tc = tc - COOLING_RATE * 0.25 + noise2 * COOLING_NOISE_FACTOR;
        ts = Math.max(ts, initialTemp);
        tc = Math.max(tc, initialTemp);

        // 校准温 = TF1 + 随机噪声 × 2
        double tCal = tf1 + generateNoise() * 2;

        currentData.setAll(tf1, tf2, ts, tc, tCal);
    }

    /**
     * PREPARING 阶段 —— 根据炉温自动选择升温或稳定子阶段。
     * <p>
     * 升温阶段（炉温1 &lt; TargetTemp - StableThreshold，即 &lt; 747°C）：
     * <pre>
     *   TF1  += HeatingRatePerSecond × 0.8 + 随机噪声
     *   TF2  += HeatingRatePerSecond × 0.8 + 随机噪声（独立噪声）
     *   TS    = TF1 × 0.3 + 随机噪声
     *   TC    = TF1 × 0.25 + 随机噪声
     *   TCal  = TF1 + 随机噪声 × 2
     * </pre>
     * 稳定阶段（炉温1 &gt;= 747°C）：
     * <pre>
     *   TF1 = 750 + 随机噪声（直接钳位到目标温度）
     *   TF2 = 750 + 随机噪声
     *   稳定计数器++，当计数器 &gt; 3 时 IsStable = true
     * </pre>
     * </p>
     */
    private void updatePreparingPhase() {
        double tf1 = currentData.getTf1();
        double tf2 = currentData.getTf2();

        // 升温阶段判断：炉温1 < TargetTemp - StableThreshold（即 < 747°C）
        if (tf1 < targetTemp - stableThreshold) {
            updateHeatingSubPhase();
            // 进入升温阶段时重置稳定计数器
            resetStableState();
        } else {
            updateStablePhase();
        }
    }

    /**
     * 升温子阶段 —— 炉温线性上升。
     * <p>
     * 算法：
     * <pre>
     *   TF1 += HeatingRatePerSecond × 0.8 + 随机噪声
     *   TF2 += HeatingRatePerSecond × 0.8 + 随机噪声（独立噪声）
     *   TS   = TF1 × 0.3 + 随机噪声
     *   TC   = TF1 × 0.25 + 随机噪声
     *   TCal = TF1 + 随机噪声 × 2
     * </pre>
     * 升温速率 × 0.8 是因为每次迭代对应 800ms（0.8 秒）。
     * 例如 40°C/s × 0.8s = 每次约 +32°C。
     * </p>
     */
    private void updateHeatingSubPhase() {
        double tf1 = currentData.getTf1();
        double tf2 = currentData.getTf2();

        // 每次升温增量 = 升温速率(°C/s) × 更新间隔(0.8s) = 40 × 0.8 = 32°C
        double heatingIncrement = heatingRatePerSecond * UPDATE_INTERVAL_SECONDS;

        // 炉温1：TF1 += HeatingRatePerSecond × 0.8 + 随机噪声
        // 炉温2：TF2 += HeatingRatePerSecond × 0.8 + 随机噪声（独立噪声）
        double noise1 = generateNoise();
        double noise2 = generateNoise();
        tf1 = tf1 + heatingIncrement + noise1;
        tf2 = tf2 + heatingIncrement + noise2;

        // 表面温：TS = TF1 × 0.3 + 随机噪声（非记录阶段低值跟随）
        double ts = tf1 * 0.3 + generateNoise();

        // 中心温：TC = TF1 × 0.25 + 随机噪声
        double tc = tf1 * 0.25 + generateNoise();

        // 校准温：TCal = TF1 + 随机噪声 × 2
        double tCal = tf1 + generateNoise() * 2;

        currentData.setAll(tf1, tf2, ts, tc, tCal);
    }

    /**
     * 稳定阶段 —— 炉温钳位在目标温度附近，记录稳定次数。
     * <p>
     * 算法：
     * <pre>
     *   TF1 = 750 + 随机噪声   （直接钳位到目标温度 ± 噪声）
     *   TF2 = 750 + 随机噪声
     *   稳定计数器++，当计数器 &gt; 3 时 IsStable = true（每 800ms 一次，约 3.2 秒）
     * </pre>
     * 仅在 RECORDING 状态下，表面温和中心温才使用指数逼近算法；
     * 在其他状态下（PREPARING 稳定子阶段、READY、COMPLETE），
     * 表面温和中心温维持低值跟随（同升温阶段）。
     * </p>
     */
    private void updateStablePhase() {
        double noise1 = generateNoise();
        double noise2 = generateNoise();

        // 炉温1：TF1 = 750 + 随机噪声（钳位到目标温度）
        // 炉温2：TF2 = 750 + 随机噪声（独立噪声）
        double tf1 = targetTemp + noise1;
        double tf2 = targetTemp + noise2;

        double ts  = currentData.getTs();
        double tc  = currentData.getTc();

        // 表面温和中心温：非记录阶段维持低值跟随
        // TS = TF1 × 0.3 + 随机噪声
        // TC = TF1 × 0.25 + 随机噪声
        ts = tf1 * 0.3 + generateNoise();
        tc = tf1 * 0.25 + generateNoise();

        // 校准温：TCal = TF1 + 随机噪声 × 2
        double tCal = tf1 + generateNoise() * 2;

        currentData.setAll(tf1, tf2, ts, tc, tCal);

        // 稳定计数器累加
        // 稳定计数器 ++，当计数器 > 3 时 IsStable = true
        stableCounter++;
        if (stableCounter > STABLE_COUNT_THRESHOLD) {
            stableFlag = true;
        }
    }

    /**
     * 记录阶段 —— 炉温维持稳定，表面温和中心温指数逼近各自目标。
     * <p>
     * 算法（文档原文）：
     * <pre>
     *   surfaceTarget = min(TF1 × 0.95, 800)
     *   TS += (surfaceTarget - TS) × 0.02 + 随机噪声   （指数接近，慢速上升）
     *
     *   centerTarget = min(TF1 × 0.85, 750)
     *   TC += (centerTarget - TC) × 0.01 + 随机噪声    （比表面温更慢）
     * </pre>
     * 炉温1和炉温2维持稳定（钳位到目标温度 ± 噪声），校准温同上。
     * </p>
     */
    private void updateRecordingPhase() {
        double noise1 = generateNoise();
        double noise2 = generateNoise();

        // 炉温1和炉温2维持稳定（钳位到目标温度）
        double tf1 = targetTemp + noise1;
        double tf2 = targetTemp + noise2;

        double ts = currentData.getTs();
        double tc = currentData.getTc();

        // 表面温指数逼近
        // surfaceTarget = min(TF1 × 0.95, 800)
        // TS += (surfaceTarget - TS) × 0.02 + 随机噪声
        double surfaceTarget = Math.min(tf1 * 0.95, SURFACE_TARGET_MAX);
        double noise3 = generateNoise();
        ts = ts + (surfaceTarget - ts) * SURFACE_APPROACH_RATE + noise3;

        // 中心温指数逼近（比表面温更慢，系数 0.01）
        // centerTarget = min(TF1 × 0.85, 750)
        // TC += (centerTarget - TC) × 0.01 + 随机噪声
        double centerTarget = Math.min(tf1 * 0.85, CENTER_TARGET_MAX);
        double noise4 = generateNoise();
        tc = tc + (centerTarget - tc) * CENTER_APPROACH_RATE + noise4;

        // 校准温：TCal = TF1 + 随机噪声 × 2
        double tCal = tf1 + generateNoise() * 2;

        currentData.setAll(tf1, tf2, ts, tc, tCal);

        // 记录阶段炉温应保持稳定，持续累加稳定计数器
        stableCounter++;
        if (stableCounter > STABLE_COUNT_THRESHOLD) {
            stableFlag = true;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成随机噪声。
     * <p>
     * 公式：随机噪声 = (Random.nextDouble() × 2 - 1) × TempFluctuation
     * 即 [-TempFluctuation, +TempFluctuation] 区间均匀分布。
     * 默认 TempFluctuation = 0.5，噪声范围为 [-0.5, +0.5]°C。
     * </p>
     *
     * @return 随机噪声值（°C）
     */
    private double generateNoise() {
        // random.nextDouble() 返回 [0.0, 1.0)
        // × 2 - 1 映射到 [-1.0, 1.0)
        // × tempFluctuation 映射到 [-0.5, +0.5)（默认情况）
        return (random.nextDouble() * 2.0 - 1.0) * tempFluctuation;
    }

    /**
     * 重置稳定状态 —— 清零稳定计数器并将稳定标记置为 false。
     * <p>
     * 在进入升温阶段或降温阶段时调用，确保 isStable() 返回值正确。
     * </p>
     */
    private void resetStableState() {
        stableCounter = 0;
        stableFlag = false;
    }

    /**
     * 将所有温度通道重置为初始温度。
     * 同时重置稳定状态。
     */
    private void resetToInitial() {
        currentData.setAll(initialTemp, initialTemp, initialTemp, initialTemp, initialTemp);
        resetStableState();
    }

    // ==================== 公开查询方法 ====================

    /**
     * 判断当前炉温是否稳定。
     * <p>
     * 稳定条件（同时满足）：
     * </p>
     * <ol>
     *   <li>稳定标记为 true（稳定计数器已超过阈值 3）</li>
     *   <li>炉温1 在稳定范围内（745°C ~ 755°C）</li>
     * </ol>
     * <p>
     * 此方法对应文档中的 CheckStartCriteria：同时满足「745~755°C」且「IsStable」→ 切换到 Ready。
     * </p>
     *
     * @return true 如果炉温稳定
     */
    public boolean isStable() {
        if (!stableFlag) {
            return false;
        }
        double tf1 = currentData.getTf1();
        return tf1 >= STABLE_RANGE_MIN && tf1 <= STABLE_RANGE_MAX;
    }

    /**
     * 获取当前稳定计数器值。
     *
     * @return 稳定计数器（连续处于稳定阶段的迭代次数）
     */
    public int getStableCounter() {
        return stableCounter;
    }

    /**
     * 获取当前稳定标记（仅计数器条件，不判断温度范围）。
     *
     * @return true 如果稳定计数器已超过阈值
     */
    public boolean isStableFlag() {
        return stableFlag;
    }

    /**
     * 判断炉温1是否已达到进入稳定阶段的门槛（>= targetTemp - stableThreshold）。
     * 即 >= 747°C（默认配置下）。
     *
     * @return true 如果炉温1已达到稳定门槛
     */
    public boolean hasReachedStableThreshold() {
        return currentData.getTf1() >= targetTemp - stableThreshold;
    }

    /**
     * 重置仿真引擎到初始状态。
     * 所有温度通道恢复为初始温度，稳定计数器清零，稳定标记重置。
     */
    public synchronized void reset() {
        resetToInitial();
    }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        SensorData snapshot = getCurrentData();
        return String.format(
                "SensorSimulator[state=%s, tf1=%.1f°C, tf2=%.1f°C, ts=%.1f°C, tc=%.1f°C, "
                        + "stable=%s, counter=%d, target=%.1f°C]",
                currentState.getDisplayName(),
                snapshot.getTf1(), snapshot.getTf2(), snapshot.getTs(), snapshot.getTc(),
                isStable(), stableCounter, targetTemp
        );
    }
}