package com.iso11820.core;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.StringJoiner;

/**
 * 温度数据实体 —— 一次采样时刻的 5 通道温度快照。
 * <p>
 * 对应 ISO 11820 标准中的 5 个温度测量通道，单位均为摄氏度（°C），
 * 所有温度值对外输出时保留 1 位小数。
 * </p>
 *
 * <h3>5 个通道</h3>
 * <table>
 *   <tr><th>字段</th><th>通道名</th><th>说明</th></tr>
 *   <tr><td>tf1</td><td>炉温1（TF1）</td><td>加热炉内主温度，仿真目标 750°C</td></tr>
 *   <tr><td>tf2</td><td>炉温2（TF2）</td><td>加热炉内副温度，与 TF1 同步但有独立噪声</td></tr>
 *   <tr><td>ts</td><td>表面温（TS）</td><td>样品表面温度，记录阶段向炉温×0.95 指数接近</td></tr>
 *   <tr><td>tc</td><td>中心温（TC）</td><td>样品中心温度，记录阶段向炉温×0.85 指数接近（比表面温更慢）</td></tr>
 *   <tr><td>tCal</td><td>校准温（TCal）</td><td>标定用温度，= TF1 + 随机波动×2，仅数值显示不画曲线</td></tr>
 * </table>
 *
 * <h3>线程安全</h3>
 * 所有温度字段使用 {@code volatile} 修饰，保证后台仿真线程写入后
 * UI 线程能立即看到最新值。每个字段的 double 赋值本身在 64 位 JVM 上是原子的。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class SensorData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 温度通道字段 ====================

    /** 炉温1 —— 加热炉内主温度（°C），保留 1 位小数 */
    private volatile double tf1;

    /** 炉温2 —— 加热炉内副温度（°C），保留 1 位小数 */
    private volatile double tf2;

    /** 表面温 —— 样品表面温度（°C），保留 1 位小数 */
    private volatile double ts;

    /** 中心温 —— 样品中心温度（°C），保留 1 位小数 */
    private volatile double tc;

    /** 校准温 —— 标定用参考温度（°C），保留 1 位小数 */
    private volatile double tCal;

    // ==================== 构造方法 ====================

    /**
     * 默认构造 —— 所有通道初始化为 0.0°C
     */
    public SensorData() {
        this(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * 全参数构造 —— 指定所有 5 个通道的温度值
     *
     * @param tf1  炉温1（°C）
     * @param tf2  炉温2（°C）
     * @param ts   表面温（°C）
     * @param tc   中心温（°C）
     * @param tCal 校准温（°C）
     */
    public SensorData(double tf1, double tf2, double ts, double tc, double tCal) {
        this.tf1  = roundOneDecimal(tf1);
        this.tf2  = roundOneDecimal(tf2);
        this.ts   = roundOneDecimal(ts);
        this.tc   = roundOneDecimal(tc);
        this.tCal = roundOneDecimal(tCal);
    }

    /**
     * 拷贝构造 —— 从另一个 SensorData 实例创建副本
     *
     * @param other 源数据对象，不可为 null
     * @throws NullPointerException 如果 other 为 null
     */
    public SensorData(SensorData other) {
        this.tf1  = other.tf1;
        this.tf2  = other.tf2;
        this.ts   = other.ts;
        this.tc   = other.tc;
        this.tCal = other.tCal;
    }

    // ==================== Getter / Setter ====================

    /** @return 炉温1（°C），保留 1 位小数 */
    public double getTf1() { return tf1; }

    /** @param tf1 炉温1（°C），自动四舍五入到 1 位小数 */
    public void setTf1(double tf1) { this.tf1 = roundOneDecimal(tf1); }

    /** @return 炉温2（°C），保留 1 位小数 */
    public double getTf2() { return tf2; }

    /** @param tf2 炉温2（°C），自动四舍五入到 1 位小数 */
    public void setTf2(double tf2) { this.tf2 = roundOneDecimal(tf2); }

    /** @return 表面温（°C），保留 1 位小数 */
    public double getTs() { return ts; }

    /** @param ts 表面温（°C），自动四舍五入到 1 位小数 */
    public void setTs(double ts) { this.ts = roundOneDecimal(ts); }

    /** @return 中心温（°C），保留 1 位小数 */
    public double getTc() { return tc; }

    /** @param tc 中心温（°C），自动四舍五入到 1 位小数 */
    public void setTc(double tc) { this.tc = roundOneDecimal(tc); }

    /** @return 校准温（°C），保留 1 位小数 */
    public double gettCal() { return tCal; }

    /** @param tCal 校准温（°C），自动四舍五入到 1 位小数 */
    public void settCal(double tCal) { this.tCal = roundOneDecimal(tCal); }

    // ==================== 批量操作 ====================

    /**
     * 一次性更新所有 5 通道温度值（比逐个 setter 调用效率更高）。
     *
     * @param tf1  炉温1（°C）
     * @param tf2  炉温2（°C）
     * @param ts   表面温（°C）
     * @param tc   中心温（°C）
     * @param tCal 校准温（°C）
     */
    public void setAll(double tf1, double tf2, double ts, double tc, double tCal) {
        this.tf1  = roundOneDecimal(tf1);
        this.tf2  = roundOneDecimal(tf2);
        this.ts   = roundOneDecimal(ts);
        this.tc   = roundOneDecimal(tc);
        this.tCal = roundOneDecimal(tCal);
    }

    /**
     * 从另一个 SensorData 实例复制所有温度值到当前对象。
     *
     * @param source 源数据对象，不可为 null
     * @throws NullPointerException 如果 source 为 null
     */
    public void copyFrom(SensorData source) {
        this.tf1  = source.tf1;
        this.tf2  = source.tf2;
        this.ts   = source.ts;
        this.tc   = source.tc;
        this.tCal = source.tCal;
    }

    // ==================== 工具方法 ====================

    /**
     * 将温度值四舍五入到小数点后 1 位。
     *
     * @param value 原始值
     * @return 保留 1 位小数的值
     */
    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * 获取格式化后的温度值字符串（保留 1 位小数）。
     *
     * @param value 温度值
     * @return 格式化字符串，如 "750.0"
     */
    public static String format(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .toString();
    }

    // ==================== Object 重写 ====================

    /**
     * 生成格式化的多行温度快照字符串，便于调试和日志输出。
     *
     * @return 多行温度信息
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", "[", "]")
                .add("炉温1=" + format(tf1) + "°C")
                .add("炉温2=" + format(tf2) + "°C")
                .add("表面温=" + format(ts) + "°C")
                .add("中心温=" + format(tc) + "°C")
                .add("校准温=" + format(tCal) + "°C")
                .toString();
    }

    /**
     * 基于 5 个通道温度值判断相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SensorData that)) return false;
        return Double.compare(that.tf1, tf1) == 0
            && Double.compare(that.tf2, tf2) == 0
            && Double.compare(that.ts,  ts)  == 0
            && Double.compare(that.tc,  tc)  == 0
            && Double.compare(that.tCal, tCal) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(tf1);
        result = 31 * result + Double.hashCode(tf2);
        result = 31 * result + Double.hashCode(ts);
        result = 31 * result + Double.hashCode(tc);
        result = 31 * result + Double.hashCode(tCal);
        return result;
    }
}