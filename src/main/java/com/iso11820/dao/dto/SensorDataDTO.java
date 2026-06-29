package com.iso11820.dao.dto;

import java.io.Serializable;

/**
 * 传感器数据 DTO —— 数据层与业务层之间的温度数据载体。
 * <p>
 * 对应核心层 {@code SensorData} 类的 5 通道温度快照结构。
 * 纯数据对象，不依赖任何核心层实现。
 * </p>
 *
 * <h3>5 个通道</h3>
 * <table>
 *   <tr><th>字段</th><th>通道名</th><th>说明</th></tr>
 *   <tr><td>tf1</td><td>炉温1（TF1）</td><td>加热炉内主温度，目标 750°C</td></tr>
 *   <tr><td>tf2</td><td>炉温2（TF2）</td><td>加热炉内副温度</td></tr>
 *   <tr><td>ts</td><td>表面温（TS）</td><td>样品表面温度</td></tr>
 *   <tr><td>tc</td><td>中心温（TC）</td><td>样品中心温度</td></tr>
 *   <tr><td>tCal</td><td>校准温（TCal）</td><td>标定用温度</td></tr>
 * </table>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class SensorDataDTO implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /** 炉温1（°C） */
    private Double tf1;

    /** 炉温2（°C） */
    private Double tf2;

    /** 表面温（°C） */
    private Double ts;

    /** 中心温（°C） */
    private Double tc;

    /** 校准温（°C） */
    private Double tCal;

    // ==================== 构造方法 ====================

    public SensorDataDTO() {}

    public SensorDataDTO(Double tf1, Double tf2, Double ts, Double tc, Double tCal) {
        this.tf1 = tf1;
        this.tf2 = tf2;
        this.ts = ts;
        this.tc = tc;
        this.tCal = tCal;
    }

    // ==================== Getter / Setter ====================

    public Double getTf1() { return tf1; }
    public void setTf1(Double tf1) { this.tf1 = tf1; }

    public Double getTf2() { return tf2; }
    public void setTf2(Double tf2) { this.tf2 = tf2; }

    public Double getTs() { return ts; }
    public void setTs(Double ts) { this.ts = ts; }

    public Double getTc() { return tc; }
    public void setTc(Double tc) { this.tc = tc; }

    public Double getTCal() { return tCal; }
    public void setTCal(Double tCal) { this.tCal = tCal; }
}