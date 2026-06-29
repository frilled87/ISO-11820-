package com.iso11820.dao;

import com.iso11820.dao.impl.ProductMasterDaoImpl;
import com.iso11820.entity.ProductMaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductMasterDao 单元测试。
 */
@DisplayName("ProductMasterDao 测试")
class ProductMasterDaoTest extends BaseDaoTest {

    private ProductMasterDao dao;

    @BeforeEach
    void setUp() {
        dao = new ProductMasterDaoImpl();
    }

    @Test
    @DisplayName("新增样品")
    void testInsert() {
        ProductMaster p = new ProductMaster();
        p.setProductid("P001");
        p.setProductname("岩棉隔热板");
        p.setSpecific("100×50×25mm");
        p.setDiameter(100.0);
        p.setHeight(50.0);
        p.setFlag(null);

        int rows = dao.insert(p);
        assertEquals(1, rows, "应插入一条记录");

        ProductMaster result = dao.getById("P001");
        assertNotNull(result, "应能查询到刚插入的样品");
        assertEquals("岩棉隔热板", result.getProductname());
    }

    @Test
    @DisplayName("更新样品信息")
    void testUpdate() {
        // 先插入
        ProductMaster p = new ProductMaster();
        p.setProductid("P002");
        p.setProductname("原名称");
        p.setSpecific("100×100mm");
        p.setDiameter(100.0);
        p.setHeight(100.0);
        dao.insert(p);

        // 更新
        p.setProductname("新名称");
        p.setDiameter(200.0);
        int rows = dao.update(p);
        assertEquals(1, rows);

        ProductMaster result = dao.getById("P002");
        assertEquals("新名称", result.getProductname());
        assertEquals(200.0, result.getDiameter());
    }

    @Test
    @DisplayName("删除样品")
    void testDeleteById() {
        ProductMaster p = new ProductMaster();
        p.setProductid("P003");
        p.setProductname("测试");
        p.setSpecific("10×10mm");
        p.setDiameter(10.0);
        p.setHeight(10.0);
        dao.insert(p);

        int rows = dao.deleteById("P003");
        assertEquals(1, rows);

        assertNull(dao.getById("P003"), "删除后应查询不到");
    }

    @Test
    @DisplayName("删除不存在的样品")
    void testDeleteByIdNotFound() {
        int rows = dao.deleteById("NONEXISTENT");
        assertEquals(0, rows, "删除不存在的记录应返回 0");
    }

    @Test
    @DisplayName("模糊搜索")
    void testFuzzySearch() {
        // 插入两条记录
        ProductMaster p1 = new ProductMaster();
        p1.setProductid("ROCK-001");
        p1.setProductname("岩棉板");
        p1.setSpecific("100×50mm");
        p1.setDiameter(100.0);
        p1.setHeight(50.0);
        dao.insert(p1);

        ProductMaster p2 = new ProductMaster();
        p2.setProductid("GLASS-001");
        p2.setProductname("玻璃棉");
        p2.setSpecific("200×100mm");
        p2.setDiameter(200.0);
        p2.setHeight(100.0);
        dao.insert(p2);

        List<ProductMaster> results = dao.fuzzySearch("岩棉");
        assertEquals(1, results.size());
        assertEquals("岩棉板", results.get(0).getProductname());

        List<ProductMaster> results2 = dao.fuzzySearch("001");
        assertEquals(2, results2.size());
    }

    @Test
    @DisplayName("查询所有样品")
    void testListAll() {
        ProductMaster p = new ProductMaster();
        p.setProductid("P004");
        p.setProductname("测试");
        p.setSpecific("10×10mm");
        p.setDiameter(10.0);
        p.setHeight(10.0);
        dao.insert(p);

        List<ProductMaster> list = dao.listAll();
        assertNotNull(list);
        assertFalse(list.isEmpty());
    }
}