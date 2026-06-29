package com.iso11820.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 试验记录实体 —— 对应数据库 {@code testmaster} 表（核心表）⭐。
 * <p>
 * 存储每次试验的完整记录，包括基本信息、质量数据、试验过程、
 * 各通道温度统计（最大值/最终值/温升）等。联合主键为 {@code (productid, testid)}，
 * 外键 {@code productid} 引用 {@code productmaster.productid}。
 * </p>
 *
 * <h3>字段分组</h3>
 * <ul>
 *   <li><b>基本信息</b>：productid, testid, testdate, ambtemp, ambhumi, according,
 *       operator, apparatusid, apparatusname, apparatuschkdate, rptno</li>
 *   <li><b>质量数据</b>：preweight, postweight, lostweight, lostweight_per</li>
 *   <li><b>试验过程</b>：totaltesttime, constpower, phenocode, flametime, flameduration</li>
 *   <li><b>温度最大值及出现时刻</b>：maxtf1~maxtc, maxtf1_time~maxtc_time</li>
 *   <li><b>温度最终值</b>：finaltf1~finaltc, finaltf1_time~finaltc_time</li>
 *   <li><b>温升</b>：deltatf1, deltatf2, deltatf（判定项）, deltats, deltatc</li>
 *   <li><b>备注</b>：memo, flag</li>
 * </ul>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class TestMaster implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 基本信息 ====================

    /** 样品编号（联合主键 + 外键） */
    private String productid;

    /** 试验ID（联合主键），格式 yyyyMMdd-HHmmss */
    private String testid;

    /** 试验日期 */
    private String testdate;

    /** 环境温度（°C） */
    private Double ambtemp;

    /** 环境湿度（%） */
    private Double ambhumi;

    /** 试验依据，如 ISO 11820:2022 */
    private String according;

    /** 操作员用户名 */
    private String operator;

    /** 设备编号 */
    private String apparatusid;

    /** 设备名称（冗余，省去关联查询） */
    private String apparatusname;

    /** 设备检定日期 */
    private String apparatuschkdate;

    /** 报告编号 */
    private String rptno;

    // ==================== 质量数据 ====================

    /** 试验前质量（g） */
    private Double preweight;

    /** 试验后质量（g） */
    private Double postweight;

    /** 失重量（g），= preweight - postweight */
    private Double lostweight;

    /** 失重率（%），判定项 */
    private Double lostweightPer;

    // ==================== 试验过程 ====================

    /** 总试验时长（秒） */
    private Integer totaltesttime;

    /** 恒功率值（0~25600） */
    private Integer constpower;

    /** 现象编码（勾选项序列化字符串） */
    private String phenocode;

    /** 火焰开始时刻（秒，无火焰填0） */
    private Integer flametime;

    /** 火焰持续时间（秒，无火焰填0） */
    private Integer flameduration;

    // ==================== 各通道温度最大值 ====================

    /** 炉温1最大值（°C） */
    private Double maxtf1;

    /** 炉温2最大值（°C） */
    private Double maxtf2;

    /** 表面温最大值（°C） */
    private Double maxts;

    /** 中心温最大值（°C） */
    private Double maxtc;

    /** 炉温1最大值出现时刻（秒） */
    private Integer maxtf1Time;

    /** 炉温2最大值出现时刻（秒） */
    private Integer maxtf2Time;

    /** 表面温最大值出现时刻（秒） */
    private Integer maxtsTime;

    /** 中心温最大值出现时刻（秒） */
    private Integer maxtcTime;

    // ==================== 各通道温度最终值（试验结束时刻） ====================

    /** 炉温1最终值（°C） */
    private Double finaltf1;

    /** 炉温2最终值（°C） */
    private Double finaltf2;

    /** 表面温最终值（°C） */
    private Double finalts;

    /** 中心温最终值（°C） */
    private Double finaltc;

    /** 炉温1最终值时刻（秒） */
    private Integer finaltf1Time;

    /** 炉温2最终值时刻（秒） */
    private Integer finaltf2Time;

    /** 表面温最终值时刻（秒） */
    private Integer finaltsTime;

    /** 中心温最终值时刻（秒） */
    private Integer finaltcTime;

    // ==================== 温升（结束值 - 开始值） ====================

    /** 炉温1温升（°C） */
    private Double deltatf1;

    /** 炉温2温升（°C） */
    private Double deltatf2;

    /** 综合温升（°C），判定项，取表面温升 */
    private Double deltatf;

    /** 表面温温升（°C） */
    private Double deltats;

    /** 中心温温升（°C） */
    private Double deltatc;

    // ==================== 备注 ====================

    /** 备注（可空） */
    private String memo;

    /** 完成标记（"10000000"=已保存，可空） */
    private String flag;

    // ==================== 构造方法 ====================

    /** 无参构造 */
    public TestMaster() {
    }

    /**
     * 全参构造。
     *
     * @param productid        样品编号
     * @param testid           试验ID
     * @param testdate         试验日期
     * @param ambtemp          环境温度
     * @param ambhumi          环境湿度
     * @param according        试验依据
     * @param operator         操作员
     * @param apparatusid      设备编号
     * @param apparatusname    设备名称
     * @param apparatuschkdate 设备检定日期
     * @param rptno            报告编号
     * @param preweight        试验前质量
     * @param postweight       试验后质量
     * @param lostweight       失重量
     * @param lostweightPer    失重率
     * @param totaltesttime    总试验时长
     * @param constpower       恒功率值
     * @param phenocode        现象编码
     * @param flametime        火焰开始时刻
     * @param flameduration    火焰持续时间
     * @param maxtf1           炉温1最大值
     * @param maxtf2           炉温2最大值
     * @param maxts            表面温最大值
     * @param maxtc            中心温最大值
     * @param maxtf1Time       炉温1最大值时刻
     * @param maxtf2Time       炉温2最大值时刻
     * @param maxtsTime        表面温最大值时刻
     * @param maxtcTime        中心温最大值时刻
     * @param finaltf1         炉温1最终值
     * @param finaltf2         炉温2最终值
     * @param finalts          表面温最终值
     * @param finaltc          中心温最终值
     * @param finaltf1Time     炉温1最终值时刻
     * @param finaltf2Time     炉温2最终值时刻
     * @param finaltsTime      表面温最终值时刻
     * @param finaltcTime      中心温最终值时刻
     * @param deltatf1         炉温1温升
     * @param deltatf2         炉温2温升
     * @param deltatf          综合温升
     * @param deltats          表面温温升
     * @param deltatc          中心温温升
     * @param memo             备注
     * @param flag             完成标记
     */
    public TestMaster(String productid, String testid, String testdate,
                      Double ambtemp, Double ambhumi, String according,
                      String operator, String apparatusid, String apparatusname,
                      String apparatuschkdate, String rptno,
                      Double preweight, Double postweight, Double lostweight,
                      Double lostweightPer,
                      Integer totaltesttime, Integer constpower, String phenocode,
                      Integer flametime, Integer flameduration,
                      Double maxtf1, Double maxtf2, Double maxts, Double maxtc,
                      Integer maxtf1Time, Integer maxtf2Time,
                      Integer maxtsTime, Integer maxtcTime,
                      Double finaltf1, Double finaltf2, Double finalts, Double finaltc,
                      Integer finaltf1Time, Integer finaltf2Time,
                      Integer finaltsTime, Integer finaltcTime,
                      Double deltatf1, Double deltatf2, Double deltatf,
                      Double deltats, Double deltatc,
                      String memo, String flag) {
        this.productid = productid;
        this.testid = testid;
        this.testdate = testdate;
        this.ambtemp = ambtemp;
        this.ambhumi = ambhumi;
        this.according = according;
        this.operator = operator;
        this.apparatusid = apparatusid;
        this.apparatusname = apparatusname;
        this.apparatuschkdate = apparatuschkdate;
        this.rptno = rptno;
        this.preweight = preweight;
        this.postweight = postweight;
        this.lostweight = lostweight;
        this.lostweightPer = lostweightPer;
        this.totaltesttime = totaltesttime;
        this.constpower = constpower;
        this.phenocode = phenocode;
        this.flametime = flametime;
        this.flameduration = flameduration;
        this.maxtf1 = maxtf1;
        this.maxtf2 = maxtf2;
        this.maxts = maxts;
        this.maxtc = maxtc;
        this.maxtf1Time = maxtf1Time;
        this.maxtf2Time = maxtf2Time;
        this.maxtsTime = maxtsTime;
        this.maxtcTime = maxtcTime;
        this.finaltf1 = finaltf1;
        this.finaltf2 = finaltf2;
        this.finalts = finalts;
        this.finaltc = finaltc;
        this.finaltf1Time = finaltf1Time;
        this.finaltf2Time = finaltf2Time;
        this.finaltsTime = finaltsTime;
        this.finaltcTime = finaltcTime;
        this.deltatf1 = deltatf1;
        this.deltatf2 = deltatf2;
        this.deltatf = deltatf;
        this.deltats = deltats;
        this.deltatc = deltatc;
        this.memo = memo;
        this.flag = flag;
    }

    // ==================== Getter / Setter ====================

    public String getProductid() { return productid; }
    public void setProductid(String productid) { this.productid = productid; }

    public String getTestid() { return testid; }
    public void setTestid(String testid) { this.testid = testid; }

    public String getTestdate() { return testdate; }
    public void setTestdate(String testdate) { this.testdate = testdate; }

    public Double getAmbtemp() { return ambtemp; }
    public void setAmbtemp(Double ambtemp) { this.ambtemp = ambtemp; }

    public Double getAmbhumi() { return ambhumi; }
    public void setAmbhumi(Double ambhumi) { this.ambhumi = ambhumi; }

    public String getAccording() { return according; }
    public void setAccording(String according) { this.according = according; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getApparatusid() { return apparatusid; }
    public void setApparatusid(String apparatusid) { this.apparatusid = apparatusid; }

    public String getApparatusname() { return apparatusname; }
    public void setApparatusname(String apparatusname) { this.apparatusname = apparatusname; }

    public String getApparatuschkdate() { return apparatuschkdate; }
    public void setApparatuschkdate(String apparatuschkdate) { this.apparatuschkdate = apparatuschkdate; }

    public String getRptno() { return rptno; }
    public void setRptno(String rptno) { this.rptno = rptno; }

    public Double getPreweight() { return preweight; }
    public void setPreweight(Double preweight) { this.preweight = preweight; }

    public Double getPostweight() { return postweight; }
    public void setPostweight(Double postweight) { this.postweight = postweight; }

    public Double getLostweight() { return lostweight; }
    public void setLostweight(Double lostweight) { this.lostweight = lostweight; }

    public Double getLostweightPer() { return lostweightPer; }
    public void setLostweightPer(Double lostweightPer) { this.lostweightPer = lostweightPer; }

    public Integer getTotaltesttime() { return totaltesttime; }
    public void setTotaltesttime(Integer totaltesttime) { this.totaltesttime = totaltesttime; }

    public Integer getConstpower() { return constpower; }
    public void setConstpower(Integer constpower) { this.constpower = constpower; }

    public String getPhenocode() { return phenocode; }
    public void setPhenocode(String phenocode) { this.phenocode = phenocode; }

    public Integer getFlametime() { return flametime; }
    public void setFlametime(Integer flametime) { this.flametime = flametime; }

    public Integer getFlameduration() { return flameduration; }
    public void setFlameduration(Integer flameduration) { this.flameduration = flameduration; }

    public Double getMaxtf1() { return maxtf1; }
    public void setMaxtf1(Double maxtf1) { this.maxtf1 = maxtf1; }

    public Double getMaxtf2() { return maxtf2; }
    public void setMaxtf2(Double maxtf2) { this.maxtf2 = maxtf2; }

    public Double getMaxts() { return maxts; }
    public void setMaxts(Double maxts) { this.maxts = maxts; }

    public Double getMaxtc() { return maxtc; }
    public void setMaxtc(Double maxtc) { this.maxtc = maxtc; }

    public Integer getMaxtf1Time() { return maxtf1Time; }
    public void setMaxtf1Time(Integer maxtf1Time) { this.maxtf1Time = maxtf1Time; }

    public Integer getMaxtf2Time() { return maxtf2Time; }
    public void setMaxtf2Time(Integer maxtf2Time) { this.maxtf2Time = maxtf2Time; }

    public Integer getMaxtsTime() { return maxtsTime; }
    public void setMaxtsTime(Integer maxtsTime) { this.maxtsTime = maxtsTime; }

    public Integer getMaxtcTime() { return maxtcTime; }
    public void setMaxtcTime(Integer maxtcTime) { this.maxtcTime = maxtcTime; }

    public Double getFinaltf1() { return finaltf1; }
    public void setFinaltf1(Double finaltf1) { this.finaltf1 = finaltf1; }

    public Double getFinaltf2() { return finaltf2; }
    public void setFinaltf2(Double finaltf2) { this.finaltf2 = finaltf2; }

    public Double getFinalts() { return finalts; }
    public void setFinalts(Double finalts) { this.finalts = finalts; }

    public Double getFinaltc() { return finaltc; }
    public void setFinaltc(Double finaltc) { this.finaltc = finaltc; }

    public Integer getFinaltf1Time() { return finaltf1Time; }
    public void setFinaltf1Time(Integer finaltf1Time) { this.finaltf1Time = finaltf1Time; }

    public Integer getFinaltf2Time() { return finaltf2Time; }
    public void setFinaltf2Time(Integer finaltf2Time) { this.finaltf2Time = finaltf2Time; }

    public Integer getFinaltsTime() { return finaltsTime; }
    public void setFinaltsTime(Integer finaltsTime) { this.finaltsTime = finaltsTime; }

    public Integer getFinaltcTime() { return finaltcTime; }
    public void setFinaltcTime(Integer finaltcTime) { this.finaltcTime = finaltcTime; }

    public Double getDeltatf1() { return deltatf1; }
    public void setDeltatf1(Double deltatf1) { this.deltatf1 = deltatf1; }

    public Double getDeltatf2() { return deltatf2; }
    public void setDeltatf2(Double deltatf2) { this.deltatf2 = deltatf2; }

    public Double getDeltatf() { return deltatf; }
    public void setDeltatf(Double deltatf) { this.deltatf = deltatf; }

    public Double getDeltats() { return deltats; }
    public void setDeltats(Double deltats) { this.deltats = deltats; }

    public Double getDeltatc() { return deltatc; }
    public void setDeltatc(Double deltatc) { this.deltatc = deltatc; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return new StringJoiner(", ", "TestMaster[", "]")
                .add("productid='" + productid + "'")
                .add("testid='" + testid + "'")
                .add("testdate='" + testdate + "'")
                .add("operator='" + operator + "'")
                .add("totaltesttime=" + totaltesttime)
                .add("deltatf=" + deltatf)
                .add("flag='" + flag + "'")
                .toString();
    }

    /**
     * 基于联合主键 {@code (productid, testid)} 判断相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestMaster that)) return false;
        return Objects.equals(productid, that.productid)
            && Objects.equals(testid, that.testid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productid, testid);
    }
}