package com.iso11820.dao;

import com.iso11820.dao.impl.OperatorDaoImpl;
import com.iso11820.entity.Operators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OperatorDao 单元测试。
 * <p>
 * 覆盖：登录成功、登录失败、按用户名查询。
 * </p>
 */
@DisplayName("OperatorDao 测试")
class OperatorDaoTest extends BaseDaoTest {

    private OperatorDao dao;

    @BeforeEach
    void setUp() {
        dao = new OperatorDaoImpl();
    }

    @Test
    @DisplayName("登录成功：admin/123456")
    void testLoginSuccess() {
        assertTrue(dao.login("admin", "123456"), "管理员应能正常登录");
        assertTrue(dao.login("experimenter", "123456"), "试验员应能正常登录");
    }

    @Test
    @DisplayName("登录失败：错误密码")
    void testLoginFailWrongPassword() {
        assertFalse(dao.login("admin", "wrong"), "错误密码应登录失败");
        assertFalse(dao.login("admin", ""), "空密码应登录失败");
        assertFalse(dao.login("admin", null), "null 密码应登录失败");
    }

    @Test
    @DisplayName("登录失败：不存在的用户")
    void testLoginFailUserNotFound() {
        assertFalse(dao.login("nonexistent", "123456"), "不存在的用户应登录失败");
    }

    @Test
    @DisplayName("按用户名查询：正常返回")
    void testGetByUsername() {
        Operators op = dao.getByUsername("admin");
        assertNotNull(op, "应能查询到 admin 用户");
        assertEquals("admin", op.getUsername());
        assertEquals("1", op.getUserid());
        assertEquals("admin", op.getUsertype());

        Operators op2 = dao.getByUsername("experimenter");
        assertNotNull(op2, "应能查询到 experimenter 用户");
        assertEquals("operator", op2.getUsertype());
    }

    @Test
    @DisplayName("按用户名查询：用户不存在返回 null")
    void testGetByUsernameNotFound() {
        Operators op = dao.getByUsername("ghost");
        assertNull(op, "不存在的用户应返回 null");
    }
}