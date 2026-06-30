package com.iso11820.service.model;

import com.iso11820.utils.NumUtil;

import java.util.Objects;

/**
 * <h1>试验报告数据实体（导出用）</h1>
 *
 * <p>纯数据载体，供 {@code ExcelReportService} 和 {@code PdfReportService} 使用。
 * 字段覆盖 testmaster 表全部关键列，不依赖数据库实体类。</p>
 *
 * <h2>使用方式</h2>
 * <pre>{@code
 * // UI 层或 Service 层组装数据后传入导出服务
 * ExportTestInfo info = new ExportTestInfo();
 * info.setProductId("P001");
 * info.setTestId("20260630-143000");
 * info.setPreWeight(50.0);
 * info.setPostWeight(45.0);
 * // ... 填充其他字段 ...
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public class ExportTestInfo {

    // ============================================================
    //  基本信息
    // ============================================================

    /** 样品编号 */
    private String productId = "";
    /** 试验 ID */
    private String testId = "";
    /** 试验日期 */
    private String testDate = "";
    /** 操作员 */
    private String operator = "";
    /** 试验依据 */
    private String according = "";
    /** 设备编号 */
    private String apparatusId = "";
    /** 设备名称 */
    private String apparatusName = "";
    /** 报告编号 */
    private String reportNo = "";

    // ============================================================
    //  环境参数
    // ============================================================

    /** 环境温度（°C） */
    private double ambientTemp;
    /** 环境湿度（%） */
    private double ambientHumidity;

    // ============================================================
    //  质量数据
    // ============================================================

    /** 试验前质量（g） */
    private double preWeight;
    /** 试验后质量（g） */
    private double postWeight;
    /** 失重量（g） */
    private double lostWeight;
    /** 失重率（%） */
    private double lostWeightPer;

    // ============================================================
    //  温度统计数据
    // ============================================================

    /** 炉温1 最大值 */
    private double maxTf1;
    /** 炉温2 最大值 */
    private double maxTf2;
    /** 表面温 最大值 */
    private double maxTs;
    /** 中心温 最大值 */
    private double maxTc;

    /** 炉温1 最终值 */
    private double finalTf1;
    /** 炉温2 最终值 */
    private double finalTf2;
    /** 表面温 最终值 */
    private double finalTs;
    /** 中心温 最终值 */
    private double finalTc;

    /** 炉温1 温升 */
    private double deltaTf1;
    /** 炉温2 温升 */
    private double deltaTf2;
    /** 表面温 温升 */
    private double deltaTs;
    /** 中心温 温升 */
    private double deltaTc;
    /** 综合温升（判定项） */
    private double deltaTf;

    // ============================================================
    //  试验过程
    // ============================================================

    /** 总试验时长（秒） */
    private int totalTestTime;
    /** 恒功率值 */
    private int constPower;
    /** 火焰开始时刻（秒） */
    private int flameTime;
    /** 火焰持续时间（秒） */
    private int flameDuration;
    /** 现象编码 */
    private String phenoCode = "";
    /** 备注 */
    private String memo = "";

    // ============================================================
    //  判定结论
    // ============================================================

    /** 是否通过（ISO 11820 简化判定） */
    private boolean passed;
    /** 判定结论说明 */
    private String conclusion = "";

    // ============================================================
    //  构造
    // ============================================================

    /** 无参构造 */
    public ExportTestInfo() {}

    // ============================================================
    //  Getter / Setter
    // ============================================================

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = (productId != null) ? productId : ""; }

    public String getTestId() { return testId; }
    public void setTestId(String testId) { this.testId = (testId != null) ? testId : ""; }

    public String getTestDate() { return testDate; }
    public void setTestDate(String testDate) { this.testDate = (testDate != null) ? testDate : ""; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = (operator != null) ? operator : ""; }

    public String getAccording() { return according; }
    public void setAccording(String according) { this.according = (according != null) ? according : ""; }

    public String getApparatusId() { return apparatusId; }
    public void setApparatusId(String apparatusId) { this.apparatusId = (apparatusId != null) ? apparatusId : ""; }

    public String getApparatusName() { return apparatusName; }
    public void setApparatusName(String apparatusName) { this.apparatusName = (apparatusName != null) ? apparatusName : ""; }

    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = (reportNo != null) ? reportNo : ""; }

    public double getAmbientTemp() { return ambientTemp; }
    public void setAmbientTemp(double ambientTemp) { this.ambientTemp = ambientTemp; }

    public double getAmbientHumidity() { return ambientHumidity; }
    public void setAmbientHumidity(double ambientHumidity) { this.ambientHumidity = ambientHumidity; }

    public double getPreWeight() { return preWeight; }
    public void setPreWeight(double preWeight) { this.preWeight = preWeight; }

    public double getPostWeight() { return postWeight; }
    public void setPostWeight(double postWeight) { this.postWeight = postWeight; }

    public double getLostWeight() { return lostWeight; }
    public void setLostWeight(double lostWeight) { this.lostWeight = lostWeight; }

    public double getLostWeightPer() { return lostWeightPer; }
    public void setLostWeightPer(double lostWeightPer) { this.lostWeightPer = lostWeightPer; }

    public double getMaxTf1() { return maxTf1; }
    public void setMaxTf1(double maxTf1) { this.maxTf1 = maxTf1; }

    public double getMaxTf2() { return maxTf2; }
    public void setMaxTf2(double maxTf2) { this.maxTf2 = maxTf2; }

    public double getMaxTs() { return maxTs; }
    public void setMaxTs(double maxTs) { this.maxTs = maxTs; }

    public double getMaxTc() { return maxTc; }
    public void setMaxTc(double maxTc) { this.maxTc = maxTc; }

    public double getFinalTf1() { return finalTf1; }
    public void setFinalTf1(double finalTf1) { this.finalTf1 = finalTf1; }

    public double getFinalTf2() { return finalTf2; }
    public void setFinalTf2(double finalTf2) { this.finalTf2 = finalTf2; }

    public double getFinalTs() { return finalTs; }
    public void setFinalTs(double finalTs) { this.finalTs = finalTs; }

    public double getFinalTc() { return finalTc; }
    public void setFinalTc(double finalTc) { this.finalTc = finalTc; }

    public double getDeltaTf1() { return deltaTf1; }
    public void setDeltaTf1(double deltaTf1) { this.deltaTf1 = deltaTf1; }

    public double getDeltaTf2() { return deltaTf2; }
    public void setDeltaTf2(double deltaTf2) { this.deltaTf2 = deltaTf2; }

    public double getDeltaTs() { return deltaTs; }
    public void setDeltaTs(double deltaTs) { this.deltaTs = deltaTs; }

    public double getDeltaTc() { return deltaTc; }
    public void setDeltaTc(double deltaTc) { this.deltaTc = deltaTc; }

    public double getDeltaTf() { return deltaTf; }
    public void setDeltaTf(double deltaTf) { this.deltaTf = deltaTf; }

    public int getTotalTestTime() { return totalTestTime; }
    public void setTotalTestTime(int totalTestTime) { this.totalTestTime = totalTestTime; }

    public int getConstPower() { return constPower; }
    public void setConstPower(int constPower) { this.constPower = constPower; }

    public int getFlameTime() { return flameTime; }
    public void setFlameTime(int flameTime) { this.flameTime = flameTime; }

    public int getFlameDuration() { return flameDuration; }
    public void setFlameDuration(int flameDuration) { this.flameDuration = flameDuration; }

    public String getPhenoCode() { return phenoCode; }
    public void setPhenoCode(String phenoCode) { this.phenoCode = (phenoCode != null) ? phenoCode : ""; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = (memo != null) ? memo : ""; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = (conclusion != null) ? conclusion : ""; }

    // ============================================================
    //  便捷方法
    // ============================================================

    /**
     * 根据 ISO 11820 简化规则自动判定。
     * 规则：综合温升 ≤ 50°C 且 失重率 ≤ 50% 且 火焰持续时间 < 5 秒
     */
    public void evaluatePassed() {
        this.passed = deltaTf <= 50.0 && lostWeightPer <= 50.0 && flameDuration < 5;
        this.conclusion = passed ? "判定通过 — 符合不燃性材料标准" : "判定不通过 — 不符合不燃性材料标准";
    }

    /**
     * 格式化温度为显示字符串，空值返回 "--"。
     */
    public static String formatTemp(double value) {
        return value == 0.0 ? "--" : NumUtil.formatTemp(value);
    }

    /**
     * 格式化百分比为显示字符串，空值返回 "--"。
     */
    public static String formatPercent(double value) {
        return value == 0.0 ? "--" : NumUtil.formatPercent(value / 100.0, 2);
    }

    @Override
    public String toString() {
        return String.format(
                "ExportTestInfo{pid=%s, tid=%s, date=%s, op=%s, passed=%s}",
                productId, testId, testDate, operator, passed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExportTestInfo that)) return false;
        return Objects.equals(productId, that.productId) && Objects.equals(testId, that.testId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, testId);
    }
}