package com.iso11820.service;

import com.iso11820.service.entity.DataPoint;
import com.iso11820.utils.*;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <h1>CSV 温度数据读写服务 — 线程安全单例</h1>
 *
 * <p>管理试验时序温度数据的 CSV 文件读写。支持：
 * <ul>
 *   <li>首次写入自动生成 UTF-8 BOM 表头</li>
 *   <li>单行追加写入（每次试验 tick 调用）</li>
 *   <li>批量缓冲写入（减少 IO 次数）</li>
 *   <li>按产品+试验 ID 读取全量数据</li>
 *   <li>按时间范围截取数据片段</li>
 *   <li>全面的容错处理：文件缺失、权限不足、格式损坏均不崩溃</li>
 * </ul>
 *
 * <h2>CSV 格式</h2>
 * <pre>
 * Time,Temp1,Temp2,TempSurface,TempCenter,TempCalibration,AmbientTemp,AmbientHumidity,Timestamp
 * 0,25.0,24.9,24.5,24.3,25.1,26.0,60.0,2026-06-30 14:30:00
 * 1,30.1,30.0,24.6,24.4,25.0,26.0,60.0,2026-06-30 14:30:01
 * </pre>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   CsvDataService csv = CsvDataService.getInstance();
 *
 *   // 写入
 *   DataPoint dp = new DataPoint(0, 25.0, 24.9, 24.5, 24.3, 25.1, 26.0, 60.0);
 *   csv.appendRow("P001", "20260630-143000", dp);
 *
 *   // 批量写入
 *   csv.batchAppend("P001", "20260630-143000", dataPoints);
 *
 *   // 读取
 *   List<DataPoint> all = csv.readAll("P001", "20260630-143000");
 *   List<DataPoint> segment = csv.readRange("P001", "20260630-143000", 0, 600);
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class CsvDataService {

    // ============================================================
    //  单例
    // ============================================================

    private static volatile CsvDataService INSTANCE;

    /** 获取单例实例 */
    public static CsvDataService getInstance() {
        if (INSTANCE == null) {
            synchronized (CsvDataService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CsvDataService();
                }
            }
        }
        return INSTANCE;
    }

    // ============================================================
    //  常量
    // ============================================================

    /** CSV 表头行 */
    private static final String CSV_HEADER =
            "Time,Temp1,Temp2,TempSurface,TempCenter,TempCalibration,AmbientTemp,AmbientHumidity,Timestamp";

    /** 日志 */
    private final Logger log = LogUtil.getLogger("CSV");

    /** 路径工具 */
    private final FilePathManageUtil pathUtil;

    /** 读写锁：保护文件写入操作 */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /** 编码 */
    private final Charset charset;

    /** 分隔符 */
    private final String delimiter;

    /** 换行符 */
    private final String lineSeparator;

    // ============================================================
    //  构造
    // ============================================================

    private CsvDataService() {
        this.pathUtil = FilePathManageUtil.getInstance();
        AppConfig config = AppConfig.getInstance();
        // 从配置读取编码，默认 UTF-8
        String encoding = config.getString("FileStorage.CsvEncoding", "UTF-8");
        this.charset = Charset.forName(encoding);
        // 从配置读取分隔符和换行符
        this.delimiter = config.getString("FileStorage.CsvDelimiter", ",");
        String ls = config.getString("FileStorage.CsvLineSeparator", "\n");
        // 将转义字符串 "\n" 转为实际换行符
        this.lineSeparator = ls.replace("\\n", "\n").replace("\\r", "\r");
    }

    // ============================================================
    //  写入
    // ============================================================

    /**
     * 追加写入一行数据点。
     * 如果文件不存在，自动创建目录并写入表头。
     *
     * @param productId 样品编号，不能为空
     * @param testId    试验 ID，不能为空
     * @param point     数据点，不能为 null
     * @return true 表示写入成功
     */
    public boolean appendRow(String productId, String testId, DataPoint point) {
        if (point == null) {
            log.warn("appendRow 跳过: DataPoint 为 null");
            return false;
        }
        return batchAppend(productId, testId, Collections.singletonList(point));
    }

    /**
     * 批量追加写入数据点（推荐使用，减少 IO 次数）。
     * 如果文件不存在，自动创建目录并写入表头。
     * 使用写锁保证并发安全。
     *
     * @param productId 样品编号，不能为空
     * @param testId    试验 ID，不能为空
     * @param points    数据点列表，不能为 null
     * @return true 表示全部写入成功
     */
    public boolean batchAppend(String productId, String testId, List<DataPoint> points) {
        // ---- 参数校验 ----
        if (productId == null || productId.isBlank()) {
            log.warn("batchAppend 跳过: productId 为空");
            return false;
        }
        if (testId == null || testId.isBlank()) {
            log.warn("batchAppend 跳过: testId 为空");
            return false;
        }
        if (points == null || points.isEmpty()) {
            log.debug("batchAppend 跳过: 数据点列表为空");
            return true; // 空列表视为成功
        }

        Path csvPath = pathUtil.getCsvPath(productId, testId);

        rwLock.writeLock().lock();
        try {
            // 确保目录存在
            pathUtil.ensureTestDataDirs(productId, testId);

            // 文件不存在则先写表头
            boolean needHeader = !Files.exists(csvPath) || Files.size(csvPath) == 0;

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(csvPath.toFile(), true), // append 模式
                            charset))) {

                // 写入表头
                if (needHeader) {
                    writer.write(CSV_HEADER);
                    writer.write(lineSeparator);
                }

                // 写入数据行
                for (DataPoint dp : points) {
                    writer.write(formatCsvLine(dp));
                    writer.write(lineSeparator);
                }

                writer.flush();
            }

            log.debug("CSV 写入成功: {} 行 → {}", points.size(), csvPath);
            return true;

        } catch (FileNotFoundException e) {
            log.error("CSV 文件访问被拒绝: {}", csvPath, e);
            return false;
        } catch (IOException e) {
            log.error("CSV 写入失败: {}", csvPath, e);
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // ============================================================
    //  读取
    // ============================================================

    /**
     * 读取指定试验的全部时序数据。
     * 文件不存在或读取失败时返回空列表，不抛异常。
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @return 数据点列表（不可变），文件不存在时返回空列表
     */
    public List<DataPoint> readAll(String productId, String testId) {
        return readRange(productId, testId, -1, -1);
    }

    /**
     * 按时间范围读取数据片段。
     *
     * @param productId    样品编号
     * @param testId       试验 ID
     * @param startSecond  起始秒（含），-1 表示从头开始
     * @param endSecond    结束秒（含），-1 表示到末尾
     * @return 符合时间范围的数据点列表，文件不存在时返回空列表
     */
    public List<DataPoint> readRange(String productId, String testId, int startSecond, int endSecond) {
        // ---- 参数校验 ----
        if (productId == null || productId.isBlank()) {
            log.warn("readRange 跳过: productId 为空");
            return Collections.emptyList();
        }
        if (testId == null || testId.isBlank()) {
            log.warn("readRange 跳过: testId 为空");
            return Collections.emptyList();
        }

        Path csvPath = pathUtil.getCsvPath(productId, testId);

        rwLock.readLock().lock();
        try {
            // 文件不存在
            if (!Files.exists(csvPath)) {
                log.warn("CSV 文件不存在: {}", csvPath);
                return Collections.emptyList();
            }
            if (Files.size(csvPath) == 0) {
                log.debug("CSV 文件为空: {}", csvPath);
                return Collections.emptyList();
            }

            List<DataPoint> result = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(csvPath.toFile()), charset))) {

                String line;
                boolean isFirstLine = true;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;

                    // 跳过空行
                    if (line.isBlank()) {
                        continue;
                    }

                    // 跳过表头
                    if (isFirstLine) {
                        isFirstLine = false;
                        // 容错：如果第一行不是表头，尝试解析为数据
                        if (line.startsWith("Time,") || line.startsWith("Time" + delimiter)) {
                            continue;
                        }
                    }

                    // 解析数据行
                    DataPoint dp = parseCsvLine(line, lineNumber);
                    if (dp == null) {
                        continue;
                    }

                    // 时间范围过滤
                    if (startSecond >= 0 && dp.getTimeSeconds() < startSecond) {
                        continue;
                    }
                    if (endSecond >= 0 && dp.getTimeSeconds() > endSecond) {
                        continue;
                    }

                    result.add(dp);
                }
            }

            log.debug("CSV 读取成功: {} 行 → {}", result.size(), csvPath);
            return Collections.unmodifiableList(result);

        } catch (FileNotFoundException e) {
            log.error("CSV 文件访问被拒绝: {}", csvPath, e);
            return Collections.emptyList();
        } catch (IOException e) {
            log.error("CSV 读取失败: {}", csvPath, e);
            return Collections.emptyList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 获取指定试验的数据行数（不含表头）。
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @return 数据行数，文件不存在时返回 0
     */
    public int countRows(String productId, String testId) {
        return readAll(productId, testId).size();
    }

    /**
     * 删除指定试验的 CSV 数据（通过删除整个试验数据目录实现）。
     *
     * @param productId 样品编号
     * @param testId    试验 ID
     * @return true 表示删除成功
     */
    public boolean deleteData(String productId, String testId) {
        if (productId == null || productId.isBlank() || testId == null || testId.isBlank()) {
            return false;
        }
        rwLock.writeLock().lock();
        try {
            boolean ok = pathUtil.deleteTestDataDir(productId, testId);
            if (ok) {
                log.info("试验数据已删除: {}/{}", productId, testId);
            }
            return ok;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // ============================================================
    //  内部方法
    // ============================================================

    /**
     * 将 DataPoint 格式化为 CSV 行。
     * 格式: Time,Temp1,Temp2,TempSurface,TempCenter,TempCalibration,AmbientTemp,AmbientHumidity,Timestamp
     */
    private String formatCsvLine(DataPoint dp) {
        return String.join(delimiter,
                String.valueOf(dp.getTimeSeconds()),
                formatDouble(dp.getTf1()),
                formatDouble(dp.getTf2()),
                formatDouble(dp.getTs()),
                formatDouble(dp.getTc()),
                formatDouble(dp.gettCal()),
                formatDouble(dp.getAmbientTemp()),
                formatDouble(dp.getAmbientHumidity()),
                dp.getTimestamp() != null ? dp.getTimestamp() : ""
        );
    }

    /**
     * 格式化 double 为字符串，避免科学计数法。
     */
    private String formatDouble(double value) {
        // 使用 NumUtil 格式化，保留 1 位小数
        return NumUtil.format(value, 1);
    }

    /**
     * 解析 CSV 行到 DataPoint。
     *
     * @param line       CSV 行内容
     * @param lineNumber 行号（用于日志）
     * @return DataPoint 或 null（解析失败时）
     */
    private DataPoint parseCsvLine(String line, int lineNumber) {
        try {
            String[] parts = line.split(delimiter, -1);
            if (parts.length < 8) {
                log.warn("CSV 第 {} 行列数不足（期望 ≥8，实际 {}），跳过: {}", lineNumber, parts.length, line);
                return null;
            }

            DataPoint dp = new DataPoint();
            dp.setTimeSeconds(NumUtil.toIntOrDefault(parts[0].trim(), 0));
            dp.setTf1(NumUtil.toDoubleOrDefault(parts[1].trim(), 0.0));
            dp.setTf2(NumUtil.toDoubleOrDefault(parts[2].trim(), 0.0));
            dp.setTs(NumUtil.toDoubleOrDefault(parts[3].trim(), 0.0));
            dp.setTc(NumUtil.toDoubleOrDefault(parts[4].trim(), 0.0));
            dp.settCal(NumUtil.toDoubleOrDefault(parts[5].trim(), 0.0));
            dp.setAmbientTemp(NumUtil.toDoubleOrDefault(parts[6].trim(), 0.0));
            dp.setAmbientHumidity(NumUtil.toDoubleOrDefault(parts[7].trim(), 0.0));

            // 时间戳列（第 9 列，可选）
            if (parts.length >= 9) {
                String ts = parts[8].trim();
                dp.setTimestamp(ts.isEmpty() ? DateUtil.now() : ts);
            }

            return dp;

        } catch (Exception e) {
            log.warn("CSV 第 {} 行解析失败: {}", lineNumber, e.getMessage());
            return null;
        }
    }
}