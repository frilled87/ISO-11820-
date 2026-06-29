package com.iso11820.dao;

import com.iso11820.dao.impl.ProductMasterDaoImpl;
import com.iso11820.dao.impl.TestMasterDaoImpl;
import com.iso11820.entity.ProductMaster;
import com.iso11820.entity.TestMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TestMasterDao 单元测试 —— 核心业务表。
 * <p>
 * 覆盖：新建试验、查询、更新结果、分页查询、
 * 未保存试验查询、更新标记、统计、事务回滚。
 * </p>
 */
@DisplayName("TestMasterDao 测试")
class TestMasterDaoTest extends BaseDaoTest {

    private TestMasterDao dao;
    private ProductMasterDao productDao;

    @BeforeEach
    void setUp() {
        dao = new TestMasterDaoImpl();
        productDao = new ProductMasterDaoImpl();
        // 插入测试用样品（外键依赖）
        ProductMaster p = new ProductMaster();
        p.setProductid("TEST-P001");
        p.setProductname("测试样品");
        p.setSpecific("100×50mm");
        p.setDiameter(100.0);
        p.setHeight(50.0);
        productDao.insert(p);
    }

    // ==================== 正常场景 ====================

    @Test
    @DisplayName("新建试验")
    void testInsert() {
        TestMaster tm = createTestRecord("TEST-P001", "20260629-120000");
        int rows = dao.insert(tm);
        assertEquals(1, rows);

        TestMaster result = dao.getByKey("TEST-P001", "20260629-120000");
        assertNotNull(result);
        assertEquals("TEST-P001", result.getProductid());
        assertEquals("admin", result.getOperator());
        assertEquals(0.0, result.getMaxtf1());
    }

    @Test
    @DisplayName("按联合主键查询")
    void testGetByKey() {
        dao.insert(createTestRecord("TEST-P001", "20260629-120000"));
        TestMaster tm = dao.getByKey("TEST-P001", "20260629-120000");
        assertNotNull(tm);
        assertEquals("20260629-120000", tm.getTestid());
    }

    @Test
    @DisplayName("查询不存在的试验记录")
    void testGetByKeyNotFound() {
        TestMaster tm = dao.getByKey("TEST-P001", "99999999-999999");
        assertNull(tm);
    }

    @Test
    @DisplayName("更新试验结果")
    void testUpdateResult() {
        dao.insert(createTestRecord("TEST-P001", "20260629-120000"));

        TestMaster update = new TestMaster();
        update.setProductid("TEST-P001");
        update.setTestid("20260629-120000");
        update.setPostweight(45.0);
        update.setLostweight(5.0);
        update.setLostweightPer(10.0);
        update.setTotaltesttime(3600);
        update.setConstpower(2048);
        update.setPhenocode("1,2,3");
        update.setFlametime(0);
        update.setFlameduration(0);
        update.setMaxtf1(750.5);
        update.setMaxtf1Time(7);
        update.setMaxtf2(749.8);
        update.setMaxtf2Time(10);
        update.setMaxts(620.0);
        update.setMaxtsTime(3600);
        update.setMaxtc(480.0);
        update.setMaxtcTime(3600);
        update.setFinaltf1(750.0);
        update.setFinaltf2(749.5);
        update.setFinalts(620.0);
        update.setFinaltc(480.0);
        update.setFinaltf1Time(3600);
        update.setFinaltf2Time(3600);
        update.setFinaltsTime(3600);
        update.setFinaltcTime(3600);
        update.setDeltatf1(25.0);
        update.setDeltatf2(24.5);
        update.setDeltatf(103.9);
        update.setDeltats(103.9);
        update.setDeltatc(80.0);
        update.setMemo("测试完成");
        update.setFlag("10000000");

        int rows = dao.updateResult(update);
        assertEquals(1, rows);

        TestMaster result = dao.getByKey("TEST-P001", "20260629-120000");
        assertEquals(45.0, result.getPostweight());
        assertEquals(3600, result.getTotaltesttime());
        assertEquals(750.5, result.getMaxtf1());
        assertEquals("10000000", result.getFlag());
    }

    // ==================== 业务扩展方法 ====================

    @Test
    @DisplayName("多条件分页查询")
    void testQueryByCondition() {
        // 插入多条测试数据
        dao.insert(createTestRecord("TEST-P001", "20260628-120000"));
        dao.insert(createTestRecord("TEST-P001", "20260629-120000"));
        dao.insert(createTestRecord("TEST-P001", "20260629-130000"));

        // 按日期范围查询
        List<TestMaster> list = dao.queryByCondition(
                "2026-06-28", "2026-06-29", null, null, 1, 10);
        assertNotNull(list);
        assertTrue(list.size() >= 2);

        // 按操作员筛选
        List<TestMaster> list2 = dao.queryByCondition(
                null, null, null, "admin", 1, 10);
        assertTrue(list2.size() >= 2);

        // 分页：第1页，每页1条
        List<TestMaster> page1 = dao.queryByCondition(
                null, null, null, null, 1, 1);
        assertEquals(1, page1.size());
    }

