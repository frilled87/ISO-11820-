package com.iso11820.dao.impl;

import com.iso11820.dao.BaseDao;
import com.iso11820.dao.SensorsDao;
import com.iso11820.entity.Sensors;

import java.util.ArrayList;
import java.util.List;

/**
 * 传感器数据访问实现 —— 纯 JDBC 操作 {@code sensors} 表。
 * <p>
 * 提供传感器配置查询和实时温度值更新功能。
 * 业务核心使用通道 0（炉温1）、1（炉温2）、2（表面温度）、3（中心温度）、16（校准温度）。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class SensorsDaoImpl extends BaseDao<Sensors> implements SensorsDao {

    /**
     * 无参构造 —— 泛型参数自动推断为 Sensors。
     */
    public SensorsDaoImpl() {
        super();
    }

    /**
     * 按分组查询传感器列表。
     *
     * @param sensorgroup 分组标识（采集/校准/备用）
     * @return 传感器列表，无结果时返回空列表
     */
    @Override
    public List<Sensors> listByGroup(String sensorgroup) {
        String sql = "SELECT * FROM sensors WHERE sensorgroup = ? ORDER BY sensorid";
        return executeQuery(sql, sensorgroup);
    }

    /**
     * 按通道ID查询传感器配置。
     *
     * @param sensorId 传感器ID
     * @return 传感器实体，不存在时返回 null
     */
    @Override
    public Sensors getById(int sensorId) {
        String sql = "SELECT * FROM sensors WHERE sensorid = ?";
        return queryOne(sql, sensorId);
    }

    /**
     * 更新传感器实时温度值。
     * <p>
     * 仿真运行时每 800ms 调用一次，同步更新当前温度值（outputvalue）
     * 和当前输入值（inputvalue）。
     * </p>
     *
     * @param sensorId    传感器ID
     * @param outputValue 当前温度值（°C），可为 null
     * @param inputValue  当前输入值，可为 null
     * @return 受影响的行数
     */
    @Override
    public int updateOutputValue(int sensorId, Double outputValue, Double inputValue) {
        String sql = "UPDATE sensors SET outputvalue = ?, inputvalue = ? WHERE sensorid = ?";
        return executeUpdate(sql, outputValue, inputValue, sensorId);
    }

    /**
     * 批量更新多个传感器的实时温度值。
     * <p>
     * 使用 BaseDao.executeBatch() 批量提交，一次数据库往返完成所有更新。
     * 仿真引擎高频更新（每 800ms 5 个通道）时，批量更新比逐条更新减少约 80% 的数据库开销。
     * </p>
     *
     * @param sensorList 传感器列表（仅需 sensorid、outputvalue、inputvalue 字段有值）
     * @return 每个传感器更新受影响的行数数组
     */
    @Override
    public int[] batchUpdateOutputValue(List<Sensors> sensorList) {
        if (sensorList == null || sensorList.isEmpty()) {
            return new int[0];
        }
        String sql = "UPDATE sensors SET outputvalue = ?, inputvalue = ? WHERE sensorid = ?";
        List<Object[]> paramsList = new ArrayList<>();
        for (Sensors s : sensorList) {
            paramsList.add(new Object[]{
                    s.getOutputvalue(),
                    s.getInputvalue(),
                    s.getSensorid()
            });
        }
        return executeBatch(sql, paramsList);
    }
}