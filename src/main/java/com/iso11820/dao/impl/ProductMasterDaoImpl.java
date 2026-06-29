package com.iso11820.dao.impl;

import com.iso11820.dao.BaseDao;
import com.iso11820.dao.ProductMasterDao;
import com.iso11820.entity.ProductMaster;

import java.util.List;

/**
 * 样品数据访问实现 —— 纯 JDBC 操作 {@code productmaster} 表。
 * <p>
 * 主键为 {@code productid}，提供增删改查和模糊搜索功能。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class ProductMasterDaoImpl extends BaseDao<ProductMaster> implements ProductMasterDao {

    /**
     * 无参构造 —— 泛型参数自动推断为 ProductMaster。
     */
    public ProductMasterDaoImpl() {
        super();
    }

    /**
     * 新增样品记录。
     *
     * @param product 样品实体
     * @return 受影响的行数
     */
    @Override
    public int insert(ProductMaster product) {
        String sql = "INSERT INTO productmaster (productid, productname, specific, diameter, height, flag) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        return executeUpdate(sql,
                product.getProductid(),
                product.getProductname(),
                product.getSpecific(),
                product.getDiameter(),
                product.getHeight(),
                product.getFlag());
    }

    /**
     * 更新样品信息。
     * <p>
     * 注意：productid 为主键不可变，仅更新其他字段。
     * </p>
     *
     * @param product 样品实体
     * @return 受影响的行数
     */
    @Override
    public int update(ProductMaster product) {
        String sql = "UPDATE productmaster SET productname = ?, specific = ?, "
                   + "diameter = ?, height = ?, flag = ? WHERE productid = ?";
        return executeUpdate(sql,
                product.getProductname(),
                product.getSpecific(),
                product.getDiameter(),
                product.getHeight(),
                product.getFlag(),
                product.getProductid());
    }

    /**
     * 按样品编号删除样品。
     *
     * @param productId 样品编号
     * @return 受影响的行数
     */
    @Override
    public int deleteById(String productId) {
        String sql = "DELETE FROM productmaster WHERE productid = ?";
        return executeUpdate(sql, productId);
    }

    /**
     * 按样品编号查询样品。
     *
     * @param productId 样品编号
     * @return 样品实体，不存在时返回 null
     */
    @Override
    public ProductMaster getById(String productId) {
        String sql = "SELECT * FROM productmaster WHERE productid = ?";
        return queryOne(sql, productId);
    }

    /**
     * 按样品编号或名称模糊查询。
     * <p>
     * 对 productid 和 productname 两个字段分别进行 LIKE 匹配，
     * 关键字前后自动添加 % 通配符。
     * </p>
     *
     * @param keyword 查询关键字
     * @return 匹配的样品列表，无结果时返回空列表
     */
    @Override
    public List<ProductMaster> fuzzySearch(String keyword) {
        String sql = "SELECT * FROM productmaster WHERE productid LIKE ? OR productname LIKE ?";
        String pattern = "%" + keyword + "%";
        return executeQuery(sql, pattern, pattern);
    }

    /**
     * 查询所有样品列表。
     *
     * @return 样品列表，无数据时返回空列表
     */
    @Override
    public List<ProductMaster> listAll() {
        String sql = "SELECT * FROM productmaster";
        return executeQuery(sql);
    }
}