    @Test
    @DisplayName("多条件分页查询：无效分页参数")
    void testQueryByConditionInvalidParams() {
        assertThrows(IllegalArgumentException.class, () ->
                dao.queryByCondition(null, null, null, null, 0, 10));
        assertThrows(IllegalArgumentException.class, () ->
                dao.queryByCondition(null, null, null, null, 1, 0));
    }

    @Test
    @DisplayName("查询未保存的试验记录")
    void testGetUnfinishedTest() {
        // 插入一条未保存的记录
        TestMaster tm = createTestRecord("TEST-P001", "20260629-120000");
        dao.insert(tm);

        // 更新使 totaltesttime > 0
        TestMaster update = new TestMaster();
        update.setProductid("TEST-P001");
        update.setTestid("20260629-120000");
        update.setTotaltesttime(3600);
        update.setFlag(null); // 未保存
        dao.updateResult(update);

        TestMaster unfinished = dao.getUnfinishedTest();
        assertNotNull(unfinished, "应查询到未保存的试验");
        assertEquals("TEST-P001", unfinished.getProductid());
    }

    @Test
    @DisplayName("无未保存试验时返回 null")
    void testGetUnfinishedTestNone() {
        TestMaster unfinished = dao.getUnfinishedTest();
        assertNull(unfinished, "无未保存试验时应返回 null");
    }

    @Test
    @DisplayName("更新试验完成标记")
    void testUpdateFlag() {
        dao.insert(createTestRecord("TEST-P001", "20260629-120000"));

        int rows = dao.updateFlag("TEST-P001", "20260629-120000", "10000000");
        assertEquals(1, rows);

        TestMaster result = dao.getByKey("TEST-P001", "20260629-120000");
        assertEquals("10000000", result.getFlag());
    }

    @Test
    @DisplayName("统计日期范围内的试验数量")
    void testCountByDateRange() {
        dao.insert(createTestRecord("TEST-P001", "20260629-120000"));
        dao.insert(createTestRecord("TEST-P001", "20260629-130000"));

        long count = dao.countByDateRange("2026-06-29", "2026-06-29");
        assertTrue(count >= 2);
    }

    @Test
    @DisplayName("检查是否存在未保存试验")
    void testHasUnfinishedTest() {
        assertFalse(dao.hasUnfinishedTest(), "初始状态应无未保存试验");

        TestMaster tm = createTestRecord("TEST-P001", "20260629-120000");
        dao.insert(tm);

        TestMaster update = new TestMaster();
        update.setProductid("TEST-P001");
        update.setTestid("20260629-120000");
        update.setTotaltesttime(3600);
        update.setFlag(null);
        dao.updateResult(update);

        assertTrue(dao.hasUnfinishedTest(), "应存在未保存试验");
    }

    @Test
    @DisplayName("事务版更新结果：正常提交")
    void testUpdateResultWithTransactionSuccess() {
        dao.insert(createTestRecord("TEST-P001", "20260629-120000"));

        TestMaster update = new TestMaster();
        update.setProductid("TEST-P001");
        update.setTestid("20260629-120000");
        update.setPostweight(45.0);
        update.setLostweight(5.0);
        update.setLostweightPer(10.0);
        update.setTotaltesttime(3600);
        update.setConstpower(2048);
        update.setPhenocode("1");
        update.setFlametime(0);
        update.setFlameduration(0);
        update.setFlag("10000000");

        boolean success = dao.updateResultWithTransaction(update);
        assertTrue(success, "事务应成功提交");

        TestMaster result = dao.getByKey("TEST-P001", "20260629-120000");
        assertEquals("10000000", result.getFlag());
        assertEquals(3600, result.getTotaltesttime());
    }

    // ==================== 辅助方法 ====================

    private TestMaster createTestRecord(String productId, String testId) {
        TestMaster tm = new TestMaster();
        tm.setProductid(productId);
        tm.setTestid(testId);
        tm.setTestdate("2026-06-29");
        tm.setAmbtemp(25.0);
        tm.setAmbhumi(60.0);
        tm.setAccording("ISO 11820:2022");
        tm.setOperator("admin");
        tm.setApparatusid("FURNACE-01");
        tm.setApparatusname("一号试验炉");
        tm.setApparatuschkdate("2026-06-29");
        tm.setRptno(productId);
        tm.setPreweight(50.0);
        return tm;
    }
}