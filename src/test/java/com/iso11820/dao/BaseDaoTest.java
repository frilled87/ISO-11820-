package com.iso11820.dao;

import com.iso11820.dao.util.DbUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * DAO 层单元测试基类。
 * <p>
 * 每个测试方法执行前自动创建独立的临时 SQLite 数据库，
 * 执行 schema.sql 建表 + 初始数据，测试完成后自动销毁，
 * 完全隔离，不污染本地数据库。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * class OperatorDaoTest extends BaseDaoTest {
 *     private OperatorDao dao;
 *
 *     @BeforeEach
 *     void setUp() {
 *         dao = new OperatorDaoImpl();
 *     }
 *
 *     @Test
 *     void testLogin() { ... }
 * }
 * }</pre>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-29
 */
public abstract class BaseDaoTest {

    /** 临时数据库文件路径 */
    protected Path tempDbPath;

    /**
     * 每个测试方法前：创建独立临时数据库 + 执行建表脚本。
     */
    @BeforeEach
    void initDatabase() throws IOException {
        // 创建临时文件
        tempDbPath = Files.createTempFile("iso11820-test-", ".db");
        // 设置 DbUtil 路径并强制重新初始化
        DbUtil.setDbPath(tempDbPath.toString());
        DbUtil.resetInitialization();
        // 触发初始化（建表 + 初始数据）
        DbUtil.getConnection(); // 内部自动调用 initDatabase()
        System.out.println("[Test] 临时数据库已创建: " + tempDbPath);
    }

    /**
     * 每个测试方法后：销毁临时数据库。
     */
    @AfterEach
    void destroyDatabase() throws IOException {
        DbUtil.resetInitialization();
        if (tempDbPath != null) {
            Files.deleteIfExists(tempDbPath);
            System.out.println("[Test] 临时数据库已删除: " + tempDbPath);
        }
    }
}