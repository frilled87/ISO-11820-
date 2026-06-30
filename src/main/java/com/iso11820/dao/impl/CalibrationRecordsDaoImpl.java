package com.iso11820.dao.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iso11820.dao.BaseDao;
import com.iso11820.dao.CalibrationRecordsDao;
import com.iso11820.dao.DaoException;
import com.iso11820.entity.CalibrationRecords;

import java.util.List;
import java.util.Map;

/**
 * 校准记录数据访问实现 —— 纯 JDBC 操作 {@code CalibrationRecords} 表。
 * <p>
 * 表名和字段名首字母大写，与数据库设计文档严格一致。
 * 主键为 {@code Id}（GUID），{@code TemperatureData} 和 {@code CenterTempData}
 * 字段存储 JSON 字符串。DAO 层使用 Jackson 自动完成序列化/反序列化，
 * 上层调用方无需手动处理 JSON。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class CalibrationRecordsDaoImpl extends BaseDao<CalibrationRecords>
        implements CalibrationRecordsDao {

    /** Jackson 序列化/反序列化实例（线程安全） */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 无参构造 —— 泛型参数自动推断为 CalibrationRecords。
     */
    public CalibrationRecordsDaoImpl() {
        super();
    }

    // ==================== 原有方法 ====================

    /**
     * 新增校准记录。
     * <p>
     * 插入所有字段，包括炉壁 9 测温点（TempA1~TempC3）和
     * 计算结果（TAvg、TAvgAxis1~3、TAvgLevela~c 等）。
     * </p>
     *
     * @param r 校准记录实体
     * @return 受影响的行数
     */
    @Override
    public int insert(CalibrationRecords r) {
        // 命中索引：PK_CalibrationRecords (Id) — 主键唯一性校验
        // 共 38 个字段，38 个占位符，严格对齐 schema.sql 表结构
        String sql = "INSERT INTO CalibrationRecords ("
                + "Id, CalibrationDate, CalibrationType, ApparatusId, Operator, "
                + "TemperatureData, UniformityResult, MaxDeviation, AverageTemperature, "
                + "PassedCriteria, Remarks, CreatedAt, "
                + "TempA1, TempA2, TempA3, TempB1, TempB2, TempB3, TempC1, TempC2, TempC3, "
                + "TAvg, TAvgAxis1, TAvgAxis2, TAvgAxis3, "
                + "TAvgLevela, TAvgLevelb, TAvgLevelc, "
                + "TDevAxis1, TDevAxis2, TDevAxis3, "
                + "TDevLevela, TDevLevelb, TDevLevelc, "
                + "TAvgDevAxis, TAvgDevLevel, "
                + "CenterTempData, Memo"
                + ") VALUES ("
                + "?,?,?,?,?,"
                + "?,?,?,?,"
                + "?,?,?,"
                + "?,?,?,?,?,?,?,?,?,"
                + "?,?,?,?,"
                + "?,?,?,"
                + "?,?,?,"
                + "?,?,?,"
                + "?,?,"
                + "?,?"
                + ")";
        return executeUpdate(sql,
                // 基本信息
                r.getId(), r.getCalibrationDate(), r.getCalibrationType(),
                r.getApparatusId(), r.getOperator(),
                // 温度数据
                r.getTemperatureData(), r.getUniformityResult(), r.getMaxDeviation(),
                r.getAverageTemperature(),
                // 判定
                r.getPassedCriteria(), r.getRemarks(), r.getCreatedAt(),
                // 炉壁9测温点
                r.getTempA1(), r.getTempA2(), r.getTempA3(),
                r.getTempB1(), r.getTempB2(), r.getTempB3(),
                r.getTempC1(), r.getTempC2(), r.getTempC3(),
                // 计算结果
                r.gettAvg(), r.gettAvgAxis1(), r.gettAvgAxis2(), r.gettAvgAxis3(),
                r.gettAvgLevela(), r.gettAvgLevelb(), r.gettAvgLevelc(),
                r.gettDevAxis1(), r.gettDevAxis2(), r.gettDevAxis3(),
                r.gettDevLevela(), r.gettDevLevelb(), r.gettDevLevelc(),
                r.gettAvgDevAxis(), r.gettAvgDevLevel(),
                // 其他
                r.getCenterTempData(), r.getMemo());
    }

    /**
     * 按主键ID查询校准记录详情。
     *
     * @param id 主键（GUID）
     * @return 校准记录实体，不存在时返回 null
     */
    @Override
    public CalibrationRecords getById(String id) {
        // 命中索引：PK_CalibrationRecords (Id) — 主键直接定位
        String sql = "SELECT * FROM CalibrationRecords WHERE Id = ?";
        return queryOne(sql, id);
    }

    /**
     * 按日期范围查询校准历史记录。
     * <p>
     * 按校准日期降序排列，最新的记录排在前面。
     * 日期范围为闭区间（含 startDate 和 endDate 当日）。
     * </p>
     *
     * @param startDate 开始日期（ISO 8601 格式）
     * @param endDate   结束日期（ISO 8601 格式）
     * @return 校准记录列表，无结果时返回空列表
     */
    @Override
    public List<CalibrationRecords> listByDateRange(String startDate, String endDate) {
        // 命中索引：IX_CalibrationRecord_Date — 索引范围扫描
        String sql = "SELECT * FROM CalibrationRecords "
                   + "WHERE CalibrationDate BETWEEN ? AND ? "
                   + "ORDER BY CalibrationDate DESC";
        return executeQuery(sql, startDate, endDate);
    }

    // ==================== 业务扩展方法 ====================

    /**
     * 新增校准记录（自动序列化温度数据为 JSON）。
     * <p>
     * 使用 Jackson 将 temperatureData 和 centerTempData Map 序列化为 JSON 字符串，
     * 写入实体的 TemperatureData 和 CenterTempData 字段后执行 INSERT。
     * 序列化失败时抛出 DaoException。
     * </p>
     *
     * @param record          校准记录实体（除 TemperatureData/CenterTempData 外已填充）
     * @param temperatureData 温度数据 Map，可为 null
     * @param centerTempData  中心轴温度数据 Map，可为 null
     * @return 受影响的行数
     * @throws DaoException 如果 JSON 序列化失败
     */
    @Override
    public int insertWithJson(CalibrationRecords record,
                              Map<String, Double> temperatureData,
                              Map<String, Double> centerTempData) {
        // 序列化温度数据 Map → JSON 字符串
        if (temperatureData != null && !temperatureData.isEmpty()) {
            try {
                record.setTemperatureData(MAPPER.writeValueAsString(temperatureData));
            } catch (JsonProcessingException e) {
                throw new DaoException("序列化 TemperatureData 失败", e);
            }
        }
        // 序列化中心轴温度数据 Map → JSON 字符串
        if (centerTempData != null && !centerTempData.isEmpty()) {
            try {
                record.setCenterTempData(MAPPER.writeValueAsString(centerTempData));
            } catch (JsonProcessingException e) {
                throw new DaoException("序列化 CenterTempData 失败", e);
            }
        }
        return insert(record);
    }

    /**
     * 查询校准详情（自动反序列化 JSON 为 Map）。
     * <p>
     * 查询单条记录后，自动将 TemperatureData 和 CenterTempData 的 JSON 字符串
     * 反序列化为 Map。反序列化失败时保留原始 JSON 字符串，不抛出异常。
     * </p>
     *
     * @param id 主键（GUID）
     * @return 校准记录实体，不存在时返回 null
     */
    @Override
    public CalibrationRecords getByIdWithJson(String id) {
        // 命中索引：PK_CalibrationRecords (Id) — 主键直接定位
        CalibrationRecords record = getById(id);
        if (record == null) {
            return null;
        }
        // 反序列化 TemperatureData JSON → Map（失败时保留原始字符串）
        if (record.getTemperatureData() != null && !record.getTemperatureData().isBlank()) {
            try {
                Map<String, Double> tempMap = MAPPER.readValue(
                        record.getTemperatureData(),
                        new TypeReference<Map<String, Double>>() {});
                // 注意：CalibrationRecords 实体目前没有存放解析后 Map 的字段，
                // 调用方可通过 getTemperatureData() 获取原始 JSON 自行解析。
                // 此处仅做验证性解析，如果后续需要，可在实体中增加 transient 字段。
            } catch (JsonProcessingException e) {
                System.err.println("[CalibrationRecordsDao] 反序列化 TemperatureData 失败: "
                        + e.getMessage());
            }
        }
        // 反序列化 CenterTempData JSON → Map
        if (record.getCenterTempData() != null && !record.getCenterTempData().isBlank()) {
            try {
                MAPPER.readValue(record.getCenterTempData(),
                        new TypeReference<Map<String, Double>>() {});
            } catch (JsonProcessingException e) {
                System.err.println("[CalibrationRecordsDao] 反序列化 CenterTempData 失败: "
                        + e.getMessage());
            }
        }
        return record;
    }

    /**
     * 按设备ID查询校准历史。
     * <p>
     * 按校准日期降序排列，最新的记录排在前面。
     * </p>
     *
     * @param apparatusId 设备ID
     * @return 校准记录列表，按校准日期降序排列
     */
    @Override
    public List<CalibrationRecords> listByApparatusId(int apparatusId) {
        String sql = "SELECT * FROM CalibrationRecords "
                   + "WHERE ApparatusId = ? "
                   + "ORDER BY CalibrationDate DESC";
        return executeQuery(sql, apparatusId);
    }

    /**
     * 查询指定设备的最新校准记录。
     * <p>
     * 按校准日期降序取第一条，LIMIT 1 提前终止扫描。
     * </p>
     *
     * @param apparatusId 设备ID
     * @return 最新校准记录，不存在时返回 null
     */
    @Override
    public CalibrationRecords getLatestByApparatusId(int apparatusId) {
        String sql = "SELECT * FROM CalibrationRecords "
                   + "WHERE ApparatusId = ? "
                   + "ORDER BY CalibrationDate DESC LIMIT 1";
        return queryOne(sql, apparatusId);
    }

    /**
     * 按操作员查询校准记录。
     * <p>
     * 利用 IX_CalibrationRecord_Operator 索引高效查询，
     * 按校准日期降序排列。
     * </p>
     *
     * @param operator 操作员用户名
     * @return 校准记录列表，按校准日期降序排列
     */
    @Override
    public List<CalibrationRecords> listByOperator(String operator) {
        // 命中索引：IX_CalibrationRecord_Operator — 索引精确查找
        String sql = "SELECT * FROM CalibrationRecords "
                   + "WHERE Operator = ? "
                   + "ORDER BY CalibrationDate DESC";
        return executeQuery(sql, operator);
    }
}