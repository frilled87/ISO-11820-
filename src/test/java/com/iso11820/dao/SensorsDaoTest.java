package com.iso11820.dao;

import com.iso11820.dao.impl.SensorsDaoImpl;
import com.iso11820.entity.Sensors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensorsDao 单元测试。
 */
@DisplayName("SensorsDao 测试")
class SensorsDaoTest extends BaseDaoTest {

    private SensorsDao dao;

    @BeforeEach
    void setUp() {
        dao = new SensorsDaoImpl();
    }

    @Test
    @DisplayName("按分组查询：采集通道")
    void testListByGroup() {
        List<Sensors> list = dao.listByGroup("采集");
        assertNotNull(list);
        assertFalse(list.isEmpty());
        // 核心通道：sensorid 0,1,2,3
        assertTrue(list.stream().anyMatch(s -> s.getSensorid() == 0));
        assertTrue(list.stream().anyMatch(s -> s.getSensorid() == 3));
    }

    @Test
    @DisplayName("按分组查询：校准通道")
    void testListByGroupCalibration() {
        List<Sensors> list = dao.listByGroup("校准");
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(16, list.get(0).getSensorid());
    }

    @Test
    @DisplayName("按分组查询：不存在的分组")
    void testListByGroupNotFound() {
        List<Sensors> list = dao.listByGroup("不存在的分组");
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("按ID查询传感器")
    void testGetById() {
        Sensors s = dao.getById(0);
        assertNotNull(s);
        assertEquals("炉温1", s.getDispname());
        assertEquals("采集", s.getSensorgroup());
    }

    @Test
    @DisplayName("更新实时温度值")
    void testUpdateOutputValue() {
        int rows = dao.updateOutputValue(0, 750.5, 32000.0);
        assertEquals(1, rows);

        Sensors s = dao.getById(0);
        assertEquals(750.5, s.getOutputvalue());
        assertEquals(32000.0, s.getInputvalue());
    }

    @Test
    @DisplayName("批量更新实时温度值")
    void testBatchUpdateOutputValue() {
        List<Sensors> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Sensors s = new Sensors();
            s.setSensorid(i);
            s.setOutputvalue(750.0 + i);
            s.setInputvalue(32000.0 + i * 100);
            list.add(s);
        }

        int[] results = dao.batchUpdateOutputValue(list);
        assertEquals(5, results.length);
        for (int r : results) {
            assertEquals(1, r, "每个传感器应更新一条记录");
        }

        // 验证
        Sensors s0 = dao.getById(0);
        assertEquals(750.0, s0.getOutputvalue());
        Sensors s3 = dao.getById(3);
        assertEquals(753.0, s3.getOutputvalue());
    }

    @Test
    @DisplayName("批量更新：空列表")
    void testBatchUpdateEmptyList() {
        int[] results = dao.batchUpdateOutputValue(new ArrayList<>());
        assertEquals(0, results.length);
    }
}