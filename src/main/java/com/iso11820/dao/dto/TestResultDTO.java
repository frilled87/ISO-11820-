package com.iso11820.dao.dto;

import java.io.Serializable;

/**
 * 试验结果 DTO —— 数据层与业务层之间的试验结果数据载体。
 * <p>
 * 纯数据对象，不依赖任何核心层或数据层实现。
 * 对应核心层 {@code TestMaster.TestResult} record 的字段结构。
 * </p>
 *
 * <h3>字段说明</h3>
 * <table>
 *   <tr><th>字段</th><th>类型</th><th>说明</th></tr>
 *   <tr><td>productId</td><td>String</td><td>样品编号</td></tr>
 *   <tr><td>testId</td><td>String</td><td>试验ID</td></tr>
 *   <tr><td>maxTf1</td><td>Double</td><td>炉温1最大值（°C）</td></tr>
 *   <tr><td>maxTf1Time</td><td>Integer</td><td>炉温1最大值出现时间（秒）</td></tr>
 *   <tr><td>deltaTf</td><td>Double</td><td>综合温升（°C）</td></tr>
 *   <tr><td>totalRecordTime</td><td>Integer</td><td>总记录时长（秒）</td></tr>
 *   <tr><td>constantPower</td><td>Integer</td><td>恒功率值（kW）</td></tr>
 *   <tr><td>preWeight</td><td>Double</td><td>试验前质量（g）</td></tr>
 *   <tr><td>postWeight</td><td>Double</td><td>试验后质量（g）</td></tr>
 *   <tr><td>lostWeightPer</td><td>Double</td><td>失重率（%）</td></tr>
 *   <tr><td>flameTime</td><td>Integer</td><td>火焰开始时刻（秒）</td></tr>
 *   <tr><td>flameDuration</td><td>Integer</td><td>火焰持续时间（秒）</td></tr>
 *   <tr><td>phenocode</td><td>String</td><td>现象编码</td></tr>
 *   <tr><td>memo</td><td>String</td><td>备注</td></tr>
 *   <tr><td>flag</td><td>String</td><td>完成标记</td></tr>
 * </table>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class TestResultDTO implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private String productId;
    private String testId;
    private Double maxTf1;
    private Integer maxTf1Time;
    private Double maxTf2;
    private Integer maxTf2Time;
    private Double maxTs;
    private Integer maxTsTime;
    private Double maxTc;
    private Integer maxTcTime;
    private Double minTf1;
    private Double minTf2;
    private Double minTs;
    private Double minTc;
    private Double finalTf1;
    private Double finalTf2;
    private Double finalTs;
    private Double finalTc;
    private Double deltaTf1;
    private Double deltaTf2;
    private Double deltaTf;
    private Double deltaTs;
    private Double deltaTc;
    private Double avgTf1;
    private Double avgTf2;
    private Double avgTs;
    private Double avgTc;
    private Integer totalRecordTime;
    private Integer constantPower;
    private Double preWeight;
    private Double postWeight;
    private Double lostWeightPer;
    private Integer flameTime;
    private Integer flameDuration;
    private String phenocode;
    private String memo;
    private String flag;

    // ==================== 构造方法 ====================

    public TestResultDTO() {}

    // ==================== Getter / Setter ====================

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = testId; }

    public Double getMaxTf1() { return maxTf1; }
    public void setMaxTf1(Double maxTf1) { this.maxTf1 = maxTf1; }

    public Integer getMaxTf1Time() { return maxTf1Time; }
    public void setMaxTf1Time(Integer maxTf1Time) { this.maxTf1Time = maxTf1Time; }

    public Double getMaxTf2() { return maxTf2; }
    public void setMaxTf2(Double maxTf2) { this.maxTf2 = maxTf2; }

    public Integer getMaxTf2Time() { return maxTf2Time; }
    public void setMaxTf2Time(Integer maxTf2Time) { this.maxTf2Time = maxTf2Time; }

    public Double getMaxTs() { return maxTs; }
    public void setMaxTs(Double maxTs) { this.maxTs = maxTs; }

    public Integer getMaxTsTime() { return maxTsTime; }
    public void setMaxTsTime(Integer maxTsTime) { this.maxTsTime = maxTsTime; }

    public Double getMaxTc() { return maxTc; }
    public void setMaxTc(Double maxTc) { this.maxTc = maxTc; }

    public Integer getMaxTcTime() { return maxTcTime; }
    public void setMaxTcTime(Integer maxTcTime) { this.maxTcTime = maxTcTime; }

    public Double getMinTf1() { return minTf1; }
    public void setMinTf1(Double minTf1) { this.minTf1 = minTf1; }

    public Double getMinTf2() { return minTf2; }
    public void setMinTf2(Double minTf2) { this.minTf2 = minTf2; }

    public Double getMinTs() { return minTs; }
    public void setMinTs(Double minTs) { this.minTs = minTs; }

    public Double getMinTc() { return minTc; }
    public void setMinTc(Double minTc) { this.minTc = minTc; }

    public Double getFinalTf1() { return finalTf1; }
    public void setFinalTf1(Double finalTf1) { this.finalTf1 = finalTf1; }

    public Double getFinalTf2() { return finalTf2; }
    public void setFinalTf2(Double finalTf2) { this.finalTf2 = finalTf2; }

    public Double getFinalTs() { return finalTs; }
    public void setFinalTs(Double finalTs) { this.finalTs = finalTs; }

    public Double getFinalTc() { return finalTc; }
    public void setFinalTc(Double finalTc) { this.finalTc = finalTc; }

    public Double getDeltaTf1() { return deltaTf1; }
    public void setDeltaTf1(Double deltaTf1) { this.deltaTf1 = deltaTf1; }

    public Double getDeltaTf2() { return deltaTf2; }
    public void setDeltaTf2(Double deltaTf2) { this.deltaTf2 = deltaTf2; }

    public Double getDeltaTf() { return deltaTf; }
    public void setDeltaTf(Double deltaTf) { this.deltaTf = deltaTf; }

    public Double getDeltaTs() { return deltaTs; }
    public void setDeltaTs(Double deltaTs) { this.deltaTs = deltaTs; }

    public Double getDeltaTc() { return deltaTc; }
    public void setDeltaTc(Double deltaTc) { this.deltaTc = deltaTc; }

    public Double getAvgTf1() { return avgTf1; }
    public void setAvgTf1(Double avgTf1) { this.avgTf1 = avgTf1; }

    public Double getAvgTf2() { return avgTf2; }
    public void setAvgTf2(Double avgTf2) { this.avgTf2 = avgTf2; }

    public Double getAvgTs() { return avgTs; }
    public void setAvgTs(Double avgTs) { this.avgTs = avgTs; }

    public Double getAvgTc() { return avgTc; }
    public void setAvgTc(Double avgTc) { this.avgTc = avgTc; }

    public Integer getTotalRecordTime() { return totalRecordTime; }
    public void setTotalRecordTime(Integer totalRecordTime) { this.totalRecordTime = totalRecordTime; }

    public Integer getConstantPower() { return constantPower; }
    public void setConstantPower(Integer constantPower) { this.constantPower = constantPower; }

    public Double getPreWeight() { return preWeight; }
    public void setPreWeight(Double preWeight) { this.preWeight = preWeight; }

    public Double getPostWeight() { return postWeight; }
    public void setPostWeight(Double postWeight) { this.postWeight = postWeight; }

    public Double getLostWeightPer() { return lostWeightPer; }
    public void setLostWeightPer(Double lostWeightPer) { this.lostWeightPer = lostWeightPer; }

    public Integer getFlameTime() { return flameTime; }
    public void setFlameTime(Integer flameTime) { this.flameTime = flameTime; }

    public Integer getFlameDuration() { return flameDuration; }
    public void setFlameDuration(Integer flameDuration) { this.flameDuration = flameDuration; }

    public String getPhenocode() { return phenocode; }
    public void setPhenocode(String phenocode) { this.phenocode = phenocode; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }
}