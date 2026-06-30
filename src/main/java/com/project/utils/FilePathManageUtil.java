package com.project.utils;

import com.project.service.CsvDataService;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * <h1>文件路径统一管理工具 — 线程安全单例</h1>
 *
 * <p>从 {@link AppConfig} 读取所有路径配置，统一封装项目中所有文件路径的拼接、
 * 创建、校验、删除操作。自动处理 Windows/Linux 路径分隔符差异。</p>
 *
 * <h2>路径约定</h2>
 * <pre>
 * {BaseDirectory}/
 * ├── TestData/{productId}/{testId}/sensor.csv    ← 试验时序数据
 * └── Reports/{testId}_报告.xlsx                   ← 导出报告
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   FilePathManageUtil fpm = FilePathManageUtil.getInstance();
 *
 *   // 获取试验 CSV 路径
 *   Path csv = fpm.getCsvPath("P001", "20260630-143000");
 *   // → ./ISO11820_Data/TestData/P001/20260630-143000/sensor.csv
 *
 *   // 确保目录存在
 *   fpm.ensureTestDataDirs("P001", "20260630-143000");
 *
 *   // 获取报告路径
 *   Path report = fpm.getReportPath("20260630-143000", "xlsx");
 *   // → ./ISO11820_Data/Reports/20260630-143000_报告.xlsx
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 2.0
 * @since 2026-06-30
 */
public final class FilePathManageUtil {

    private static volatile FilePathManageUtil INSTANCE;

    /** 日志（使用 FILE 模块专用 logger） */
    private final Logger log = LogUtil.getLogger("FILE");

    /** 配置单例 */
    private final AppConfig config;

    /** CSV 文件名 */
    private static final String CSV_FILE_NAME = "sensor.csv";

    /** 报告文件名后缀 */
    private static final String REPORT_SUFFIX = "_报告";

    /** Excel 报告扩展名 */
    private static final String EXCEL_EXT = "xlsx";

    /** PDF 报告扩展名 */
    private static final String PDF_EXT = "pdf";

    // ============================================================
    //  单例
    // ============================================================

    private FilePathManageUtil() {
        this.config = AppConfig.getInstance();
    }

