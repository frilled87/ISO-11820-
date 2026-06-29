package com.iso11820.dao.impl;

import com.iso11820.dao.BaseDao;
import com.iso11820.dao.DaoException;
import com.iso11820.dao.TestMasterDao;
import com.iso11820.dao.util.DbUtil;
import com.iso11820.entity.TestMaster;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * 试验记录数据访问实现 —— 纯 JDBC 操作 {@code testmaster} 表（核心表）⭐。
 * <p>
 * 联合主键为 {@code (productid, testid)}，外键 productid 引用 productmaster.productid。
 * 新建试验时统计字段全部填 0，试验完成后通过 updateResult 更新。
 * </p>
 *
 * <h3>保存保护机制</h3>
 * <p>
 * 当 totaltesttime > 0 且 flag != '10000000' 时，表示存在已完成但未保存的试验，
 * UI 层应阻止新建试验和开始记录操作。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public class TestMasterDaoImpl extends BaseDao<TestMaster> implements TestMasterDao {

    /**
     * 无参构造 —— 泛型参数自动推断为 TestMaster。
     */
    public TestMasterDaoImpl() {
        super();
    }

    /**
     * 新建试验记录（初始插入，统计字段均为默认值 0）。
     * <p>
     * 插入时除基本信息字段外，温度统计、质量、时长等字段全部填 0。
     * 试验完成后通过 {@link #updateResult(TestMaster)} 更新实际统计值。
     * </p>
     *
     * @param tm 试验记录实体（仅基本信息字段有效）
     * @return 受影响的行数
     */
    @Override
    public int insert(TestMaster tm) {
        String sql = "INSERT INTO testmaster ("
                + "productid, testid, testdate, ambtemp, ambhumi, according, "
                + "operator, apparatusid, apparatusname, apparatuschkdate, rptno, "
                + "preweight, postweight, lostweight, lostweight_per, "
                + "totaltesttime, constpower, phenocode, flametime, flameduration, "
                + "maxtf1, maxtf2, maxts, maxtc, "
                + "maxtf1_time, maxtf2_time, maxts_time, maxtc_time, "
                + "finaltf1, finaltf2, finalts, finaltc, "
                + "finaltf1_time, finaltf2_time, finalts_time, finaltc_time, "
                + "deltatf1, deltatf2, deltatf, deltats, deltatc, "
                + "memo, flag"
                + ") VALUES ("
                + "?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, "
                + "?, ?, ?, ?, "
                + "?, ?, ?, ?, "
                + "?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, "
                + "?, ?"
                + ")";
        return executeUpdate(sql,
                // 基本信息
                tm.getProductid(), tm.getTestid(), tm.getTestdate(),
                tm.getAmbtemp(), tm.getAmbhumi(), tm.getAccording(),
                tm.getOperator(), tm.getApparatusid(), tm.getApparatusname(),
                tm.getApparatuschkdate(), tm.getRptno(),
                // 质量数据（初始为 0）
                tm.getPreweight() != null ? tm.getPreweight() : 0.0,
                0.0,  // postweight
                0.0,  // lostweight
                0.0,  // lostweight_per
                // 试验过程（初始为 0）
                0,    // totaltesttime
                0,    // constpower
                "",   // phenocode
                0,    // flametime
                0,    // flameduration
                // 温度最大值
                0.0, 0.0, 0.0, 0.0,
                0, 0, 0, 0,
                // 温度最终值
                0.0, 0.0, 0.0, 0.0,
                0, 0, 0, 0,
                // 温升
                0.0, 0.0, 0.0, 0.0, 0.0,
                // 备注
                tm.getMemo(), tm.getFlag());
    }

    /**
     * 按联合主键查询单条试验详情。
     *
     * @param productid 样品编号
     * @param testid    试验ID
     * @return 试验记录实体，不存在时返回 null
     */
    @Override
    public TestMaster getByKey(String productid, String testid) {
        String sql = "SELECT * FROM testmaster WHERE productid = ? AND testid = ?";
        return queryOne(sql, productid, testid);
    }

    /**
     * 试验完成后更新结果字段。
     * <p>
     * 更新项包括：试验后质量、失重量、失重率、综合温升、
     * 总时长、恒功率值、现象编码、火焰信息、各通道温度最大值及出现时刻、
     * 温度最终值、各通道温升、保存标记 flag 等全部统计字段。
     * </p>
     *
     * @param tm 包含完整统计数据的试验记录实体
     * @return 受影响的行数
     */
    @Override
    public int updateResult(TestMaster tm) {
        String sql = "UPDATE testmaster SET "
                // 质量数据
                + "postweight = ?, lostweight = ?, lostweight_per = ?, "
                // 试验过程
                + "totaltesttime = ?, constpower = ?, phenocode = ?, "
                + "flametime = ?, flameduration = ?, "
                // 温度最大值
                + "maxtf1 = ?, maxtf2 = ?, maxts = ?, maxtc = ?, "
                + "maxtf1_time = ?, maxtf2_time = ?, maxts_time = ?, maxtc_time = ?, "
                // 温度最终值
                + "finaltf1 = ?, finaltf2 = ?, finalts = ?, finaltc = ?, "
                + "finaltf1_time = ?, finaltf2_time = ?, finalts_time = ?, finaltc_time = ?, "
                // 温升
                + "deltatf1 = ?, deltatf2 = ?, deltatf = ?, deltats = ?, deltatc = ?, "
                // 备注与标记
                + "memo = ?, flag = ? "
                + "WHERE productid = ? AND testid = ?";
        return executeUpdate(sql,
                // 质量数据
                tm.getPostweight(), tm.getLostweight(), tm.getLostweightPer(),
                // 试验过程
                tm.getTotaltesttime(), tm.getConstpower(), tm.getPhenocode(),
                tm.getFlametime(), tm.getFlameduration(),
                // 温度最大值
                tm.getMaxtf1(), tm.getMaxtf2(), tm.getMaxts(), tm.getMaxtc(),
                tm.getMaxtf1Time(), tm.getMaxtf2Time(), tm.getMaxtsTime(), tm.getMaxtcTime(),
                // 温度最终值
                tm.getFinaltf1(), tm.getFinaltf2(), tm.getFinalts(), tm.getFinaltc(),
                tm.getFinaltf1Time(), tm.getFinaltf2Time(), tm.getFinaltsTime(), tm.getFinaltcTime(),
                // 温升
                tm.getDeltatf1(), tm.getDeltatf2(), tm.getDeltatf(), tm.getDeltats(), tm.getDeltatc(),
                // 备注与标记
                tm.getMemo(), tm.getFlag(),
                // WHERE 条件
                tm.getProductid(), tm.getTestid());
    }

    /**
     * 查询是否存在已完成但未保存的试验。
     * <p>
     * 判定条件：totaltesttime > 0 且 flag != '10000000'（或 flag IS NULL）。
     * 存在未保存试验时，UI 应阻止新建试验和开始记录。
     * </p>
     *
     * @return true 如果存在未保存的已完成试验
     */
    @Override
    public boolean hasUnfinishedTest() {
        // 命中索引：PK_testmaster (productid, testid) → 全表扫描，但通常数据量小
        String sql = "SELECT COUNT(*) FROM testmaster "
                   + "WHERE totaltesttime > 0 AND (flag IS NULL OR flag != '10000000')";
        Object result = queryScalar(sql);
        if (result instanceof Number num) {
            return num.longValue() > 0;
        }
        return false;
    }

    // ==================== 业务扩展方法 ====================

    /**
     * 多条件分页查询试验记录。
     * <p>
     * 动态构建 WHERE 条件，按 testdate 倒序排列。
     * 优先利用 IX_Testmaster_Testdate 索引（日期范围查询），
     * 操作员筛选命中 IX_Testmaster_Operator 索引。
     * 高选择性条件（日期范围）在前，低选择性条件（模糊匹配）在后。
     * </p>
     *
     * @param startDate        开始日期（含），可为 null
     * @param endDate          结束日期（含），可为 null
     * @param productIdKeyword 样品编号关键字，可为 null
     * @param operator         操作员用户名，可为 null
     * @param pageNum          页码（从 1 开始）
     * @param pageSize         每页记录数
     * @return 试验记录列表
     * @throws IllegalArgumentException 如果分页参数无效
     */
    @Override
    public List<TestMaster> queryByCondition(String startDate, String endDate,
                                              String productIdKeyword, String operator,
                                              int pageNum, int pageSize) {
        if (pageNum < 1) {
            throw new IllegalArgumentException("页码必须 >= 1，当前值: " + pageNum);
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("每页记录数必须 >= 1，当前值: " + pageSize);
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM testmaster WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 1. 日期范围（高选择性，最先添加，命中 IX_Testmaster_Testdate）
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" AND testdate >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" AND testdate <= ?");
            params.add(endDate);
        }

        // 2. 操作员精确筛选（命中 IX_Testmaster_Operator）
        if (operator != null && !operator.isBlank()) {
            sql.append(" AND operator = ?");
            params.add(operator);
        }

        // 3. 样品编号模糊匹配（低选择性，放最后）
        if (productIdKeyword != null && !productIdKeyword.isBlank()) {
            sql.append(" AND productid LIKE ?");
            params.add("%" + productIdKeyword + "%");
        }

        // 排序 + 分页
        sql.append(" ORDER BY testdate DESC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((pageNum - 1) * pageSize);

        return executeQuery(sql.toString(), params.toArray());
    }

    /**
     * 查询未保存的试验记录。
     * <p>
     * 业务保证同时最多只有一条未保存的试验记录，LIMIT 1 提前终止扫描。
     * </p>
     *
     * @return 未保存的试验记录，不存在时返回 null
     */
    @Override
    public TestMaster getUnfinishedTest() {
        // 命中索引：扫描 testmaster 表，LIMIT 1 提前终止
        String sql = "SELECT * FROM testmaster "
                   + "WHERE totaltesttime > 0 AND (flag IS NULL OR flag != '10000000') "
                   + "LIMIT 1";
        return queryOne(sql);
    }

    /**
     * 单独更新试验完成标记。
     * <p>
     * 操作精确命中联合主键 (productid, testid)，无需扫描全表。
     * </p>
     *
     * @param productid 样品编号
     * @param testid    试验ID
     * @param flag      完成标记值
     * @return 受影响的行数
     */
    @Override
    public int updateFlag(String productid, String testid, String flag) {
        // 命中索引：PK_testmaster (productid, testid) — 联合主键直接定位
        String sql = "UPDATE testmaster SET flag = ? WHERE productid = ? AND testid = ?";
        return executeUpdate(sql, flag, productid, testid);
    }

    /**
     * 统计指定日期范围内的试验数量。
     * <p>
     * 利用 IX_Testmaster_Testdate 索引进行范围统计。
     * </p>
     *
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 试验数量
     */
    @Override
    public long countByDateRange(String startDate, String endDate) {
        // 命中索引：IX_Testmaster_Testdate — 索引范围扫描
        String sql = "SELECT COUNT(*) FROM testmaster WHERE testdate BETWEEN ? AND ?";
        Object result = queryScalar(sql, startDate, endDate);
        if (result instanceof Number num) {
            return num.longValue();
        }
        return 0L;
    }

    /**
     * 事务版试验结果更新。
     * <p>
     * 使用显式事务管理，确保所有字段原子性更新。
     * 失败时自动回滚，保证数据一致性。事务内复用同一个 Connection。
     * </p>
     *
     * @param tm 包含完整统计数据的试验记录实体
     * @return true 如果更新成功并提交，false 如果回滚
     */
    @Override
    public boolean updateResultWithTransaction(TestMaster tm) {
        // 命中索引：PK_testmaster (productid, testid) — 联合主键直接定位
        String sql = "UPDATE testmaster SET "
                // 质量数据
                + "postweight = ?, lostweight = ?, lostweight_per = ?, "
                // 试验过程
                + "totaltesttime = ?, constpower = ?, phenocode = ?, "
                + "flametime = ?, flameduration = ?, "
                // 温度最大值
                + "maxtf1 = ?, maxtf2 = ?, maxts = ?, maxtc = ?, "
                + "maxtf1_time = ?, maxtf2_time = ?, maxts_time = ?, maxtc_time = ?, "
                // 温度最终值
                + "finaltf1 = ?, finaltf2 = ?, finalts = ?, finaltc = ?, "
                + "finaltf1_time = ?, finaltf2_time = ?, finalts_time = ?, finaltc_time = ?, "
                // 温升
                + "deltatf1 = ?, deltatf2 = ?, deltatf = ?, deltats = ?, deltatc = ?, "
                // 备注与标记
                + "memo = ?, flag = ? "
                + "WHERE productid = ? AND testid = ?";

        Connection conn = null;
        try {
            conn = DbUtil.beginTransaction();
            // 使用原始 JDBC 执行更新（复用事务连接）
            java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
            try {
                int idx = 1;
                // 质量数据
                pstmt.setObject(idx++, tm.getPostweight());
                pstmt.setObject(idx++, tm.getLostweight());
                pstmt.setObject(idx++, tm.getLostweightPer());
                // 试验过程
                pstmt.setObject(idx++, tm.getTotaltesttime());
                pstmt.setObject(idx++, tm.getConstpower());
                pstmt.setObject(idx++, tm.getPhenocode());
                pstmt.setObject(idx++, tm.getFlametime());
                pstmt.setObject(idx++, tm.getFlameduration());
                // 温度最大值
                pstmt.setObject(idx++, tm.getMaxtf1());
                pstmt.setObject(idx++, tm.getMaxtf2());
                pstmt.setObject(idx++, tm.getMaxts());
                pstmt.setObject(idx++, tm.getMaxtc());
                pstmt.setObject(idx++, tm.getMaxtf1Time());
                pstmt.setObject(idx++, tm.getMaxtf2Time());
                pstmt.setObject(idx++, tm.getMaxtsTime());
                pstmt.setObject(idx++, tm.getMaxtcTime());
                // 温度最终值
                pstmt.setObject(idx++, tm.getFinaltf1());
                pstmt.setObject(idx++, tm.getFinaltf2());
                pstmt.setObject(idx++, tm.getFinalts());
                pstmt.setObject(idx++, tm.getFinaltc());
                pstmt.setObject(idx++, tm.getFinaltf1Time());
                pstmt.setObject(idx++, tm.getFinaltf2Time());
                pstmt.setObject(idx++, tm.getFinaltsTime());
                pstmt.setObject(idx++, tm.getFinaltcTime());
                // 温升
                pstmt.setObject(idx++, tm.getDeltatf1());
                pstmt.setObject(idx++, tm.getDeltatf2());
                pstmt.setObject(idx++, tm.getDeltatf());
                pstmt.setObject(idx++, tm.getDeltats());
                pstmt.setObject(idx++, tm.getDeltatc());
                // 备注与标记
                pstmt.setObject(idx++, tm.getMemo());
                pstmt.setObject(idx++, tm.getFlag());
                // WHERE 条件
                pstmt.setObject(idx++, tm.getProductid());
                pstmt.setObject(idx++, tm.getTestid());

                pstmt.executeUpdate();
            } finally {
                DbUtil.closeResource(pstmt);
            }
            DbUtil.commitTransaction(conn);
            return true;
        } catch (Exception e) {
            DbUtil.rollbackTransaction(conn);
            throw new DaoException("事务更新试验结果失败: productid="
                    + tm.getProductid() + ", testid=" + tm.getTestid(), e);
        } finally {
            DbUtil.closeTransactionConnection(conn);
        }
    }
}