package com.iso11820.dao;

import com.iso11820.entity.ProductMaster;

import java.util.List;

/**
 * 样品数据访问接口 —— 对应 {@code productmaster} 表。
 * <p>
 * 提供样品的增删改查和模糊搜索功能。
 * 主键为 {@code productid}。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public interface ProductMasterDao {

    /**
     * 新增样品记录。
     *
     * @param product 样品实体
     * @return 受影响的行数
     */
    int insert(ProductMaster product);

    /**
     * 更新样品信息。
     *
     * @param product 样品实体（productid 不可变）
     * @return 受影响的行数
     */
    int update(ProductMaster product);

    /**
     * 按样品编号删除样品。
     *
     * @param productId 样品编号
     * @return 受影响的行数
     */
    int deleteById(String productId);

    /**
     * 按样品编号查询样品。
     *
     * @param productId 样品编号
     * @return 样品实体，不存在时返回 null
     */
    ProductMaster getById(String productId);

    /**
     * 按样品编号或名称模糊查询。
     * <p>
     * 对 productid 和 productname 两个字段分别进行 LIKE 匹配。
     * </p>
     *
     * @param keyword 查询关键字
     * @return 匹配的样品列表，无结果时返回空列表
     */
    List<ProductMaster> fuzzySearch(String keyword);

    /**
     * 查询所有样品列表。
     *
     * @return 样品列表，无数据时返回空列表
     */
    List<ProductMaster> listAll();
}