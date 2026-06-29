package com.iso11820.dao;

import com.iso11820.entity.Operators;

/**
 * 操作员数据访问接口 —— 对应 {@code operators} 表。
 * <p>
 * 提供登录验证和用户信息查询功能。
 * 注意：operators 表无主键约束，登录校验按 username + pwd 进行。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public interface OperatorDao {

    /**
     * 验证用户名密码是否正确。
     *
     * @param username 登录用户名
     * @param pwd      明文密码
     * @return true 如果用户名和密码匹配
     */
    boolean login(String username, String pwd);

    /**
     * 根据用户名查询完整用户信息。
     *
     * @param username 登录用户名
     * @return 用户实体，不存在时返回 null
     */
    Operators getByUsername(String username);
}