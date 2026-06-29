package com.iso11820.dao.support;

import com.iso11820.dao.dto.CalibrationDTO;
import com.iso11820.dao.dto.SensorDataDTO;
import com.iso11820.dao.dto.TestResultDTO;
import com.iso11820.entity.CalibrationRecords;
import com.iso11820.entity.Sensors;
import com.iso11820.entity.TestMaster;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 实体转换工具类 —— 数据库实体与业务 DTO 的双向转换。
 * <p>
 * 所有方法均为静态方法，线程安全，空值安全。
 * 数据层通过此工具类与业务层完全解耦——数据层只依赖 DTO 结构，
 * 不依赖核心层具体实现。后续核心层接入时，接口保持不变。
 * </p>
 *
 * <h3>转换方向</h3>
 * <ul>
 *   <li>Entity → DTO：DAO 查询结果 → 业务可读对象</li>
 *   <li>DTO → Entity：业务参数 → 数据库实体（供 DAO 写入）</li>
 * </ul>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public final class EntityConverter {

    private EntityConverter() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== TestMaster ↔ TestResultDTO ====================

    /**
     * 将 TestMaster 实体转换为 TestResultDTO。
     * <p>
     * 空值安全：入参为 null 返回 null，缺失字段给默认值 0。
     * </p>
     *
     * @param tm 试验记录实体
     * @return 试验结果 DTO，入参为 null 时返回 null
     */
    public static TestResultDTO toTestResultDTO(TestMaster tm) {
        if (tm == null) {
            return null;
        }
        TestResultDTO dto = new TestResultDTO();
        dto.setProductId(tm.getProductid());
        dto.setTestId(tm.getTestid());
        dto.setMaxTf1(nvl(tm.getMaxtf1()));
        dto.setMaxTf1Time(nvl(tm.getMaxtf1Time()));
        dto.setMaxTf2(nvl(tm.getMaxtf2()));
        dto.setMaxTf2Time(nvl(tm.getMaxtf2Time()));
        dto.setMaxTs(nvl(tm.getMaxts()));
        dto.setMaxTsTime(nvl(tm.getMaxtsTime()));
        dto.setMaxTc(nvl(tm.getMaxtc()));
        dto.setMaxTcTime(nvl(tm.getMaxtcTime()));
        dto.setMinTf1(nvl(tm.getMaxtf1()));  // 注：TestMaster 实体无 min 字段，后续可扩展
        dto.setMinTf2(nvl(tm.getMaxtf2()));
        dto.setMinTs(nvl(tm.getMaxts()));
        dto.setMinTc(nvl(tm.getMaxtc()));
        dto.setFinalTf1(nvl(tm.getFinaltf1()));
        dto.setFinalTf2(nvl(tm.getFinaltf2()));
        dto.setFinalTs(nvl(tm.getFinalts()));
        dto.setFinalTc(nvl(tm.getFinaltc()));
        dto.setDeltaTf1(nvl(tm.getDeltatf1()));
        dto.setDeltaTf2(nvl(tm.getDeltatf2()));
        dto.setDeltaTf(nvl(tm.getDeltatf()));
        dto.setDeltaTs(nvl(tm.getDeltats()));
        dto.setDeltaTc(nvl(tm.getDeltatc()));
        dto.setAvgTf1(nvl(tm.getMaxtf1()));   // 注：TestMaster 实体无 avg 字段，后续可扩展
        dto.setAvgTf2(nvl(tm.getMaxtf2()));
        dto.setAvgTs(nvl(tm.getMaxts()));
        dto.setAvgTc(nvl(tm.getMaxtc()));
        dto.setTotalRecordTime(nvl(tm.getTotaltesttime()));
        dto.setConstantPower(nvl(tm.getConstpower()));
        dto.setPreWeight(nvl(tm.getPreweight()));
        dto.setPostWeight(nvl(tm.getPostweight()));
        dto.setLostWeightPer(nvl(tm.getLostweightPer()));
        dto.setFlameTime(nvl(tm.getFlametime()));
        dto.setFlameDuration(nvl(tm.getFlameduration()));
        dto.setPhenocode(tm.getPhenocode());
        dto.setMemo(tm.getMemo());
        dto.setFlag(tm.getFlag());
        return dto;
    }

    /**
     * 将 TestResultDTO 转换为 TestMaster 实体（用于新建试验）。
     * <p>
     * 仅填充基本信息字段，统计字段由试验完成后单独更新。
     * </p>
     *
     * @param dto 试验结果 DTO
     * @return TestMaster 实体，入参为 null 时返回 null
     */
    public static TestMaster toTestMaster(TestResultDTO dto) {
        if (dto == null) {
            return null;
        }
        TestMaster tm = new TestMaster();
        tm.setProductid(dto.getProductId());
        tm.setTestid(dto.getTestId());
        tm.setPostweight(dto.getPostWeight());
        tm.setTotaltesttime(dto.getTotalRecordTime());
        tm.setConstpower(dto.getConstantPower());
        tm.setPhenocode(dto.getPhenocode());
        tm.setFlametime(dto.getFlameTime());
        tm.setFlameduration(dto.getFlameDuration());
        tm.setMaxtf1(dto.getMaxTf1());
        tm.setMaxtf1Time(dto.getMaxTf1Time());
        tm.setMaxtf2(dto.getMaxTf2());
        tm.setMaxtf2Time(dto.getMaxTf2Time());
        tm.setMaxts(dto.getMaxTs());
        tm.setMaxtsTime(dto.getMaxTsTime());
        tm.setMaxtc(dto.getMaxTc());
        tm.setMaxtcTime(dto.getMaxTcTime());
        tm.setFinaltf1(dto.getFinalTf1());
        tm.setFinaltf2(dto.getFinalTf2());
        tm.setFinalts(dto.getFinalTs());
        tm.setFinaltc(dto.getFinalTc());
        tm.setDeltatf1(dto.getDeltaTf1());
        tm.setDeltatf2(dto.getDeltaTf2());
        tm.setDeltatf(dto.getDeltaTf());
        tm.setDeltats(dto.getDeltaTs());
        tm.setDeltatc(dto.getDeltaTc());
        tm.setMemo(dto.getMemo());
        tm.setFlag(dto.getFlag());
        return tm;
    }

    /**
     * 将 TestMaster 列表批量转换为 TestResultDTO 列表。
     *
     * @param list 试验记录实体列表
     * @return DTO 列表，入参为 null 时返回空列表
     */
    public static List<TestResultDTO> toTestResultDTOList(List<TestMaster> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(EntityConverter::toTestResultDTO).collect(Collectors.toList());
    }

    // ==================== Sensors ↔ SensorDataDTO ====================

    /**
     * 将 Sensors 实体转换为 SensorDataDTO。
     *
     * @param s 传感器实体
     * @return 传感器数据 DTO，入参为 null 时返回 null
     */
    public static SensorDataDTO toSensorDataDTO(Sensors s) {
        if (s == null) {
            return null;
        }
        return new SensorDataDTO(
                s.getOutputvalue(),  // 炉温1 → tf1
                null,                // 炉温2：由核心层从 Sensor1 获取
                null,                // 表面温：由核心层从 Sensor2 获取
                null,                // 中心温：由核心层从 Sensor3 获取
                null                 // 校准温：由核心层从 Sensor16 获取
        );
    }

    /**
     * 从传感器列表构造完整的 5 通道 SensorDataDTO。
     * <p>
     * 核心层每 800ms 调用一次，从 5 个核心传感器通道（0/1/2/3/16）
     * 的 outputvalue 组装为完整的温度快照。
     * </p>
     *
     * @param sensorList 传感器列表（需包含 sensorid 0,1,2,3,16）
     * @return 5 通道温度快照 DTO，缺失通道用 0.0 填充
     */
    public static SensorDataDTO toSensorDataDTO(List<Sensors> sensorList) {
        if (sensorList == null || sensorList.isEmpty()) {
            return new SensorDataDTO(0.0, 0.0, 0.0, 0.0, 0.0);
        }
        Double tf1 = 0.0, tf2 = 0.0, ts = 0.0, tc = 0.0, tCal = 0.0;
        for (Sensors s : sensorList) {
            if (s.getSensorid() == null) continue;
            Double val = nvl(s.getOutputvalue());
            switch (s.getSensorid()) {
                case 0:  tf1 = val;  break;
                case 1:  tf2 = val;  break;
                case 2:  ts  = val;  break;
                case 3:  tc  = val;  break;
                case 16: tCal = val; break;
            }
        }
        return new SensorDataDTO(tf1, tf2, ts, tc, tCal);
    }

    // ==================== CalibrationRecords ↔ CalibrationDTO ====================

    /**
     * 将 CalibrationRecords 实体转换为 CalibrationDTO。
     * <p>
     * 注意：TemperatureData 和 CenterTempData 的 JSON 反序列化
     * 由 DAO 层的 getByIdWithJson() 方法处理，此处仅传递原始字符串。
     * </p>
     *
     * @param r 校准记录实体
     * @return 校准 DTO，入参为 null 时返回 null
     */
    public static CalibrationDTO toCalibrationDTO(CalibrationRecords r) {
        if (r == null) {
            return null;
        }
        CalibrationDTO dto = new CalibrationDTO();
        dto.setId(r.getId());
        dto.setCalibrationDate(r.getCalibrationDate());
        dto.setCalibrationType(r.getCalibrationType());
        dto.setApparatusId(r.getApparatusId());
        dto.setOperator(r.getOperator());
        dto.setUniformityResult(r.getUniformityResult());
        dto.setMaxDeviation(r.getMaxDeviation());
        dto.setAverageTemperature(r.getAverageTemperature());
        dto.setPassedCriteria(r.getPassedCriteria());
        dto.setRemarks(r.getRemarks());
        // temperatureData Map 由调用方通过 getByIdWithJson 后自行处理
        return dto;
    }

    /**
     * 将 CalibrationRecords 列表批量转换为 CalibrationDTO 列表。
     *
     * @param list 校准记录实体列表
     * @return DTO 列表，入参为 null 时返回空列表
     */
    public static List<CalibrationDTO> toCalibrationDTOList(List<CalibrationRecords> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(EntityConverter::toCalibrationDTO).collect(Collectors.toList());
    }

    // ==================== 内部工具方法 ====================

    /**
     * null 安全转换：null → 0。
     */
    private static Double nvl(Double value) {
        return value != null ? value : 0.0;
    }

    /**
     * null 安全转换：null → 0。
     */
    private static Integer nvl(Integer value) {
        return value != null ? value : 0;
    }
}