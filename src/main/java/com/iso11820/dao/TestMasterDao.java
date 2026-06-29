package com.iso11820.dao;

import com.iso11820.entity.TestMaster;

import java.util.List;

/**
 * 试验记录数据访问接口 —— 对应 {@code testmaster} 表（核心表）⭐。
 * <p>
 * 提供试验记录的创建、查询、结果更新和未保存检查功能。
 * 联合主键为 {@code (productid, testid)}。
 * </p>
 *
 * <h3>关键约定</h3>
 * <ul>
 *   <li>新建试验时统计字段全部填 0，试验完成后通过 {@link #updateResult(TestMaster)} 更新</li>
 *   <li>{@code flag = "10000000"} 表示试验记录已保存</li>
 *   <li>存在未保存的试验记录时，禁止新建试验或重新开始记录</li>
 * </ul>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public interface TestMasterDao {

    /**
     * 新建试验记录（初始插入，统计字段均为默认值）。
     * <p>
     * 插入时温度统计、质量等字段全部为 0，试验完成后通过
     * {@link #updateResult(TestMaster)} 更新实际统计值。
     * </p>
     *
     * @param testMaster 试验记录实体（仅基本信息字段有效）
     * @return 受影响的行数
     */
    int insert(TestMaster testMaster);

    /**
     * 按联合主键查询单条试验详情。
     *
     * @param productid 样品编号
     * @param testid    试验ID
     * @return 试验记录实体，不存在时返回 null
     */
    TestMaster getByKey(String productid, String testid);

    /**
     * 试验完成后更新结果字段。
     * <p>
     * 更新项包括：试验后质量、失重量、失重率、温升、总时长、
     * 温度最大值、最终值、恒功率值、现象编码、火焰信息、flag 标记等。
     * </p>
     *
     * @param testMaster 包含完整统计数据的试验记录实体
     * @return 受影响的行数
     */
    int updateResult(TestMaster testMaster);

    /**
     * 查询是否存在已完成但未保存的试验。
     * <p>
     * 判定条件：totaltesttime > 0 且 flag != '10000000'。
     * 存在未保存试验时，UI 应阻止新建试验和开始记录。
     * </p>
     *
     * @return true 如果存在未保存的已完成试验
     */
    boolean hasUnfinishedTest();

    // ==================== 业务扩展方法 ====================

    /**
     * 多条件分页查询试验记录。
     * <p>
     * 按日期范围（testdate）、样品编号模糊匹配、操作员精确筛选，
     * 按 testdate 倒序排列，支持分页。
     * 利用已建立的 IX_Testmaster_Testdate、IX_Testmaster_Operator 索引优化查询。
     * </p>
     *
     * @param startDate       开始日期（含），可为 null 表示不限制
     * @param endDate         结束日期（含），可为 null 表示不限制
     * @param productIdKeyword 样品编号关键字（模糊匹配），可为 null 表示不限制
     * @param operator        操作员用户名（精确匹配），可为 null 表示不限制
     * @param pageNum         页码（从 1 开始）
     * @param pageSize        每页记录数
     * @return 试验记录列表，无结果时返回空列表
     * @throws IllegalArgumentException 如果 pageNum < 1 或 pageSize < 1
     */
    List<TestMaster> queryByCondition(String startDate, String endDate,
                                      String productIdKeyword, String operator,
                                      int pageNum, int pageSize);

    /**
     * 查询未保存的试验记录。
     * <p>
     * 查询条件：totaltesttime > 0 AND flag != '10000000'。
     * 业务保证同时最多只有一条未保存的试验记录，返回第一条。
     * </p>
     *
     * @return 未保存的试验记录，不存在时返回 null
     */
    TestMaster getUnfinishedTest();

    /**
     * 单独更新试验完成标记。
     * <p>
     * 仅更新指定试验的 flag 字段，用于标记试验已保存（flag = "10000000"）。
     * 操作精确命中联合主键，无需扫描全表。
     * </p>
     *
     * @param productid 样品编号
     * @param testid    试验ID
     * @param flag      完成标记值（"10000000" 表示已保存）
     * @return 受影响的行数
     */
    int updateFlag(String productid, String testid, String flag);

    /**
     * 统计指定日期范围内的试验数量。
     * <p>
     * 利用 IX_Testmaster_Testdate 索引高效统计。
     * </p>
     *
     * @param startDate 开始日期（含），不可为 null
     * @param endDate   结束日期（含），不可为 null
     * @return 试验数量
     */
    long countByDateRange(String startDate, String endDate);

    /**
     * 事务版试验结果更新。
     * <p>
     * 更新试验结果时使用事务，确保所有字段原子性更新。
     * 失败时自动回滚，保证数据一致性。
     * </p>
     *
     * @param testMaster 包含完整统计数据的试验记录实体
     * @return true 如果更新成功并提交，false 如果回滚
     */
    boolean updateResultWithTransaction(TestMaster testMaster);
}