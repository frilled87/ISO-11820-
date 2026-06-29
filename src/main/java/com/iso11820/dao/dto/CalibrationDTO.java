package com.iso11820.dao.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * 校准数据 DTO —— 数据层与业务层之间的校准记录数据载体。
 * <p>
 * 对应核心层校准业务对象，包含校准基本信息、温度数据 Map 和判定结果。
 * 纯数据对象，不依赖任何核心层实现。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class CalibrationDTO implements Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    /** 主键（GUID） */
    private String id;

    /** 校准日期时间 */
    private String calibrationDate;

    /** 校准类型：Surface 或 Center */
    private String calibrationType;

    /** 设备ID */
    private Integer apparatusId;

    /** 操作员 */
    private String operator;

    /** 温度数据（反序列化后的 Map） */
    private Map<String, Double> temperatureData;

    /** 中心轴温度数据（反序列化后的 Map） */
    private Map<String, Double> centerTempData;

    /** 均匀性结果 */
    private Double uniformityResult;

    /** 最大偏差 */
    private Double maxDeviation;

    /** 平均温度 */
    private Double averageTemperature;

    /** 是否通过：0=未通过，1=通过 */
    private Integer passedCriteria;

    /** 备注 */
    private String remarks;

    // ==================== 构造方法 ====================

    public CalibrationDTO() {}

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

    public Map<String, Double> getTemperatureData() { return temperatureData; }
    public void setTemperatureData(Map<String, Double> temperatureData) { this.temperatureData = temperatureData; }

    public Map<String, Double> getCenterTempData() { return centerTempData; }
    public void setCenterTempData(Map<String, Double> centerTempData) { this.centerTempData = centerTempData; }

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
}