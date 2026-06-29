package com.iso11820.dao.impl;

import com.iso11820.dao.BaseDao;
import com.iso11820.dao.OperatorDao;
import com.iso11820.entity.Operators;

/**
 * 操作员数据访问实现 —— 纯 JDBC 操作 {@code operators} 表。
 * <p>
 * 注意：operators 表无主键约束，登录校验和查询均按 username 进行。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class OperatorDaoImpl extends BaseDao<Operators> implements OperatorDao {

    /**
     * 无参构造 —— 泛型参数自动推断为 Operators。
     */
    public OperatorDaoImpl() {
        super();
    }

    /**
     * 验证用户名密码是否正确。
     * <p>
     * 按 username + pwd 联合校验，与数据库设计文档中
     * "当前代码登录时查询 username + pwd，不是 userid + pwd" 一致。
     * </p>
     *
     * @param username 登录用户名
     * @param pwd      明文密码
     * @return true 如果用户名和密码匹配
     */
    @Override
    public boolean login(String username, String pwd) {
        String sql = "SELECT COUNT(*) FROM operators WHERE username = ? AND pwd = ?";
        Object result = queryScalar(sql, username, pwd);
        if (result instanceof Number num) {
            return num.longValue() > 0;
        }
        return false;
    }

    /**
     * 根据用户名查询完整用户信息。
     *
     * @param username 登录用户名
     * @return 用户实体，不存在时返回 null
     */
    @Override
    public Operators getByUsername(String username) {
        String sql = "SELECT * FROM operators WHERE username = ?";
        return queryOne(sql, username);
    }
}