package com.iso11820.dao;

import com.iso11820.entity.Sensors;

import java.util.List;

/**
 * 传感器数据访问接口 —— 对应 {@code sensors} 表。
 * <p>
 * 提供传感器配置查询和实时温度值更新功能。
 * 业务核心使用通道 0（炉温1）、1（炉温2）、2（表面温度）、3（中心温度）、16（校准温度）。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public interface SensorsDao {

    /**
     * 按分组查询传感器列表。
     *
     * @param sensorgroup 分组标识（采集/校准/备用）
     * @return 传感器列表，无结果时返回空列表
     */
    List<Sensors> listByGroup(String sensorgroup);

    /**
     * 按通道ID查询传感器配置。
     *
     * @param sensorId 传感器ID
     * @return 传感器实体，不存在时返回 null
     */
    Sensors getById(int sensorId);

    /**
     * 更新传感器实时温度值。
     * <p>
     * 仿真运行时每 800ms 调用一次，更新当前温度值和输入值。
     * </p>
     *
     * @param sensorId    传感器ID
     * @param outputValue 当前温度值（°C），可为 null
     * @param inputValue  当前输入值，可为 null
     * @return 受影响的行数
     */
    int updateOutputValue(int sensorId, Double outputValue, Double inputValue);

    /**
     * 批量更新多个传感器的实时温度值。
     * <p>
     * 使用 JDBC batch 机制，一次性提交多个传感器的更新操作，
     * 大幅提升仿真引擎高频更新时的性能。所有更新在同一批次中执行。
     * </p>
     *
     * @param sensorList 传感器列表（仅需 sensorid、outputvalue、inputvalue 字段有值）
     * @return 每个传感器更新受影响的行数数组（与 sensorList 顺序对应）
     */
    int[] batchUpdateOutputValue(List<Sensors> sensorList);
}