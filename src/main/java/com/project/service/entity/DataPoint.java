package com.project.service.entity;

import com.project.utils.DateUtil;
import com.project.utils.NumUtil;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <h1>时序温度数据点实体</h1>
 *
 * <p>表示试验记录阶段每秒采集的一组温度和环境数据。
 * 对应 CSV 文件中的一行记录。</p>
 *
 * <h2>CSV 列映射</h2>
 * <table>
 *   <tr><th>字段</th><th>CSV 列名</th><th>单位</th></tr>
 *   <tr><td>timeSeconds</td><td>Time</td><td>秒</td></tr>
 *   <tr><td>tf1</td><td>Temp1</td><td>°C</td></tr>
 *   <tr><td>tf2</td><td>Temp2</td><td>°C</td></tr>
 *   <tr><td>ts</td><td>TempSurface</td><td>°C</td></tr>
 *   <tr><td>tc</td><td>TempCenter</td><td>°C</td></tr>
 *   <tr><td>tCal</td><td>TempCalibration</td><td>°C</td></tr>
 *   <tr><td>ambientTemp</td><td>AmbientTemp</td><td>°C</td></tr>
 *   <tr><td>ambientHumidity</td><td>AmbientHumidity</td><td>%</td></tr>
 *   <tr><td>timestamp</td><td>Timestamp</td><td>—</td></tr>
 * </table>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public class DataPoint {

    /** 记录时刻（秒，从记录开始计时） */
    private int timeSeconds;

    /** 炉温1（°C） */
    private double tf1;

    /** 炉温2（°C） */
    private double tf2;

    /** 表面温度（°C） */
    private double ts;

    /** 中心温度（°C） */
    private double tc;

    /** 校准温度（°C） */
    private double tCal;

    /** 环境温度（°C） */
    private double ambientTemp;

    /** 环境湿度（%） */
    private double ambientHumidity;

    /** 记录时间戳（格式化字符串） */
    private String timestamp;

    // ============================================================
    //  构造
    // ============================================================

    /** 无参构造（所有数值默认 0，时间戳自动填充） */
    public DataPoint() {
        this.timestamp = DateUtil.now();
    }

    /**
     * 全参构造（温度值自动保留 1 位小数）。
     *
     * @param timeSeconds     记录时刻（秒）
     * @param tf1             炉温1
     * @param tf2             炉温2
     * @param ts              表面温度
     * @param tc              中心温度
     * @param tCal            校准温度
     * @param ambientTemp     环境温度
     * @param ambientHumidity 环境湿度
     */
    public DataPoint(int timeSeconds,
                     double tf1, double tf2,
                     double ts, double tc, double tCal,
                     double ambientTemp, double ambientHumidity) {
        this.timeSeconds = timeSeconds;
        this.tf1 = NumUtil.roundTemp(tf1);
        this.tf2 = NumUtil.roundTemp(tf2);
        this.ts = NumUtil.roundTemp(ts);
        this.tc = NumUtil.roundTemp(tc);
        this.tCal = NumUtil.roundTemp(tCal);
        this.ambientTemp = NumUtil.roundTemp(ambientTemp);
        this.ambientHumidity = NumUtil.round(ambientHumidity, 1);
        this.timestamp = DateUtil.now();
    }

    // ============================================================
    //  Getter / Setter
    // ============================================================

    public int getTimeSeconds() { return timeSeconds; }

    /**
     * 设置记录时刻。
     * @param timeSeconds 秒数（≥ 0）
     */
    public void setTimeSeconds(int timeSeconds) {
        if (timeSeconds < 0) {
            throw new IllegalArgumentException("记录时刻不能为负数");
        }
        this.timeSeconds = timeSeconds;
    }

    public double getTf1() { return tf1; }
    public void setTf1(double tf1) { this.tf1 = NumUtil.roundTemp(tf1); }

    public double getTf2() { return tf2; }
    public void setTf2(double tf2) { this.tf2 = NumUtil.roundTemp(tf2); }

    public double getTs() { return ts; }
    public void setTs(double ts) { this.ts = NumUtil.roundTemp(ts); }

    public double getTc() { return tc; }
    public void setTc(double tc) { this.tc = NumUtil.roundTemp(tc); }

    public double gettCal() { return tCal; }
    public void settCal(double tCal) { this.tCal = NumUtil.roundTemp(tCal); }

    public double getAmbientTemp() { return ambientTemp; }
    public void setAmbientTemp(double ambientTemp) { this.ambientTemp = NumUtil.roundTemp(ambientTemp); }

    public double getAmbientHumidity() { return ambientHumidity; }
    public void setAmbientHumidity(double ambientHumidity) { this.ambientHumidity = NumUtil.round(ambientHumidity, 1); }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) {
        this.timestamp = (timestamp != null) ? timestamp : DateUtil.now();
    }

    // ============================================================
    //  Object
    // ============================================================

    @Override
    public String toString() {
        return String.format(
                "DataPoint{t=%ds, tf1=%.1f, tf2=%.1f, ts=%.1f, tc=%.1f, tCal=%.1f, ambT=%.1f, ambH=%.1f, ts=%s}",
                timeSeconds, tf1, tf2, ts, tc, tCal, ambientTemp, ambientHumidity, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataPoint that)) return false;
        return timeSeconds == that.timeSeconds
                && Double.compare(that.tf1, tf1) == 0
                && Double.compare(that.tf2, tf2) == 0
                && Double.compare(that.ts, ts) == 0
                && Double.compare(that.tc, tc) == 0
                && Double.compare(that.tCal, tCal) == 0
                && Double.compare(that.ambientTemp, ambientTemp) == 0
                && Double.compare(that.ambientHumidity, ambientHumidity) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timeSeconds, tf1, tf2, ts, tc, tCal, ambientTemp, ambientHumidity);
    }
}