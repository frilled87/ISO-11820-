package com.iso11820.dao.util;

import com.iso11820.dao.DaoException;
import com.iso11820.entity.TestMaster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 数据完整性校验工具类。
 * <p>
 * 提供实体存在性检查、字段合法性校验等静态方法。
 * 所有写入操作前应调用对应的校验方法，非法数据直接抛出 DaoException 拒绝写入。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public final class DaoChecker {

    private DaoChecker() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== 存在性校验 ====================

    /**
     * 校验样品编号是否存在于 productmaster 表中。
     * <p>
     * 新建试验前必须调用此方法，确保外键引用有效。
     * </p>
     *
     * @param productId 样品编号
     * @return true 如果样品存在
     * @throws DaoException 如果数据库查询失败
     */
    public static boolean checkProductExists(String productId) {
        if (productId == null || productId.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM productmaster WHERE productid = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DaoException("校验样品存在性失败: " + productId, e);
        }
        return false;
    }

    /**
     * 校验试验记录是否存在。
     *
     * @param productId 样品编号
     * @param testId    试验ID
     * @return true 如果试验记录已存在
     * @throws DaoException 如果数据库查询失败
     */
    public static boolean checkTestExists(String productId, String testId) {
        if (productId == null || productId.isBlank()
                || testId == null || testId.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM testmaster WHERE productid = ? AND testid = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            pstmt.setString(2, testId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DaoException("校验试验存在性失败: " + productId + "/" + testId, e);
        }
        return false;
    }

    // ==================== 字段合法性校验 ====================

    /**
     * 全字段合法性校验 —— 对 TestMaster 实体进行非空字段和数值范围检查。
     * <p>
     * 校验规则：
     * </p>
     * <ul>
     *   <li>productid、testid 不能为空</li>
     *   <li>ambtemp 范围 -50 ~ 100（°C）</li>
     *   <li>ambhumi 范围 0 ~ 100（%）</li>
     *   <li>preweight、postweight >= 0</li>
     *   <li>totaltesttime >= 0</li>
     *   <li>constpower 范围 0 ~ 25600</li>
     *   <li>温度值范围 -100 ~ 2000（°C）</li>
     *   <li>时间值 >= 0</li>
     * </ul>
     *
     * @param tm 试验记录实体
     * @throws DaoException 如果字段校验不通过
     */
    public static void validateTestMaster(TestMaster tm) {
        if (tm == null) {
            throw new DaoException("TestMaster 实体不能为 null");
        }

        // 必填字段
        requireNonBlank(tm.getProductid(), "productid");
        requireNonBlank(tm.getTestid(), "testid");

        // 环境参数范围
        if (tm.getAmbtemp() != null) {
            requireRange(tm.getAmbtemp(), -50.0, 100.0, "ambtemp（环境温度）");
        }
        if (tm.getAmbhumi() != null) {
            requireRange(tm.getAmbhumi(), 0.0, 100.0, "ambhumi（环境湿度）");
        }

        // 质量非负
        if (tm.getPreweight() != null) {
            requireNonNegative(tm.getPreweight(), "preweight（试验前质量）");
        }
        if (tm.getPostweight() != null) {
            requireNonNegative(tm.getPostweight(), "postweight（试验后质量）");
        }

        // 时长非负
        if (tm.getTotaltesttime() != null) {
            requireNonNegative(tm.getTotaltesttime().doubleValue(), "totaltesttime（总试验时长）");
        }

        // 恒功率范围
        if (tm.getConstpower() != null) {
            requireRange(tm.getConstpower().doubleValue(), 0.0, 25600.0, "constpower（恒功率值）");
        }

        // 温度值范围
        validateTemperature(tm.getMaxtf1(), "maxtf1");
        validateTemperature(tm.getMaxtf2(), "maxtf2");
        validateTemperature(tm.getMaxts(), "maxts");
        validateTemperature(tm.getMaxtc(), "maxtc");
        validateTemperature(tm.getFinaltf1(), "finaltf1");
        validateTemperature(tm.getFinaltf2(), "finaltf2");
        validateTemperature(tm.getFinalts(), "finalts");
        validateTemperature(tm.getFinaltc(), "finaltc");
        validateTemperature(tm.getDeltatf1(), "deltatf1");
        validateTemperature(tm.getDeltatf2(), "deltatf2");
        validateTemperature(tm.getDeltatf(), "deltatf");
        validateTemperature(tm.getDeltats(), "deltats");
        validateTemperature(tm.getDeltatc(), "deltatc");

        // 时间值非负
        if (tm.getMaxtf1Time() != null) requireNonNegative(tm.getMaxtf1Time(), "maxtf1Time");
        if (tm.getFlametime() != null) requireNonNegative(tm.getFlametime(), "flametime");
        if (tm.getFlameduration() != null) requireNonNegative(tm.getFlameduration(), "flameduration");
    }

    // ==================== 内部校验方法 ====================

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new DaoException("字段 " + fieldName + " 不能为空");
        }
    }

    private static void requireNonNegative(Number value, String fieldName) {
        if (value.doubleValue() < 0) {
            throw new DaoException("字段 " + fieldName + " 不能为负数，当前值: " + value);
        }
    }

    private static void requireRange(Number value, double min, double max, String fieldName) {
        double v = value.doubleValue();
        if (v < min || v > max) {
            throw new DaoException("字段 " + fieldName + " 超出范围 [" + min + ", " + max
                    + "]，当前值: " + v);
        }
    }

    private static void validateTemperature(Double value, String fieldName) {
        if (value != null) {
            requireRange(value, -100.0, 2000.0, fieldName + "（温度）");
        }
    }
}