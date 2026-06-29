package com.iso11820.dao;

import com.iso11820.dao.impl.CalibrationRecordsDaoImpl;
import com.iso11820.entity.CalibrationRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CalibrationRecordsDao 单元测试。
 * <p>
 * 覆盖：新增、查询、按日期范围查询、JSON 序列化写入/反序列化查询、
 * 按设备/操作员查询、最新记录查询。
 * </p>
 */
@DisplayName("CalibrationRecordsDao 测试")
class CalibrationRecordsDaoTest extends BaseDaoTest {

    private CalibrationRecordsDao dao;

    @BeforeEach
    void setUp() {
        dao = new CalibrationRecordsDaoImpl();
    }

    @Test
    @DisplayName("新增校准记录")
    void testInsert() {
        CalibrationRecords r = createRecord("CAL-001");
        int rows = dao.insert(r);
        assertEquals(1, rows);

        CalibrationRecords result = dao.getById("CAL-001");
        assertNotNull(result);
        assertEquals("Surface", result.getCalibrationType());
        assertEquals(0, result.getApparatusId());
        assertEquals("admin", result.getOperator());
    }

    @Test
    @DisplayName("按ID查询不存在的记录")
    void testGetByIdNotFound() {
        CalibrationRecords r = dao.getById("NONEXISTENT");
        assertNull(r);
    }

    @Test
    @DisplayName("按日期范围查询")
    void testListByDateRange() {
        CalibrationRecords r1 = createRecord("CAL-001");
        r1.setCalibrationDate("2026-06-01T10:00:00");
        dao.insert(r1);

        CalibrationRecords r2 = createRecord("CAL-002");
        r2.setCalibrationDate("2026-06-15T10:00:00");
        dao.insert(r2);

        CalibrationRecords r3 = createRecord("CAL-003");
        r3.setCalibrationDate("2026-06-30T10:00:00");
        dao.insert(r3);

        List<CalibrationRecords> list = dao.listByDateRange(
                "2026-06-01", "2026-06-15");
        assertNotNull(list);
        assertTrue(list.size() >= 2);
    }

    @Test
    @DisplayName("JSON 序列化写入")
    void testInsertWithJson() {
        CalibrationRecords r = createRecord("CAL-JSON-001");

        Map<String, Double> tempData = new HashMap<>();
        tempData.put("A1", 750.0);
        tempData.put("A2", 749.5);
        tempData.put("B1", 750.2);
        tempData.put("B2", 749.8);

        Map<String, Double> centerData = new HashMap<>();
        centerData.put("C1", 700.0);
        centerData.put("C2", 698.5);

        int rows = dao.insertWithJson(r, tempData, centerData);
        assertEquals(1, rows);

        CalibrationRecords result = dao.getById("CAL-JSON-001");
        assertNotNull(result.getTemperatureData());
        assertTrue(result.getTemperatureData().contains("A1"));
        assertTrue(result.getTemperatureData().contains("B1"));
        assertNotNull(result.getCenterTempData());
        assertTrue(result.getCenterTempData().contains("C1"));
    }

    @Test
    @DisplayName("JSON 序列化写入：空 Map")
    void testInsertWithJsonEmptyMap() {
        CalibrationRecords r = createRecord("CAL-JSON-002");
        int rows = dao.insertWithJson(r, null, null);
        assertEquals(1, rows);

        CalibrationRecords result = dao.getById("CAL-JSON-002");
        assertNotNull(result);
    }

    @Test
    @DisplayName("JSON 反序列化查询")
    void testGetByIdWithJson() {
        CalibrationRecords r = createRecord("CAL-JSON-003");
        Map<String, Double> tempData = new HashMap<>();
        tempData.put("A1", 750.0);
        dao.insertWithJson(r, tempData, null);

        CalibrationRecords result = dao.getByIdWithJson("CAL-JSON-003");
        assertNotNull(result);
        assertNotNull(result.getTemperatureData());
        // JSON 字符串应存在
        assertTrue(result.getTemperatureData().contains("750.0"));
    }

    @Test
    @DisplayName("按设备ID查询校准历史")
    void testListByApparatusId() {
        CalibrationRecords r1 = createRecord("CAL-APP-001");
        r1.setApparatusId(0);
        dao.insert(r1);

        CalibrationRecords r2 = createRecord("CAL-APP-002");
        r2.setApparatusId(0);
        dao.insert(r2);

        List<CalibrationRecords> list = dao.listByApparatusId(0);
        assertTrue(list.size() >= 2);
    }

    @Test
    @DisplayName("按设备ID查询：无记录")
    void testListByApparatusIdNotFound() {
        List<CalibrationRecords> list = dao.listByApparatusId(999);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("查询最新校准记录")
    void testGetLatestByApparatusId() {
        CalibrationRecords r1 = createRecord("CAL-LATEST-001");
        r1.setCalibrationDate("2026-06-01T10:00:00");
        dao.insert(r1);

        CalibrationRecords r2 = createRecord("CAL-LATEST-002");
        r2.setCalibrationDate("2026-06-30T10:00:00");
        dao.insert(r2);

        CalibrationRecords latest = dao.getLatestByApparatusId(0);
        assertNotNull(latest);
        assertEquals("CAL-LATEST-002", latest.getId());
        assertTrue(latest.getCalibrationDate().contains("2026-06-30"));
    }

    @Test
    @DisplayName("按操作员查询")
    void testListByOperator() {
        CalibrationRecords r1 = createRecord("CAL-OP-001");
        r1.setOperator("admin");
        dao.insert(r1);

        CalibrationRecords r2 = createRecord("CAL-OP-002");
        r2.setOperator("admin");
        dao.insert(r2);

        List<CalibrationRecords> list = dao.listByOperator("admin");
        assertTrue(list.size() >= 2);
    }

    @Test
    @DisplayName("按操作员查询：无记录")
    void testListByOperatorNotFound() {
        List<CalibrationRecords> list = dao.listByOperator("ghost");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private CalibrationRecords createRecord(String id) {
        CalibrationRecords r = new CalibrationRecords();
        r.setId(id);
        r.setCalibrationDate("2026-06-29T10:00:00");
        r.setCalibrationType("Surface");
        r.setApparatusId(0);
        r.setOperator("admin");
        r.setTemperatureData("{}");
        r.setUniformityResult(0.5);
        r.setMaxDeviation(1.2);
        r.setAverageTemperature(750.0);
        r.setPassedCriteria(1);
        r.setRemarks("测试");
        r.setCreatedAt("2026-06-29T10:00:00");
        // 炉壁9测温点
        r.setTempA1(750.0); r.setTempA2(749.5); r.setTempA3(750.2);
        r.setTempB1(749.8); r.setTempB2(750.0); r.setTempB3(750.1);
        r.setTempC1(750.2); r.setTempC2(749.9); r.setTempC3(749.8);
        // 计算结果
        r.settAvg(750.0);
        r.settAvgAxis1(750.0); r.settAvgAxis2(749.8); r.settAvgAxis3(750.0);
        r.settAvgLevela(750.0); r.settAvgLevelb(750.0); r.settAvgLevelc(750.0);
        r.settDevAxis1(0.1); r.settDevAxis2(0.2); r.settDevAxis3(0.1);
        r.settDevLevela(0.2); r.settDevLevelb(0.1); r.settDevLevelc(0.2);
        r.settAvgDevAxis(0.15);
        r.settAvgDevLevel(0.17);
        r.setCenterTempData(null);
        r.setMemo(null);
        return r;
    }
}