    /**
     * 获取单例实例。
     * @return 全局唯一的 FilePathManageUtil 实例
     */
    public static FilePathManageUtil getInstance() {
        if (INSTANCE == null) {
            synchronized (FilePathManageUtil.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FilePathManageUtil();
                }
            }
        }
        return INSTANCE;
    }

    // ============================================================
    //  基础路径获取
    // ============================================================

    /**
     * 获取文件存储根目录（绝对路径）。
     * 将配置中的相对路径转为基于当前工作目录的绝对路径。
     *
     * @return 根目录 Path
     */
    public Path getBaseDir() {
        return toAbsolutePath(config.getBaseDirectory());
    }

    /**
     * 获取试验数据根目录。
     * @return TestData 目录 Path
     */
    public Path getTestDataRoot() {
        return toAbsolutePath(config.getTestDataDirectory());
    }

    /**
     * 获取报告输出根目录。
     * @return Reports 目录 Path
     */
    public Path getReportRoot() {
        return toAbsolutePath(config.getReportOutputDirectory());
    }

    /**
     * 获取数据库文件完整路径。
     * @return 数据库文件 Path
     */
    public Path getDatabasePath() {
        return toAbsolutePath(config.getDatabasePath());
    }

    /**
     * 获取日志目录完整路径。
     * @return 日志目录 Path
     */
    public Path getLogDir() {
        return toAbsolutePath(config.getLogDirectory());
    }

    // ============================================================
    //  试验数据路径
    // ============================================================

    /**
     * 获取指定试验的 CSV 数据文件完整路径。
     *
     * <pre>
     * 路径格式: {TestDataRoot}/{productId}/{testId}/sensor.csv
     * </pre>
     *
     * @param productId 样品编号，不能为空
     * @param testId    试验 ID，不能为空
     * @return CSV 文件 Path
     * @throws IllegalArgumentException productId 或 testId 为空
     */
    public Path getCsvPath(String productId, String testId) {
        validateNotBlank(productId, "productId");
        validateNotBlank(testId, "testId");
        return getTestDataRoot()
                .resolve(sanitize(productId))
                .resolve(sanitize(testId))
                .resolve(CSV_FILE_NAME);
    }

    /**
     * 获取指定试验的数据目录。
     *
     * <pre>
     * 路径格式: {TestDataRoot}/{productId}/{testId}/
     * </pre>
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @return 试验数据目录 Path
     */
    public Path getTestDataDir(String productId, String testId) {
        validateNotBlank(productId, "productId");
        validateNotBlank(testId, "testId");
        return getTestDataRoot()
                .resolve(sanitize(productId))
                .resolve(sanitize(testId));
    }

    // ============================================================
    //  报告路径
    // ============================================================

    /**
     * 获取报告文件完整路径。
     *
     * <pre>
     * 路径格式: {ReportRoot}/{testId}_报告.{format}
     * 示例: ./ISO11820_Data/Reports/20260630-143000_报告.xlsx
     * </pre>
     *
     * @param testId 试验 ID，不能为空
     * @param format 文件格式（xlsx / pdf / csv），不能为空
     * @return 报告文件 Path
     */
    public Path getReportPath(String testId, String format) {
        validateNotBlank(testId, "testId");
        validateNotBlank(format, "format");
        String fileName = sanitize(testId) + REPORT_SUFFIX + "." + format.toLowerCase();
        return getReportRoot().resolve(fileName);
    }

    /**
     * 获取 Excel 报告文件完整路径（快捷方法）。
     * 等价于 {@code getReportPath(testId, "xlsx")}。
     *
     * @param testId 试验 ID
     * @return Excel 报告文件 Path
     */
    public Path getExcelReportPath(String testId) {
        return getReportPath(testId, EXCEL_EXT);
    }

    /**
     * 获取 PDF 报告文件完整路径（快捷方法）。
     * 等价于 {@code getReportPath(testId, "pdf")}。
     *
     * @param testId 试验 ID
     * @return PDF 报告文件 Path
     */
    public Path getPdfReportPath(String testId) {
        return getReportPath(testId, PDF_EXT);
    }

    /**
     * 判断指定试验的报告文件是否存在。
     *
     * @param testId 试验 ID
     * @param format 格式（xlsx / pdf）
     * @return true 表示报告文件存在
     */
    public boolean reportExists(String testId, String format) {
        return fileExists(getReportPath(testId, format));
    }

    // ============================================================
    //  目录创建
    // ============================================================

    /**
     * 确保指定试验的数据目录存在，不存在则一次性创建多级父目录。
     * 通常在写入 CSV 之前调用。
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @return true 表示目录已就绪（新建或已存在）
     * @throws RuntimeException 创建失败时抛出
     */
    public boolean ensureTestDataDirs(String productId, String testId) {
        Path dir = getTestDataDir(productId, testId);
        return ensureDir(dir);
    }

    /**
     * 确保报告输出目录存在。
     * @return true 表示目录已就绪
     */
    public boolean ensureReportDir() {
        return ensureDir(getReportRoot());
    }

    /**
     * 确保日志目录存在。
     * @return true 表示目录已就绪
     */
    public boolean ensureLogDir() {
        return ensureDir(getLogDir());
    }

    /**
     * 确保数据库文件所在目录存在。
     * @return true 表示目录已就绪
     */
    public boolean ensureDatabaseDir() {
        Path dbPath = getDatabasePath();
        Path parent = dbPath.getParent();
        return parent == null || ensureDir(parent);
    }

    /**
     * 确保所有基础目录存在（应用启动时调用一次）。
     * 包括：TestData、Reports、Logs、Database 父目录。
     */
    public void ensureAllBaseDirs() {
        ensureDir(getTestDataRoot());
        ensureDir(getReportRoot());
        ensureDir(getLogDir());
        ensureDatabaseDir();
        log.info("所有基础目录检查完成");
    }

    // ============================================================
    //  文件操作
    // ============================================================

    /**
     * 判断文件是否存在。
     *
     * @param path 文件路径，不能为 null
     * @return true 表示文件存在且为普通文件
     */
    public boolean fileExists(Path path) {
        if (path == null) {
            return false;
        }
        return Files.exists(path) && Files.isRegularFile(path);
    }

    /**
     * 判断目录是否存在。
     *
     * @param path 目录路径，不能为 null
     * @return true 表示目录存在
     */
    public boolean dirExists(Path path) {
        if (path == null) {
            return false;
        }
        return Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * 判断指定试验的 CSV 数据文件是否存在。
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @return true 表示 CSV 文件存在
     */
    public boolean csvExists(String productId, String testId) {
        return fileExists(getCsvPath(productId, testId));
    }

    /**
     * 删除指定试验的整个数据目录（含 CSV 文件和父目录）。
     * 递归删除，谨慎使用。
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @return true 表示删除成功（或目录本就不存在）
     */
    public boolean deleteTestDataDir(String productId, String testId) {
        Path dir = getTestDataDir(productId, testId);
        return deleteDir(dir);
    }

    /**
     * 递归删除目录及其所有内容。
     *
     * @param dir 目录路径
     * @return true 表示删除成功或目录不存在
     */
    public boolean deleteDir(Path dir) {
        if (!Files.exists(dir)) {
            return true;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            log.info("目录已删除: {}", dir);
            return true;
        } catch (IOException e) {
            log.error("删除目录失败: {}", dir, e);
            return false;
        }
    }

    /**
     * 获取文件大小（字节），文件不存在时返回 -1。
     *
     * @param path 文件路径
     * @return 文件大小（字节）或 -1
     */
    public long getFileSize(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) : -1;
        } catch (IOException e) {
            log.error("获取文件大小失败: {}", path, e);
            return -1;
        }
    }

    /**
     * 递归计算目录总大小（字节）。
     *
     * @param dir 目录路径
     * @return 总字节数，目录不存在时返回 0
     */
    public long getDirSize(Path dir) {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            log.error("计算目录大小失败: {}", dir, e);
            return 0;
        }
    }

    /**
     * 获取 TestData 目录总大小（可读格式）。
     *
     * @return 如 "125.3 MB"
     */
    public String getTestDataSizeFormatted() {
        return formatBytes(getDirSize(getTestDataRoot()));
    }

    /**
     * 获取 Reports 目录总大小（可读格式）。
     *
     * @return 如 "8.2 MB"
     */
    public String getReportSizeFormatted() {
        return formatBytes(getDirSize(getReportRoot()));
    }

    // ============================================================
    //  批量清理（测试环境用）
    // ============================================================

    /**
     * 清空所有试验数据目录（CSV 文件及父目录）。
     * ⚠️ 不可逆操作，仅用于测试环境。
     *
     * @return 删除的目录数量
     */
    public int cleanAllTestData() {
        Path testDataRoot = getTestDataRoot();
        if (!Files.exists(testDataRoot)) {
            return 0;
        }

        int count = 0;
        try (Stream<Path> entries = Files.list(testDataRoot)) {
            for (Path entry : entries.toList()) {
                if (Files.isDirectory(entry)) {
                    deleteDir(entry);
                    count++;
                }
            }
        } catch (IOException e) {
            log.error("清理 TestData 目录失败", e);
        }

        log.info("已清空所有试验数据: {} 个产品目录", count);
        return count;
    }

    /**
     * 清空所有报告文件。
     * ⚠️ 不可逆操作，仅用于测试环境。
     *
     * @return 删除的文件数量
     */
    public int cleanAllReports() {
        Path reportRoot = getReportRoot();
        if (!Files.exists(reportRoot)) {
            return 0;
        }

        int count = 0;
        try (Stream<Path> entries = Files.list(reportRoot)) {
            for (Path entry : entries.toList()) {
                if (Files.isRegularFile(entry)) {
                    try {
                        Files.delete(entry);
                        count++;
                    } catch (IOException e) {
                        log.warn("删除报告文件失败: {}", entry);
                    }
                }
            }
        } catch (IOException e) {
            log.error("清理 Reports 目录失败", e);
        }

        log.info("已清空所有报告文件: {} 个文件", count);
        return count;
    }

    /**
     * 一键清空所有数据（试验数据 + 报告文件）。
     * ⚠️ 不可逆操作，仅用于测试环境重置。
     *
     * @return 清理摘要字符串
     */
    public String cleanAll() {
        int testDataCount = cleanAllTestData();
        int reportCount = cleanAllReports();
        String summary = String.format("已清空: %d 个产品目录, %d 个报告文件", testDataCount, reportCount);
        log.info("一键清空完成: {}", summary);
        return summary;
    }

    /**
     * 统计 CSV 数据文件总数。
     *
     * @return CSV 文件数量
     */
    public long countCsvFiles() {
        Path root = getTestDataRoot();
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.getFileName().toString().equals(CSV_FILE_NAME)).count();
        } catch (IOException e) {
            log.error("统计 CSV 文件失败", e);
            return 0;
        }
    }

    /**
     * 统计报告文件总数。
     *
     * @return 报告文件数量（xlsx + pdf）
     */
    public long countReportFiles() {
        Path root = getReportRoot();
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> list = Files.list(root)) {
            return list.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith("." + EXCEL_EXT) || name.endsWith("." + PDF_EXT);
                    })
                    .count();
        } catch (IOException e) {
            log.error("统计报告文件失败", e);
            return 0;
        }
    }

    // ============================================================
    //  内部方法
    // ============================================================

    /**
     * 将相对路径转为绝对路径。
     * 以当前工作目录（user.dir）为基准。
     */
    private Path toAbsolutePath(String pathStr) {
        Path path = Paths.get(pathStr);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
    }

    /**
     * 确保目录存在，不存在则创建多级父目录。
     */
    private boolean ensureDir(Path dir) {
        if (Files.exists(dir)) {
            if (Files.isDirectory(dir)) {
                return true;
            }
            throw new RuntimeException("路径已存在但不是目录: " + dir);
        }
        try {
            Files.createDirectories(dir);
            log.debug("目录已创建: {}", dir);
            return true;
        } catch (IOException e) {
            log.error("创建目录失败: {}", dir, e);
            throw new RuntimeException("创建目录失败: " + dir, e);
        }
    }

    /**
     * 清理路径片段中的非法字符，防止路径穿越。
     */
    private String sanitize(String segment) {
        if (segment == null) {
            return "";
        }
        // 移除路径分隔符和特殊字符，防止路径穿越攻击
        return segment.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 参数非空校验。
     */
    private void validateNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    /**
     * 字节数转可读格式。
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 返回当前路径配置概览（调试用）。
     */
    @Override
    public String toString() {
        return String.format("""
                FilePathManageUtil{
                  BaseDir       = %s
                  TestDataRoot  = %s  (%s)
                  ReportRoot    = %s  (%s)
                  DatabasePath  = %s
                  LogDir        = %s
                  CSV文件数     = %d
                  报告文件数    = %d
                }""",
                getBaseDir(),
                getTestDataRoot(), getTestDataSizeFormatted(),
                getReportRoot(), getReportSizeFormatted(),
                getDatabasePath(), getLogDir(),
                countCsvFiles(), countReportFiles());
    }
}