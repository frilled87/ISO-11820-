package com.iso11820.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 校准记录实体 —— 对应数据库 {@code CalibrationRecords} 表。
 * <p>
 * 存储设备校准历史记录，包含炉壁 9 测温点（A/B/C层 × 1/2/3轴）的温度数据、
 * 均匀性计算结果、中心轴温度数据等。表名首字母大写，与其他表不同。
 * 主键为 {@code Id}（GUID）。
 * </p>
 *
 * <h3>字段分组</h3>
 * <ul>
 *   <li><b>基本信息</b>：Id, CalibrationDate, CalibrationType, ApparatusId, Operator</li>
 *   <li><b>温度数据</b>：TemperatureData（JSON字符串）, CenterTempData（JSON字符串）</li>
 *   <li><b>炉壁9测温点</b>：TempA1~TempC3</li>
 *   <li><b>计算结果</b>：TAvg, TAvgAxis1~3, TAvgLevela~c, TDevAxis1~3, TDevLevela~c, TAvgDevAxis, TAvgDevLevel</li>
 *   <li><b>判定</b>：UniformityResult, MaxDeviation, AverageTemperature, PassedCriteria</li>
 * </ul>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class CalibrationRecords implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ==================== 基本信息 ====================

    /** 主键（GUID） */
    private String id;

    /** 校准日期时间（ISO 8601字符串） */
    private String calibrationDate;

    /** 校准类型：Surface 或 Center */
    private String calibrationType;

    /** 设备ID */
    private Integer apparatusId;

    /** 操作员 */
    private String operator;

    // ==================== 温度数据 ====================

    /** 温度数据集合（JSON字符串） */
    private String temperatureData;

    /** 中心轴温度数据（JSON字符串，可空） */
    private String centerTempData;

    // ==================== 判定结果 ====================

    /** 均匀性结果 */
    private Double uniformityResult;

    /** 最大偏差 */
    private Double maxDeviation;

    /** 平均温度 */
    private Double averageTemperature;

    /** 是否通过：0=未通过，1=通过 */
    private Integer passedCriteria;

    // ==================== 备注 ====================

    /** 备注 */
    private String remarks;

    /** 创建时间 */
    private String createdAt;

    /** 附加备注（可空） */
    private String memo;

    // ==================== 炉壁9测温点（A/B/C层 × 1/2/3轴） ====================

    /** A层1轴测温点 */
    private Double tempA1;
    /** A层2轴测温点 */
    private Double tempA2;
    /** A层3轴测温点 */
    private Double tempA3;
    /** B层1轴测温点 */
    private Double tempB1;
    /** B层2轴测温点 */
    private Double tempB2;
    /** B层3轴测温点 */
    private Double tempB3;
    /** C层1轴测温点 */
    private Double tempC1;
    /** C层2轴测温点 */
    private Double tempC2;
    /** C层3轴测温点 */
    private Double tempC3;

    // ==================== 计算结果 ====================

    /** 总均温 */
    private Double tAvg;

    /** 轴向平均温度（轴1/2/3） */
    private Double tAvgAxis1;
    private Double tAvgAxis2;
    private Double tAvgAxis3;

    /** 层向平均温度（层A/B/C） */
    private Double tAvgLevela;
    private Double tAvgLevelb;
    private Double tAvgLevelc;

    /** 轴向温度偏差（轴1/2/3） */
    private Double tDevAxis1;
    private Double tDevAxis2;
    private Double tDevAxis3;

    /** 层向温度偏差（层A/B/C） */
    private Double tDevLevela;
    private Double tDevLevelb;
    private Double tDevLevelc;

    /** 轴向平均偏差 */
    private Double tAvgDevAxis;

    /** 层向平均偏差 */
    private Double tAvgDevLevel;

    // ==================== 构造方法 ====================

    /** 无参构造 */
    public CalibrationRecords() {
    }

    /**
     * 全参构造。
     *
     * @param id                 主键（GUID）
     * @param calibrationDate    校准日期时间
     * @param calibrationType    校准类型
     * @param apparatusId        设备ID
     * @param operator           操作员
     * @param temperatureData    温度数据JSON
     * @param uniformityResult   均匀性结果
     * @param maxDeviation       最大偏差
     * @param averageTemperature 平均温度
     * @param passedCriteria     是否通过
     * @param remarks            备注
     * @param createdAt          创建时间
     * @param tempA1  A层1轴
     * @param tempA2  A层2轴
     * @param tempA3  A层3轴
     * @param tempB1  B层1轴
     * @param tempB2  B层2轴
     * @param tempB3  B层3轴
     * @param tempC1  C层1轴
     * @param tempC2  C层2轴
     * @param tempC3  C层3轴
     * @param tAvg         总均温
     * @param tAvgAxis1    轴向均温1
     * @param tAvgAxis2    轴向均温2
     * @param tAvgAxis3    轴向均温3
     * @param tAvgLevela   层均温A
     * @param tAvgLevelb   层均温B
     * @param tAvgLevelc   层均温C
     * @param tDevAxis1    轴偏差1
     * @param tDevAxis2    轴偏差2
     * @param tDevAxis3    轴偏差3
     * @param tDevLevela   层偏差A
     * @param tDevLevelb   层偏差B
     * @param tDevLevelc   层偏差C
     * @param tAvgDevAxis  轴向平均偏差
     * @param tAvgDevLevel 层向平均偏差
     * @param centerTempData 中心轴JSON数据
     * @param memo          附加备注
     */
    public CalibrationRecords(String id, String calibrationDate, String calibrationType,
                              Integer apparatusId, String operator, String temperatureData,
                              Double uniformityResult, Double maxDeviation,
                              Double averageTemperature, Integer passedCriteria,
                              String remarks, String createdAt,
                              Double tempA1, Double tempA2, Double tempA3,
                              Double tempB1, Double tempB2, Double tempB3,
                              Double tempC1, Double tempC2, Double tempC3,
                              Double tAvg, Double tAvgAxis1, Double tAvgAxis2, Double tAvgAxis3,
                              Double tAvgLevela, Double tAvgLevelb, Double tAvgLevelc,
                              Double tDevAxis1, Double tDevAxis2, Double tDevAxis3,
                              Double tDevLevela, Double tDevLevelb, Double tDevLevelc,
                              Double tAvgDevAxis, Double tAvgDevLevel,
                              String centerTempData, String memo) {
        this.id = id;
        this.calibrationDate = calibrationDate;
        this.calibrationType = calibrationType;
        this.apparatusId = apparatusId;
        this.operator = operator;
        this.temperatureData = temperatureData;
        this.uniformityResult = uniformityResult;
        this.maxDeviation = maxDeviation;
        this.averageTemperature = averageTemperature;
        this.passedCriteria = passedCriteria;
        this.remarks = remarks;
        this.createdAt = createdAt;
        this.tempA1 = tempA1;
        this.tempA2 = tempA2;
        this.tempA3 = tempA3;
        this.tempB1 = tempB1;
        this.tempB2 = tempB2;
        this.tempB3 = tempB3;
        this.tempC1 = tempC1;
        this.tempC2 = tempC2;
        this.tempC3 = tempC3;
        this.tAvg = tAvg;
        this.tAvgAxis1 = tAvgAxis1;
        this.tAvgAxis2 = tAvgAxis2;
        this.tAvgAxis3 = tAvgAxis3;
        this.tAvgLevela = tAvgLevela;
        this.tAvgLevelb = tAvgLevelb;
        this.tAvgLevelc = tAvgLevelc;
        this.tDevAxis1 = tDevAxis1;
        this.tDevAxis2 = tDevAxis2;
        this.tDevAxis3 = tDevAxis3;
        this.tDevLevela = tDevLevela;
        this.tDevLevelb = tDevLevelb;
        this.tDevLevelc = tDevLevelc;
        this.tAvgDevAxis = tAvgDevAxis;
        this.tAvgDevLevel = tAvgDevLevel;
        this.centerTempData = centerTempData;
        this.memo = memo;
    }

    // ==================== Getter / Setter ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCalibrationDate() { return calibrationDate; }
    public void setCalibrationDate(String calibrationDate) { this.calibrationDate = calibrationDate; }

    public String getCalibrationType() { return calibrationType; }
    public void setCalibrationType(String calibrationType) { this.calibrationType = calibrationType; }

    public Integer getApparatusId() { return apparatusId; }
    public void setApparatusId(Integer apparatusId) { this.apparatusId = apparatusId; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getTemperatureData() { return temperatureData; }
    public void setTemperatureData(String temperatureData) { this.temperatureData = temperatureData; }

    public Double getUniformityResult() { return uniformityResult; }
    public void setUniformityResult(Double uniformityResult) { this.uniformityResult = uniformityResult; }

    public Double getMaxDeviation() { return maxDeviation; }
    public void setMaxDeviation(Double maxDeviation) { this.maxDeviation = maxDeviation; }

    public Double getAverageTemperature() { return averageTemperature; }
    public void setAverageTemperature(Double averageTemperature) { this.averageTemperature = averageTemperature; }

    public Integer getPassedCriteria() { return passedCriteria; }
    public void setPassedCriteria(Integer passedCriteria) { this.passedCriteria = passedCriteria; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Double getTempA1() { return tempA1; }
    public void setTempA1(Double tempA1) { this.tempA1 = tempA1; }

    public Double getTempA2() { return tempA2; }
    public void setTempA2(Double tempA2) { this.tempA2 = tempA2; }

    public Double getTempA3() { return tempA3; }
    public void setTempA3(Double tempA3) { this.tempA3 = tempA3; }

    public Double getTempB1() { return tempB1; }
    public void setTempB1(Double tempB1) { this.tempB1 = tempB1; }

    public Double getTempB2() { return tempB2; }
    public void setTempB2(Double tempB2) { this.tempB2 = tempB2; }

    public Double getTempB3() { return tempB3; }
    public void setTempB3(Double tempB3) { this.tempB3 = tempB3; }

    public Double getTempC1() { return tempC1; }
    public void setTempC1(Double tempC1) { this.tempC1 = tempC1; }

    public Double getTempC2() { return tempC2; }
    public void setTempC2(Double tempC2) { this.tempC2 = tempC2; }

    public Double getTempC3() { return tempC3; }
    public void setTempC3(Double tempC3) { this.tempC3 = tempC3; }

    public Double gettAvg() { return tAvg; }
    public void settAvg(Double tAvg) { this.tAvg = tAvg; }

    public Double gettAvgAxis1() { return tAvgAxis1; }
    public void settAvgAxis1(Double tAvgAxis1) { this.tAvgAxis1 = tAvgAxis1; }

    public Double gettAvgAxis2() { return tAvgAxis2; }
    public void settAvgAxis2(Double tAvgAxis2) { this.tAvgAxis2 = tAvgAxis2; }

    public Double gettAvgAxis3() { return tAvgAxis3; }
    public void settAvgAxis3(Double tAvgAxis3) { this.tAvgAxis3 = tAvgAxis3; }

    public Double gettAvgLevela() { return tAvgLevela; }
    public void settAvgLevela(Double tAvgLevela) { this.tAvgLevela = tAvgLevela; }

    public Double gettAvgLevelb() { return tAvgLevelb; }
    public void settAvgLevelb(Double tAvgLevelb) { this.tAvgLevelb = tAvgLevelb; }

    public Double gettAvgLevelc() { return tAvgLevelc; }
    public void settAvgLevelc(Double tAvgLevelc) { this.tAvgLevelc = tAvgLevelc; }

    public Double gettDevAxis1() { return tDevAxis1; }
    public void settDevAxis1(Double tDevAxis1) { this.tDevAxis1 = tDevAxis1; }

    public Double gettDevAxis2() { return tDevAxis2; }
    public void settDevAxis2(Double tDevAxis2) { this.tDevAxis2 = tDevAxis2; }

    public Double gettDevAxis3() { return tDevAxis3; }
    public void settDevAxis3(Double tDevAxis3) { this.tDevAxis3 = tDevAxis3; }

    public Double gettDevLevela() { return tDevLevela; }
    public void settDevLevela(Double tDevLevela) { this.tDevLevela = tDevLevela; }

    public Double gettDevLevelb() { return tDevLevelb; }
    public void settDevLevelb(Double tDevLevelb) { this.tDevLevelb = tDevLevelb; }

    public Double gettDevLevelc() { return tDevLevelc; }
    public void settDevLevelc(Double tDevLevelc) { this.tDevLevelc = tDevLevelc; }

    public Double gettAvgDevAxis() { return tAvgDevAxis; }
    public void settAvgDevAxis(Double tAvgDevAxis) { this.tAvgDevAxis = tAvgDevAxis; }

    public Double gettAvgDevLevel() { return tAvgDevLevel; }
    public void settAvgDevLevel(Double tAvgDevLevel) { this.tAvgDevLevel = tAvgDevLevel; }

    public String getCenterTempData() { return centerTempData; }
    public void setCenterTempData(String centerTempData) { this.centerTempData = centerTempData; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    // ==================== Object 重写 ====================

    @Override
    public String toString() {
        return new StringJoiner(", ", "CalibrationRecords[", "]")
                .add("id='" + id + "'")
                .add("calibrationDate='" + calibrationDate + "'")
                .add("calibrationType='" + calibrationType + "'")
                .add("apparatusId=" + apparatusId)
                .add("operator='" + operator + "'")
                .add("passedCriteria=" + passedCriteria)
                .add("tAvg=" + tAvg)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CalibrationRecords that)) return false;
        return Objects.equals(id, that.id)
            && Objects.equals(calibrationDate, that.calibrationDate)
            && Objects.equals(calibrationType, that.calibrationType)
            && Objects.equals(apparatusId, that.apparatusId)
            && Objects.equals(operator, that.operator)
            && Objects.equals(temperatureData, that.temperatureData)
            && Objects.equals(uniformityResult, that.uniformityResult)
            && Objects.equals(maxDeviation, that.maxDeviation)
            && Objects.equals(averageTemperature, that.averageTemperature)
            && Objects.equals(passedCriteria, that.passedCriteria)
            && Objects.equals(remarks, that.remarks)
            && Objects.equals(createdAt, that.createdAt)
            && Objects.equals(tempA1, that.tempA1)
            && Objects.equals(tempA2, that.tempA2)
            && Objects.equals(tempA3, that.tempA3)
            && Objects.equals(tempB1, that.tempB1)
            && Objects.equals(tempB2, that.tempB2)
            && Objects.equals(tempB3, that.tempB3)
            && Objects.equals(tempC1, that.tempC1)
            && Objects.equals(tempC2, that.tempC2)
            && Objects.equals(tempC3, that.tempC3)
            && Objects.equals(tAvg, that.tAvg)
            && Objects.equals(tAvgAxis1, that.tAvgAxis1)
            && Objects.equals(tAvgAxis2, that.tAvgAxis2)
            && Objects.equals(tAvgAxis3, that.tAvgAxis3)
            && Objects.equals(tAvgLevela, that.tAvgLevela)
            && Objects.equals(tAvgLevelb, that.tAvgLevelb)
            && Objects.equals(tAvgLevelc, that.tAvgLevelc)
            && Objects.equals(tDevAxis1, that.tDevAxis1)
            && Objects.equals(tDevAxis2, that.tDevAxis2)
            && Objects.equals(tDevAxis3, that.tDevAxis3)
            && Objects.equals(tDevLevela, that.tDevLevela)
            && Objects.equals(tDevLevelb, that.tDevLevelb)
            && Objects.equals(tDevLevelc, that.tDevLevelc)
            && Objects.equals(tAvgDevAxis, that.tAvgDevAxis)
            && Objects.equals(tAvgDevLevel, that.tAvgDevLevel)
            && Objects.equals(centerTempData, that.centerTempData)
            && Objects.equals(memo, that.memo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, calibrationDate, calibrationType, apparatusId, operator,
                temperatureData, uniformityResult, maxDeviation, averageTemperature,
                passedCriteria, remarks, createdAt,
                tempA1, tempA2, tempA3, tempB1, tempB2, tempB3, tempC1, tempC2, tempC3,
                tAvg, tAvgAxis1, tAvgAxis2, tAvgAxis3,
                tAvgLevela, tAvgLevelb, tAvgLevelc,
                tDevAxis1, tDevAxis2, tDevAxis3,
                tDevLevela, tDevLevelb, tDevLevelc,
                tAvgDevAxis, tAvgDevLevel, centerTempData, memo);
    }
}