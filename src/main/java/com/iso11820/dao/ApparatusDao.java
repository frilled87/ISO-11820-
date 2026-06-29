package com.iso11820.dao;

import com.iso11820.entity.Apparatus;

import java.util.List;

/**
 * 设备数据访问接口 —— 对应 {@code apparatus} 表。
 * <p>
 * 提供设备信息查询和恒功率值更新功能。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public interface ApparatusDao {

    /**
     * 按设备ID查询设备信息。
     *
     * @param apparatusId 设备ID
     * @return 设备实体，不存在时返回 null
     */
    Apparatus getById(int apparatusId);

    /**
     * 查询所有设备列表。
     *
     * @return 设备列表，无数据时返回空列表
     */
    List<Apparatus> listAll();

    /**
     * 更新设备恒功率值。
     *
     * @param id          设备ID
     * @param constPower  恒功率值（0~25600），可为 null
     * @return 受影响的行数
     */
    int updateConstPower(int id, Integer constPower);
}