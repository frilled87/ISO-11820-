package com.iso11820.dao.impl;

import com.iso11820.dao.ApparatusDao;
import com.iso11820.dao.BaseDao;
import com.iso11820.entity.Apparatus;

import java.util.List;

/**
 * 设备数据访问实现 —— 纯 JDBC 操作 {@code apparatus} 表。
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class ApparatusDaoImpl extends BaseDao<Apparatus> implements ApparatusDao {

    /**
     * 无参构造 —— 泛型参数自动推断为 Apparatus。
     */
    public ApparatusDaoImpl() {
        super();
    }

    /**
     * 按设备ID查询设备信息。
     *
     * @param apparatusId 设备ID
     * @return 设备实体，不存在时返回 null
     */
    @Override
    public Apparatus getById(int apparatusId) {
        String sql = "SELECT * FROM apparatus WHERE apparatusid = ?";
        return queryOne(sql, apparatusId);
    }

    /**
     * 查询所有设备列表。
     *
     * @return 设备列表，无数据时返回空列表
     */
    @Override
    public List<Apparatus> listAll() {
        String sql = "SELECT * FROM apparatus";
        return executeQuery(sql);
    }

    /**
     * 更新设备恒功率值。
     *
     * @param id         设备ID
     * @param constPower 恒功率值（0~25600），可为 null
     * @return 受影响的行数
     */
    @Override
    public int updateConstPower(int id, Integer constPower) {
        String sql = "UPDATE apparatus SET constpower = ? WHERE apparatusid = ?";
        return executeUpdate(sql, constPower, id);
    }
}