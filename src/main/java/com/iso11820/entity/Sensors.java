package com.iso11820.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 传感器配置实体 —— 对应数据库 {@code sensors} 表。
 * <p>
 * 存储传感器通道的配置信息，包括量程、信号类型、当前运行值等。
 * 主键为 {@code sensorid}。业务核心使用通道 0（炉温1）、1（炉温2）、
 * 2（表面温度）、3（中心温度）、16（校准温度），其余为备用通道。
 * </p>
 *
 * <h3>字段说明</h3>
 * <table>
 *   <tr><th>字段</th><th>类型</th><th>说明</th></tr>
 *   <tr><td>sensorid</td><td>Integer</td><td>传感器ID（主键）</td></tr>
 *   <tr><td>sensorname</td><td>String</td><td>传感器代号，如 Sensor0</td></tr>
 *   <tr><td>dispname</td><td>String</td><td>显示名，如 炉温1</td></tr>
 *   <tr><td>sensorgroup</td><td>String</td><td>分组标识（采集/校准/备用）</td></tr>
 *   <tr><td>unit</td><td>String</td><td>单位，如 ℃</td></tr>
 *   <tr><td>discription</td><td>String</td><td>描述</td></tr>
 *   <tr><td>flag</td><td>String</td><td>标记（启用/禁用）</td></tr>
 *   <tr><td>signalzero</td><td>Double</td><td>信号零点</td></tr>
 *   <tr><td>signalspan</td><td>Double</td><td>信号量程</td></tr>
 *   <tr><td>outputzero</td><td>Double</td><td>输出温度下限（如 0）</td></tr>
 *   <tr><td>outputspan</td><td>Double</td><td>输出温度上限（如 1000）</td></tr>
 *   <tr><td>outputvalue</td><td>Double</td><td>当前温度值（运行时更新）</td></tr>
 *   <tr><td>inputvalue</td><td>Double</td><td>当前输入值（运行时更新）</td></tr>
 *   <tr><td>signaltype</td><td>Integer</td><td>信号类型：4=数字量（仿真用）</td></tr>
 * </table>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class Sensors implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 传感器ID（主键） */
    private Integer sensorid;

    /** 传感器代号，如 Sensor0 */
    private String sensorname;

    /** 显示名，如 炉温1 */
    private String dispname;

    /** 分组标识（采集/校准/备用） */
    private String sensorgroup;

    /** 单位，如 ℃ */
    private String unit;

    /** 描述 */
    private String discription;

    /** 标记（启用/禁用） */
    private String flag;

    /** 信号零点 */
    private Double signalzero;

    /** 信号量程 */
    private Double signalspan;

    /** 输出温度下限（如 0） */
    private Double outputzero;

    /** 输出温度上限（如 1000） */
    private Double outputspan;

    /** 当前温度值（运行时更新） */
    private Double outputvalue;

    /** 当前输入值（运行时更新） */
    private Double inputvalue;

    /** 信号类型：4=数字量（仿真用） */
    private Integer signaltype;

    // ==================== 构造方法 ====================

    /** 无参构造 */
    public Sensors() {
    }

    /**
     * 全参构造。
     *
     * @param sensorid    传感器ID
     * @param sensorname  传感器代号
     * @param dispname    显示名
     * @param sensorgroup 分组标识
     * @param unit        单位
     * @param discription 描述
     * @param flag        标记
     * @param signalzero  信号零点
     * @param signalspan  信号量程
     * @param outputzero  输出温度下限
     * @param outputspan  输出温度上限
     * @param outputvalue 当前温度值
     * @param inputvalue  当前输入值
     * @param signaltype  信号类型
     */
    public Sensors(Integer sensorid, String sensorname, String dispname, String sensorgroup,
                   String unit, String discription, String flag,
                   Double signalzero, Double signalspan, Double outputzero,
                   Double outputspan, Double outputvalue, Double inputvalue,
                   Integer signaltype) {
        this.sensorid = sensorid;
        this.sensorname = sensorname;
        this.dispname = dispname;
        this.sensorgroup = sensorgroup;
        this.unit = unit;
        this.discription = discription;
        this.flag = flag;
        this.signalzero = signalzero;
        this.signalspan = signalspan;
        this.outputzero = outputzero;
        this.outputspan = outputspan;
        this.outputvalue = outputvalue;
        this.inputvalue = inputvalue;
        this.signaltype = signaltype;
    }

    // ==================== Getter / Setter ====================

    public Integer getSensorid() { return sensorid; }

    public void setSensorid(Integer sensorid) { this.sensorid = sensorid; }

    public String getSensorname() { return sensorname; }

    public void setSensorname(String sensorname) { this.sensorname = sensorname; }

    public String getDispname() { return dispname; }

    public void setDispname(String dispname) { this.dispname = dispname; }

    public String getSensorgroup() { return sensorgroup; }

    public void setSensorgroup(String sensorgroup) { this.sensorgroup = sensorgroup; }

    public String getUnit() { return unit; }

    public void setUnit(String unit) { this.unit = unit; }

    public String getDiscription() { return discription; }

    public void setDiscription(String discription) { this.discription = discription; }

    public String getFlag() { return flag; }

    public void setFlag(String flag) { this.flag = flag; }

    public Double getSignalzero() { return signalzero; }

    public void setSignalzero(Double signalzero) { this.signalzero = signalzero; }

    public Double getSignalspan() { return signalspan; }

    public void setSignalspan(Double signalspan) { this.signalspan = signalspan; }

    public Double getOutputzero() { return outputzero; }

    public void setOutputzero(Double outputzero) { this.outputzero = outputzero; }

    public Double getOutputspan() { return outputspan; }

    public void setOutputspan(Double outputspan) { this.outputspan = outputspan; }

    public Double getOutputvalue() { return outputvalue; }

    public void setOutputvalue(Double outputvalue) { this.outputvalue = outputvalue; }

    public Double getInputvalue() { return inputvalue; }

    public void setInputvalue(Double inputvalue) { this.inputvalue = inputvalue; }

    public Integer getSignaltype() { return signaltype; }

    public void setSignaltype(Integer signaltype) { this.signaltype = signaltype; }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return new StringJoiner(", ", "Sensors[", "]")
                .add("sensorid=" + sensorid)
                .add("sensorname='" + sensorname + "'")
                .add("dispname='" + dispname + "'")
                .add("sensorgroup='" + sensorgroup + "'")
                .add("unit='" + unit + "'")
                .add("discription='" + discription + "'")
                .add("flag='" + flag + "'")
                .add("signalzero=" + signalzero)
                .add("signalspan=" + signalspan)
                .add("outputzero=" + outputzero)
                .add("outputspan=" + outputspan)
                .add("outputvalue=" + outputvalue)
                .add("inputvalue=" + inputvalue)
                .add("signaltype=" + signaltype)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sensors that)) return false;
        return Objects.equals(sensorid, that.sensorid)
            && Objects.equals(sensorname, that.sensorname)
            && Objects.equals(dispname, that.dispname)
            && Objects.equals(sensorgroup, that.sensorgroup)
            && Objects.equals(unit, that.unit)
            && Objects.equals(discription, that.discription)
            && Objects.equals(flag, that.flag)
            && Objects.equals(signalzero, that.signalzero)
            && Objects.equals(signalspan, that.signalspan)
            && Objects.equals(outputzero, that.outputzero)
            && Objects.equals(outputspan, that.outputspan)
            && Objects.equals(outputvalue, that.outputvalue)
            && Objects.equals(inputvalue, that.inputvalue)
            && Objects.equals(signaltype, that.signaltype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sensorid, sensorname, dispname, sensorgroup, unit, discription,
                flag, signalzero, signalspan, outputzero, outputspan, outputvalue,
                inputvalue, signaltype);
    }
}