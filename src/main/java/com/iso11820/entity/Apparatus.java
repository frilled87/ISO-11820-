package com.iso11820.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 设备实体 —— 对应数据库 {@code apparatus} 表。
 * <p>
 * 存储试验设备的基本信息，包括设备编号、名称、检定有效期、
 * 串口配置以及上次记录的恒功率值。
 * </p>
 *
 * <h3>字段说明</h3>
 * <table>
 *   <tr><th>字段</th><th>类型</th><th>说明</th></tr>
 *   <tr><td>apparatusid</td><td>Integer</td><td>设备ID（主键）</td></tr>
 *   <tr><td>innernumber</td><td>String</td><td>设备内部编号，如 FURNACE-01</td></tr>
 *   <tr><td>apparatusname</td><td>String</td><td>设备名称，如 一号试验炉</td></tr>
 *   <tr><td>checkdatef</td><td>String</td><td>检定有效期开始（日期字符串）</td></tr>
 *   <tr><td>checkdatet</td><td>String</td><td>检定有效期结束（日期字符串）</td></tr>
 *   <tr><td>pidport</td><td>String</td><td>PID串口，如 COM9</td></tr>
 *   <tr><td>powerport</td><td>String</td><td>功率串口，如 COM9</td></tr>
 *   <tr><td>constpower</td><td>Integer</td><td>上次记录的恒功率值（可空）</td></tr>
 * </table>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class Apparatus implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备ID（主键） */
    private Integer apparatusid;

    /** 设备内部编号，如 FURNACE-01 */
    private String innernumber;

    /** 设备名称，如 一号试验炉 */
    private String apparatusname;

    /** 检定有效期开始（日期字符串） */
    private String checkdatef;

    /** 检定有效期结束（日期字符串） */
    private String checkdatet;

    /** PID串口，如 COM9 */
    private String pidport;

    /** 功率串口，如 COM9 */
    private String powerport;

    /** 上次记录的恒功率值（可空） */
    private Integer constpower;

    // ==================== 构造方法 ====================

    /** 无参构造 */
    public Apparatus() {
    }

    /**
     * 全参构造。
     *
     * @param apparatusid   设备ID
     * @param innernumber   设备内部编号
     * @param apparatusname 设备名称
     * @param checkdatef    检定有效期开始
     * @param checkdatet    检定有效期结束
     * @param pidport       PID串口
     * @param powerport     功率串口
     * @param constpower    恒功率值（可空）
     */
    public Apparatus(Integer apparatusid, String innernumber, String apparatusname,
                     String checkdatef, String checkdatet, String pidport,
                     String powerport, Integer constpower) {
        this.apparatusid = apparatusid;
        this.innernumber = innernumber;
        this.apparatusname = apparatusname;
        this.checkdatef = checkdatef;
        this.checkdatet = checkdatet;
        this.pidport = pidport;
        this.powerport = powerport;
        this.constpower = constpower;
    }

    // ==================== Getter / Setter ====================

    public Integer getApparatusid() { return apparatusid; }

    public void setApparatusid(Integer apparatusid) { this.apparatusid = apparatusid; }

    public String getInnernumber() { return innernumber; }

    public void setInnernumber(String innernumber) { this.innernumber = innernumber; }

    public String getApparatusname() { return apparatusname; }

    public void setApparatusname(String apparatusname) { this.apparatusname = apparatusname; }

    public String getCheckdatef() { return checkdatef; }

    public void setCheckdatef(String checkdatef) { this.checkdatef = checkdatef; }

    public String getCheckdatet() { return checkdatet; }

    public void setCheckdatet(String checkdatet) { this.checkdatet = checkdatet; }

    public String getPidport() { return pidport; }

    public void setPidport(String pidport) { this.pidport = pidport; }

    public String getPowerport() { return powerport; }

    public void setPowerport(String powerport) { this.powerport = powerport; }

    public Integer getConstpower() { return constpower; }

    public void setConstpower(Integer constpower) { this.constpower = constpower; }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return new StringJoiner(", ", "Apparatus[", "]")
                .add("apparatusid=" + apparatusid)
                .add("innernumber='" + innernumber + "'")
                .add("apparatusname='" + apparatusname + "'")
                .add("checkdatef='" + checkdatef + "'")
                .add("checkdatet='" + checkdatet + "'")
                .add("pidport='" + pidport + "'")
                .add("powerport='" + powerport + "'")
                .add("constpower=" + constpower)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Apparatus that)) return false;
        return Objects.equals(apparatusid, that.apparatusid)
            && Objects.equals(innernumber, that.innernumber)
            && Objects.equals(apparatusname, that.apparatusname)
            && Objects.equals(checkdatef, that.checkdatef)
            && Objects.equals(checkdatet, that.checkdatet)
            && Objects.equals(pidport, that.pidport)
            && Objects.equals(powerport, that.powerport)
            && Objects.equals(constpower, that.constpower);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apparatusid, innernumber, apparatusname,
                checkdatef, checkdatet, pidport, powerport, constpower);
    }
}