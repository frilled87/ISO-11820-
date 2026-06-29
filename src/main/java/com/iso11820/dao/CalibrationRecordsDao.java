package com.iso11820.dao;

import com.iso11820.entity.CalibrationRecords;

import java.util.List;
import java.util.Map;

/**
 * 校准记录数据访问接口 —— 对应 {@code CalibrationRecords} 表。
 * <p>
 * 提供设备校准记录的创建、查询和按日期范围检索功能。
 * 表名和字段名首字母大写，与数据库设计文档严格一致。
 * 主键为 {@code Id}（GUID）。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public interface CalibrationRecordsDao {

    /**
     * 新增校准记录。
     *
     * @param record 校准记录实体
     * @return 受影响的行数
     */
    int insert(CalibrationRecords record);

    /**
     * 按主键ID查询校准记录详情。
     *
     * @param id 主键（GUID）
     * @return 校准记录实体，不存在时返回 null
     */
    CalibrationRecords getById(String id);

    /**
     * 按日期范围查询校准历史记录。
     * <p>
     * 按校准日期降序排列，最新的记录排在前面。
     * </p>
     *
     * @param startDate 开始日期（ISO 8601 格式，含当日）
     * @param endDate   结束日期（ISO 8601 格式，含当日）
     * @return 校准记录列表，无结果时返回空列表
     */
    List<CalibrationRecords> listByDateRange(String startDate, String endDate);

    // ==================== 业务扩展方法 ====================

    /**
     * 新增校准记录（自动序列化温度数据为 JSON）。
     * <p>
     * 自动将温度 Map 通过 Jackson 序列化为 JSON 字符串，
     * 存入 TemperatureData 和 CenterTempData 字段。
     * 上层调用方无需手动处理 JSON 序列化。
     * </p>
     *
     * @param record           校准记录实体（除 TemperatureData/CenterTempData 外已填充）
     * @param temperatureData  温度数据 Map（key=测温点名称, value=温度值），可为 null
     * @param centerTempData   中心轴温度数据 Map，可为 null
     * @return 受影响的行数
     */
    int insertWithJson(CalibrationRecords record,
                       Map<String, Double> temperatureData,
                       Map<String, Double> centerTempData);

    /**
     * 查询校准详情（自动反序列化 JSON 为 Map）。
     * <p>
     * 查询单条记录，自动将 TemperatureData 和 CenterTempData 的 JSON 字符串
     * 反序列化为 Map，存入实体的扩展字段。
     * </p>
     *
     * @param id 主键（GUID）
     * @return 校准记录实体，不存在时返回 null
     */
    CalibrationRecords getByIdWithJson(String id);

    /**
     * 按设备ID查询校准历史。
     *
     * @param apparatusId 设备ID
     * @return 校准记录列表，按校准日期降序排列
     */
    List<CalibrationRecords> listByApparatusId(int apparatusId);

    /**
     * 查询指定设备的最新校准记录。
     *
     * @param apparatusId 设备ID
     * @return 最新校准记录，不存在时返回 null
     */
    CalibrationRecords getLatestByApparatusId(int apparatusId);

    /**
     * 按操作员查询校准记录。
     * <p>
     * 利用 IX_CalibrationRecord_Operator 索引高效查询。
     * </p>
     *
     * @param operator 操作员用户名
     * @return 校准记录列表，按校准日期降序排列
     */
    List<CalibrationRecords> listByOperator(String operator);
}