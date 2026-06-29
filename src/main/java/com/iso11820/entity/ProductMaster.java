package com.iso11820.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 样品实体 —— 对应数据库 {@code productmaster} 表。
 * <p>
 * 存储试验样品的基本信息，包括编号、名称、规格和尺寸。
 * 主键为 {@code productid}。
 * </p>
 *
 * <h3>字段说明</h3>
 * <table>
 *   <tr><th>字段</th><th>类型</th><th>说明</th></tr>
 *   <tr><td>productid</td><td>String</td><td>样品编号（主键），如 20240613-001</td></tr>
 *   <tr><td>productname</td><td>String</td><td>样品名称，如 岩棉隔热板</td></tr>
 *   <tr><td>specific</td><td>String</td><td>规格型号，如 100×50×25mm</td></tr>
 *   <tr><td>diameter</td><td>Double</td><td>直径（mm）</td></tr>
 *   <tr><td>height</td><td>Double</td><td>高度（mm）</td></tr>
 *   <tr><td>flag</td><td>String</td><td>备用字段（可空）</td></tr>
 * </table>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class ProductMaster implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 样品编号（主键），如 20240613-001 */
    private String productid;

    /** 样品名称，如 岩棉隔热板 */
    private String productname;

    /** 规格型号，如 100×50×25mm */
    private String specific;

    /** 直径（mm） */
    private Double diameter;

    /** 高度（mm） */
    private Double height;

    /** 备用字段（可空） */
    private String flag;

    // ==================== 构造方法 ====================

    /** 无参构造 */
    public ProductMaster() {
    }

    /**
     * 全参构造。
     *
     * @param productid   样品编号
     * @param productname 样品名称
     * @param specific    规格型号
     * @param diameter    直径（mm）
     * @param height      高度（mm）
     * @param flag        备用字段
     */
    public ProductMaster(String productid, String productname, String specific,
                         Double diameter, Double height, String flag) {
        this.productid = productid;
        this.productname = productname;
        this.specific = specific;
        this.diameter = diameter;
        this.height = height;
        this.flag = flag;
    }

    // ==================== Getter / Setter ====================

    public String getProductid() { return productid; }

    public void setProductid(String productid) { this.productid = productid; }

    public String getProductname() { return productname; }

    public void setProductname(String productname) { this.productname = productname; }

    public String getSpecific() { return specific; }

    public void setSpecific(String specific) { this.specific = specific; }

    public Double getDiameter() { return diameter; }

    public void setDiameter(Double diameter) { this.diameter = diameter; }

    public Double getHeight() { return height; }

    public void setHeight(Double height) { this.height = height; }

    public String getFlag() { return flag; }

    public void setFlag(String flag) { this.flag = flag; }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return new StringJoiner(", ", "ProductMaster[", "]")
                .add("productid='" + productid + "'")
                .add("productname='" + productname + "'")
                .add("specific='" + specific + "'")
                .add("diameter=" + diameter)
                .add("height=" + height)
                .add("flag='" + flag + "'")
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductMaster that)) return false;
        return Objects.equals(productid, that.productid)
            && Objects.equals(productname, that.productname)
            && Objects.equals(specific, that.specific)
            && Objects.equals(diameter, that.diameter)
            && Objects.equals(height, that.height)
            && Objects.equals(flag, that.flag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productid, productname, specific, diameter, height, flag);
    }
}