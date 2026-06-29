package com.iso11820.dao;

import com.iso11820.dao.impl.ApparatusDaoImpl;
import com.iso11820.entity.Apparatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApparatusDao 单元测试。
 */
@DisplayName("ApparatusDao 测试")
class ApparatusDaoTest extends BaseDaoTest {

    private ApparatusDao dao;

    @BeforeEach
    void setUp() {
        dao = new ApparatusDaoImpl();
    }

    @Test
    @DisplayName("按ID查询设备")
    void testGetById() {
        Apparatus app = dao.getById(0);
        assertNotNull(app, "应能查询到默认设备");
        assertEquals("FURNACE-01", app.getInnernumber());
        assertEquals("一号试验炉", app.getApparatusname());
        assertEquals("COM9", app.getPidport());
    }

    @Test
    @DisplayName("按ID查询：不存在的设备返回 null")
    void testGetByIdNotFound() {
        Apparatus app = dao.getById(999);
        assertNull(app, "不存在的设备应返回 null");
    }

    @Test
    @DisplayName("查询所有设备列表")
    void testListAll() {
        List<Apparatus> list = dao.listAll();
        assertNotNull(list, "列表不应为 null");
        assertFalse(list.isEmpty(), "应至少有一条设备记录");
        assertEquals("FURNACE-01", list.get(0).getInnernumber());
    }

    @Test
    @DisplayName("更新恒功率值")
    void testUpdateConstPower() {
        int rows = dao.updateConstPower(0, 3000);
        assertEquals(1, rows, "应更新一条记录");

        Apparatus app = dao.getById(0);
        assertEquals(3000, app.getConstpower(), "恒功率值应已更新");
    }

    @Test
    @DisplayName("更新恒功率值为 null")
    void testUpdateConstPowerNull() {
        int rows = dao.updateConstPower(0, null);
        assertEquals(1, rows, "应更新一条记录");

        Apparatus app = dao.getById(0);
        assertNull(app.getConstpower(), "恒功率值应为 null");
    }
}