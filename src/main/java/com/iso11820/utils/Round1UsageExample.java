package com.iso11820.utils;

/**
 * <h1>第 1 轮 — 配置与日志调用示例</h1>
 *
 * <p>展示 AppConfig 和 LogUtil 的常用调用方式，可直接复制到业务代码中使用。</p>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
@SuppressWarnings("unused")
public final class Round1UsageExample {

    private Round1UsageExample() {
        throw new UnsupportedOperationException("示例类不允许实例化");
    }

    // ============================================================
    //  AppConfig 示例
    // ============================================================

    /**
     * 演示 AppConfig 的各种读取方式。
     */
    public static void appConfigDemo() {
        AppConfig config = AppConfig.getInstance();

        // ---- 1. 数据库配置 ----
        // 获取数据库路径
        String dbPath = config.getDatabasePath();
        System.out.println("数据库路径: " + dbPath);

        // ---- 2. 仿真参数 ----
        // 获取目标炉温
        double targetTemp = config.getTargetFurnaceTemp();
        System.out.println("目标炉温: " + targetTemp + " °C");

        // 获取升温速率
        double heatingRate = config.getHeatingRatePerSecond();
        System.out.println("升温速率: " + heatingRate + " °C/s");

        // 获取温度波动
        double fluctuation = config.getTempFluctuation();
        System.out.println("温度波动: ±" + fluctuation + " °C");

        // 获取初始炉温
        double initialTemp = config.getInitialFurnaceTemp();
        System.out.println("初始炉温: " + initialTemp + " °C");

        // 是否启用仿真
        boolean simEnabled = config.isSimulationEnabled();
        System.out.println("仿真模式: " + (simEnabled ? "开启" : "关闭"));

        // ---- 3. 文件存储路径 ----
        // 获取根目录
        String baseDir = config.getBaseDirectory();
        System.out.println("文件根目录: " + baseDir);

        // 获取试验数据目录
        String testDataDir = config.getTestDataDirectory();
        System.out.println("试验数据目录: " + testDataDir);

        // ---- 4. 报告配置 ----
        // 获取报告输出目录
        String reportDir = config.getReportOutputDirectory();
        System.out.println("报告输出目录: " + reportDir);

        // 是否启用 PDF 导出
        boolean pdfEnabled = config.isPdfExportEnabled();
        System.out.println("PDF 导出: " + (pdfEnabled ? "启用" : "禁用"));

        // ---- 5. 通用 JSON 路径读取 ----
        // 读取任意路径的配置值（带默认值兜底）
        String encoding = config.getString("FileStorage.CsvEncoding", "UTF-8");
        int maxHistory = config.getInt("Logging.MaxHistoryDays", 30);
        double chartWidth = config.getDouble("Report.ChartWidth", 800.0);

        System.out.println("CSV 编码: " + encoding);
        System.out.println("日志保留天数: " + maxHistory);
        System.out.println("图表宽度: " + chartWidth + "px");

        // ---- 6. 打印全部配置（调试用） ----
        System.out.println("\n========== 全部配置 ==========");
        System.out.println(config);

        // ---- 7. 运行时重载配置 ----
        // config.reload();
    }

    // ============================================================
    //  LogUtil 示例
    // ============================================================

    /**
     * 演示 LogUtil 的各种日志级别和便捷方法。
     */
    public static void logUtilDemo() {
        // ---- 1. 基本日志级别 ----
        LogUtil.trace("这是 TRACE 级别日志，通常不会输出");
        LogUtil.debug("这是 DEBUG 级别日志，调试用");
        LogUtil.info("这是 INFO 级别日志，记录关键节点");
        LogUtil.warn("这是 WARN 级别日志，警告信息");
        LogUtil.error("这是 ERROR 级别日志，错误信息");

        // ---- 2. 参数化日志（SLF4J {} 占位符） ----
        String userName = "admin";
        int testCount = 42;
        LogUtil.info("用户 {} 登录成功，今日第 {} 次试验", userName, testCount);

        double tf1 = 750.3;
        double tf2 = 749.8;
        LogUtil.debug("炉温1={}°C, 炉温2={}°C", tf1, tf2);

        // ---- 3. 异常日志 ----
        try {
            // 模拟业务异常
            throw new RuntimeException("文件写入失败");
        } catch (Exception e) {
            LogUtil.error("导出报告时发生异常", e);
        }

        // ---- 4. 方法入口/出口/耗时 ----
        LogUtil.enter("exportReport", "productId=P001", "testId=20260630-120000");

        long start = System.currentTimeMillis();
        // ... 模拟业务逻辑 ...
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LogUtil.exit("exportReport", "成功");
        LogUtil.elapsed("exportReport", start);
        // 输出: exportReport 耗时: 100 ms

        // ---- 5. 自定义 Logger ----
        // 使用专用 logger 名称隔离日志
        org.slf4j.Logger exportLogger = LogUtil.getLogger("EXPORT");
        exportLogger.info("Excel 文件已生成: ./reports/test.xlsx");

        // 使用类名作为 logger
        org.slf4j.Logger classLogger = LogUtil.getLogger(Round1UsageExample.class);
        classLogger.debug("通过类名获取 logger");
    }

    // ============================================================
    //  main（可直接运行查看效果）
    // ============================================================

    /**
     * 运行此方法查看配置加载和日志输出效果。
     */
    public static void main(String[] args) {
        System.out.println("========== AppConfig 示例 ==========");
        appConfigDemo();

        System.out.println("\n========== LogUtil 示例 ==========");
        logUtilDemo();

        System.out.println("\n========== 第 1 轮示例完成 ==========");
    }
}