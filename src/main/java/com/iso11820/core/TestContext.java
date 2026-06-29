package com.iso11820.core;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 试验上下文实体 —— 存储当前试验的所有核心信息。
 * <p>
 * 每个试验（对应 testmaster 表一条记录）有一个 TestContext 实例，
 * 贯穿试验的整个生命周期：从新建试验 → 升温 → 记录 → 完成 → 保存。
 * </p>
 *
 * <h3>字段分组</h3>
 * <ul>
 *   <li><b>试验标识</b>：productId（样品编号）、testId（试验ID，格式 yyyyMMdd-HHmmss）</li>
 *   <li><b>环境参数</b>：ambientTemp（环境温度 °C）、ambientHumidity（环境湿度 %）</li>
 *   <li><b>样品质量</b>：preWeight（试验前质量 g）、postWeight（试验后质量 g）</li>
 *   <li><b>试验过程</b>：currentState（当前状态）、recordedDuration（已记录秒数）</li>
 *   <li><b>设备信息</b>：apparatusId（设备编号）、apparatusName（设备名称）</li>
 * </ul>
 *
 * <h3>线程安全</h3>
 * 不可变标识字段（productId、testId）使用 {@code final}；<br>
 * 可变字段使用 {@code volatile} 保证多线程可见性，确保后台采集线程写入后 UI 线程能立即读取。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class TestContext implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 试验 ID 日期格式：yyyyMMdd-HHmmss */
    public static final DateTimeFormatter TEST_ID_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    // ==================== 试验标识（不可变） ====================

    /** 样品编号（联合主键之一），如 "20240613-001" */
    private final String productId;

    /** 试验 ID（联合主键之一），格式 yyyyMMdd-HHmmss，如 "20240613-143025" */
    private final String testId;

    // ==================== 环境参数（可变） ====================

    /** 环境温度（°C），保留 1 位小数 */
    private volatile double ambientTemp;

    /** 环境湿度（%），保留 1 位小数 */
    private volatile double ambientHumidity;

    // ==================== 样品质量（可变） ====================

    /** 试验前样品质量（克），保留 2 位小数 */
    private volatile double preWeight;

    /** 试验后样品质量（克），保留 2 位小数，试验完成前为 0 */
    private volatile double postWeight;

    // ==================== 试验过程（可变） ====================

    /** 当前试验状态 */
    private volatile TestState currentState;

    /** 已记录时长（秒），仅在 RECORDING 状态下递增 */
    private volatile int recordedDuration;

    // ==================== 设备信息（可变） ====================

    /** 设备编号，如 "FURNACE-01" */
    private volatile String apparatusId;

    /** 设备名称，如 "一号试验炉" */
    private volatile String apparatusName;

    // ==================== 构造方法 ====================

    /**
     * 最小构造 —— 创建试验上下文并生成试验 ID。
     * <p>
     * 试验 ID 自动以当前时间生成，格式为 {@code yyyyMMdd-HHmmss}。
     * 初始状态为 {@link TestState#IDLE}。
     * </p>
     *
     * @param productId 样品编号，不可为 null 或空
     * @throws IllegalArgumentException 如果 productId 为 null 或空
     */
    public TestContext(String productId) {
        this(productId, LocalDateTime.now().format(TEST_ID_FORMATTER));
    }

    /**
     * 全标识构造 —— 指定样品编号和试验 ID。
     * <p>
     * 初始状态为 {@link TestState#IDLE}，其余字段为默认值 0。
     * </p>
     *
     * @param productId 样品编号，不可为 null 或空
     * @param testId    试验 ID，不可为 null 或空
     * @throws IllegalArgumentException 如果任一参数为 null 或空
     */
    public TestContext(String productId, String testId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("样品编号（productId）不能为空");
        }
        if (testId == null || testId.isBlank()) {
            throw new IllegalArgumentException("试验ID（testId）不能为空");
        }
        this.productId = productId;
        this.testId = testId;
        this.currentState = TestState.IDLE;
        this.recordedDuration = 0;
    }

    // ==================== Getter / Setter ====================

    /** @return 样品编号 */
    public String getProductId() { return productId; }

    /** @return 试验 ID */
    public String getTestId() { return testId; }

    /** @return 环境温度（°C） */
    public double getAmbientTemp() { return ambientTemp; }

    /** @param ambientTemp 环境温度（°C），自动四舍五入到 1 位小数 */
    public void setAmbientTemp(double ambientTemp) {
        this.ambientTemp = Math.round(ambientTemp * 10.0) / 10.0;
    }

    /** @return 环境湿度（%） */
    public double getAmbientHumidity() { return ambientHumidity; }

    /** @param ambientHumidity 环境湿度（%），自动四舍五入到 1 位小数 */
    public void setAmbientHumidity(double ambientHumidity) {
        this.ambientHumidity = Math.round(ambientHumidity * 10.0) / 10.0;
    }

    /** @return 试验前样品质量（克） */
    public double getPreWeight() { return preWeight; }

    /** @param preWeight 试验前质量（克），自动四舍五入到 2 位小数 */
    public void setPreWeight(double preWeight) {
        this.preWeight = Math.round(preWeight * 100.0) / 100.0;
    }

    /** @return 试验后样品质量（克），试验完成前为 0 */
    public double getPostWeight() { return postWeight; }

    /** @param postWeight 试验后质量（克），自动四舍五入到 2 位小数 */
    public void setPostWeight(double postWeight) {
        this.postWeight = Math.round(postWeight * 100.0) / 100.0;
    }

    /** @return 当前试验状态 */
    public TestState getCurrentState() { return currentState; }

    /**
     * 设置当前状态（由状态机内部调用，不直接暴露给外部随意修改）。
     *
     * @param currentState 新状态
     */
    void setCurrentState(TestState currentState) {
        this.currentState = currentState;
    }

    /** @return 已记录时长（秒） */
    public int getRecordedDuration() { return recordedDuration; }

    /** @param recordedDuration 已记录时长（秒） */
    public void setRecordedDuration(int recordedDuration) {
        this.recordedDuration = recordedDuration;
    }

    /** @return 设备编号 */
    public String getApparatusId() { return apparatusId; }

    /** @param apparatusId 设备编号 */
    public void setApparatusId(String apparatusId) { this.apparatusId = apparatusId; }

    /** @return 设备名称 */
    public String getApparatusName() { return apparatusName; }

    /** @param apparatusName 设备名称 */
    public void setApparatusName(String apparatusName) { this.apparatusName = apparatusName; }

    // ==================== 便捷查询 ====================

    /**
     * 判断当前是否为可操作的活跃状态（非 IDLE、非 COMPLETE）。
     *
     * @return true 如果当前正在进行试验
     */
    public boolean isActive() {
        return currentState != TestState.IDLE && currentState != TestState.COMPLETE;
    }

    /**
     * 判断试验是否已结束记录阶段。
     *
     * @return true 如果当前状态为 COMPLETE
     */
    public boolean isComplete() {
        return currentState == TestState.COMPLETE;
    }

    /**
     * 判断是否处于空闲状态。
     *
     * @return true 如果当前状态为 IDLE
     */
    public boolean isIdle() {
        return currentState == TestState.IDLE;
    }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return new StringJoiner(", ", "TestContext[", "]")
                .add("productId='" + productId + "'")
                .add("testId='" + testId + "'")
                .add("state=" + currentState)
                .add("ambient=" + formatTemp(ambientTemp) + "°C")
                .add("humidity=" + formatTemp(ambientHumidity) + "%")
                .add("preWeight=" + formatWeight(preWeight) + "g")
                .add("duration=" + recordedDuration + "s")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestContext that)) return false;
        return Objects.equals(productId, that.productId)
            && Objects.equals(testId, that.testId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, testId);
    }

    /** 格式化温度值（1 位小数） */
    private static String formatTemp(double v) {
        return String.format("%.1f", v);
    }

    /** 格式化质量值（2 位小数） */
    private static String formatWeight(double v) {
        return String.format("%.2f", v);
    }
}