# ISO 11820 — 工具层交付文档

> **项目**: ISO 11820 建筑材料不燃性试验仿真系统  
> **模块**: 工具层（utils + service）  
> **开发人员**: 工具服务开发（5人小组中的工具层负责）  
> **技术栈**: Java 17 + Maven  
> **日期**: 2026-06-30  

---

## 一、架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI 层（其他组员）                          │
│                    JavaFX + FXML + Controller                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │ 调用
┌──────────────────────────▼──────────────────────────────────────┐
│                     Service 服务层（本模块）                       │
│  ┌─────────────────┐  ┌──────────────────┐  ┌────────────────┐  │
│  │ReportExportService│  │ ExcelReportService│  │PdfReportService│  │
│  │   ⭐ 统一门面     │  │  POI 5.x 导出    │  │ iText7 导出    │  │
│  └────────┬────────┘  └────────┬─────────┘  └───────┬────────┘  │
│           │                    │                      │          │
│  ┌────────▼────────────────────▼──────────────────────▼────────┐ │
│  │                    CsvDataService                            │ │
│  │              CSV 温度时序数据读写服务                          │ │
│  └────────────────────────┬────────────────────────────────────┘ │
│                           │                                      │
│  ┌────────────────────────▼────────────────────────────────────┐ │
│  │              entity/DataPoint  +  model/ExportTestInfo       │ │
│  │                    数据实体（纯 POJO）                        │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────────┘
                           │ 依赖
┌──────────────────────────▼──────────────────────────────────────┐
│                     Utils 工具层（本模块）                         │
│  ┌──────────┐ ┌──────────┐ ┌───────────┐ ┌──────────┐          │
│  │AppConfig │ │ LogUtil  │ │ DateUtil  │ │ NumUtil  │          │
│  │配置管理   │ │日志门面  │ │日期时间   │ │数值工具  │          │
│  └──────────┘ └──────────┘ └───────────┘ └──────────┘          │
│  ┌──────────┐ ┌────────────────────┐                            │
│  │JsonUtil  │ │FilePathManageUtil  │                            │
│  │JSON工具  │ │路径统一管理        │                            │
│  └──────────┘ └────────────────────┘                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │ 读取
┌──────────────────────────▼──────────────────────────────────────┐
│                  appsettings.json  +  logback.xml                │
│                    配置文件          日志配置                     │
└─────────────────────────────────────────────────────────────────┘
```

**依赖原则**: 上层可依赖下层，下层绝不感知上层。Service 依赖 Utils，Utils 只依赖配置文件。

---

## 二、四轮开发文件清单

### 第 1 轮：配置与日志

| 文件 | 包路径 | 行数 | 说明 |
|------|--------|------|------|
| `AppConfig.java` | `com.iso11820.utils` | ~200 | 配置管理单例，线程安全 DCL |
| `LogUtil.java` | `com.iso11820.utils` | ~180 | 日志门面，封装 SLF4J/Logback |
| `DateUtil.java` | `com.iso11820.utils` | ~280 | 日期格式化、时间戳、时差计算 |
| `NumUtil.java` | `com.iso11820.utils` | ~260 | 精度控制、空值转换、数字校验 |
| `JsonUtil.java` | `com.iso11820.utils` | ~280 | JSON 序列化/反序列化、文件读写 |
| `appsettings.json` | `resources` | ~55 | 完整配置文件 |
| `logback.xml` | `resources` | ~110 | 日志配置（控制台彩色+滚动文件） |

### 第 2 轮：CSV 与文件管理

| 文件 | 包路径 | 行数 | 说明 |
|------|--------|------|------|
| `FilePathManageUtil.java` | `com.iso11820.utils` | ~480 | 路径管理、目录创建、批量清理 |
| `DataPoint.java` | `com.iso11820.service.entity` | ~180 | 时序温度数据点实体 |
| `CsvDataService.java` | `com.iso11820.service` | ~300 | CSV 读写服务（追加/批量/读取/删除） |

### 第 3 轮：Excel/PDF 导出

| 文件 | 包路径 | 行数 | 说明 |
|------|--------|------|------|
| `ExportTestInfo.java` | `com.iso11820.service.model` | ~280 | 试验报告数据实体 |
| `ExcelReportService.java` | `com.iso11820.service` | ~370 | Excel 3 Sheet 报告导出 |
| `PdfReportService.java` | `com.iso11820.service` | ~420 | PDF A4 中文报告导出 |

### 第 4 轮：整合与文档

| 文件 | 包路径 | 行数 | 说明 |
|------|--------|------|------|
| `ReportExportService.java` | `com.iso11820.service` | ~200 | ⭐ 统一导出门面 |
| `Round4FullIntegrationExample.java` | `com.iso11820.service` | ~250 | 全链路集成演示 |
| `README.md` | 项目根目录 | — | 本文档 |

### 示例文件

| 文件 | 说明 |
|------|------|
| `Round1UsageExample.java` | 配置/日志/日期/数值/JSON 调用示例 |
| `Round2UsageExample.java` | CSV 读写 + 文件路径管理示例 |
| `Round3UsageExample.java` | Excel/PDF 导出示例 |
| `Round4FullIntegrationExample.java` | 全流程集成测试（可直接运行） |

---

## 三、类依赖关系图

```
ReportExportService（门面）
  ├── ExcelReportService ────┬── AppConfig
  │                          ├── FilePathManageUtil
  │                          ├── CsvDataService ── DataPoint
  │                          └── NumUtil, DateUtil
  ├── PdfReportService ──────┬── AppConfig
  │                          ├── FilePathManageUtil
  │                          └── NumUtil, DateUtil
  └── ExportTestInfo ──────── NumUtil

CsvDataService ──┬── FilePathManageUtil ── AppConfig
                 ├── NumUtil, DateUtil
                 └── DataPoint ── NumUtil, DateUtil

FilePathManageUtil ── AppConfig, LogUtil

AppConfig, LogUtil, DateUtil, NumUtil, JsonUtil
  └── 无内部依赖（仅依赖 Jackson + SLF4J）
```

---

## 四、Maven 完整依赖汇总

以下依赖可直接复制到 `pom.xml` 的 `<dependencies>` 块中：

```xml
<!-- ============================================================ -->
<!--  日志框架                                                      -->
<!-- ============================================================ -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.14</version>
</dependency>

<!-- ============================================================ -->
<!--  JSON 序列化                                                   -->
<!-- ============================================================ -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.1</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
    <version>2.16.1</version>
</dependency>

<!-- ============================================================ -->
<!--  Excel 导出（Apache POI 5.x）                                    -->
<!-- ============================================================ -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.5</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- ============================================================ -->
<!--  PDF 导出（iText7）                                             -->
<!-- ============================================================ -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>7.2.5</version>
    <type>pom</type>
</dependency>
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>pdfCalligraph</artifactId>
    <version>7.2.5</version>
</dependency>
```

> **Maven 属性**（放在 `<properties>` 中）：
> ```xml
> <maven.compiler.source>17</maven.compiler.source>
> <maven.compiler.target>17</maven.compiler.target>
> <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
> ```

---

## 五、运行前置条件

### 5.1 开发环境

| 要求 | 版本/说明 |
|------|----------|
| JDK | **17** 或更高（LTS 推荐） |
| Maven | 3.8+ |
| IDE | IntelliJ IDEA 2023+（推荐）或 VS Code |
| 操作系统 | Windows 10/11 或 Linux（Ubuntu 20.04+） |

### 5.2 编码设置

- 所有 `.java` 文件使用 **UTF-8** 编码
- `appsettings.json`、`logback.xml` 使用 **UTF-8** 编码
- CSV 文件默认 **UTF-8** 编码（可在配置中修改）
- IDEA 设置：`File → Settings → Editor → File Encodings → UTF-8`

### 5.3 文件夹权限

| 目录 | 权限要求 | 说明 |
|------|---------|------|
| `./ISO11820_Data/` | 读写 | 数据根目录，程序自动创建 |
| `./logs/` | 读写 | 日志目录，程序自动创建 |
| `./Data/` | 读写 | 数据库目录（SQLite） |
| 字体文件（Windows） | 读 | PDF 需读取 `C:/Windows/Fonts/simsun.ttc` |

> Linux 下 PDF 中文渲染需手动安装中文字体：`sudo apt install fonts-noto-cjk`

### 5.4 配置文件位置

- `src/main/resources/appsettings.json` — classpath 配置（开发环境）
- 可通过 `AppConfig.setExternalConfigPath("/path/to/config.json")` 指定外部配置

---

## 六、高频报错排查手册

### 6.1 中文乱码

| 现象 | 原因 | 解决 |
|------|------|------|
| 控制台乱码 | IDEA 编码设置不正确 | `Help → Edit Custom VM Options` 添加 `-Dfile.encoding=UTF-8` |
| CSV 中文乱码 | Excel 打开方式问题 | 用记事本另存为 UTF-8 BOM 格式，或使用 WPS 打开 |
| PDF 中文显示为方块 | 缺少中文字体 | 方案1：`pdf.setCustomFontPath("D:/fonts/msyh.ttf")` 指定字体；方案2：放入 `src/main/resources/fonts/NotoSansSC-Regular.otf` |
| 日志文件乱码 | logback.xml 未配置编码 | 确认 `logback.xml` 中 `<charset>UTF-8</charset>` |

### 6.2 文件找不到

| 现象 | 原因 | 解决 |
|------|------|------|
| `appsettings.json` 找不到 | 不在 classpath 中 | 确认文件在 `src/main/resources/` 下，或调用 `setExternalConfigPath()` |
| CSV 文件不存在 | 未写入数据或路径错误 | 确保先调用 `CsvDataService.appendRow()` 写入数据 |
| SQLite 数据库不存在 | 首次运行未初始化 | 确保 `sql/schema.sql` 在 classpath 中 |
| 字体文件找不到 | Windows/Linux 路径差异 | Windows: `C:/Windows/Fonts/simsun.ttc`；Linux: 需手动安装 |

### 6.3 CSV 解析失败

| 现象 | 原因 | 解决 |
|------|------|------|
| 解析返回空列表 | 文件不存在或为空 | 调用 `FilePathManageUtil.csvExists()` 检查 |
| 部分行跳过 | 格式损坏 | 查看日志中 WARN 级别信息，定位损坏行号 |
| 数值全为 0 | 分隔符不匹配 | 确认 `appsettings.json` 中 `CsvDelimiter` 为 `,` |
| 读取到乱码 | 编码不一致 | 确认 `CsvEncoding` 配置为 `UTF-8` |

### 6.4 PDF 字体缺失

| 现象 | 原因 | 解决 |
|------|------|------|
| 中文全部方块 | 系统无中文字体 | 下载 NotoSansSC-Regular.otf 放入 `resources/fonts/` |
| 部分字符缺失 | 字体不完整 | 使用完整字体文件（如微软雅黑 20MB+） |
| 字体加载失败 | 路径错误 | 使用绝对路径：`pdf.setCustomFontPath("D:/fonts/msyh.ttf")` |
| Linux 下无字体 | 未安装中文字体包 | `sudo apt install fonts-noto-cjk fonts-wqy-microhei` |

### 6.5 导出权限不足

| 现象 | 原因 | 解决 |
|------|------|------|
| Excel 写入失败 | 文件被占用 | 关闭已打开的 Excel 文件后重试 |
| 目录创建失败 | 无写入权限 | 检查当前目录是否可写，或修改 `appsettings.json` 中的路径 |
| PDF 写入失败 | 路径不存在 | 确认 `FileStorage.BaseDirectory` 路径合法 |
| 批量导出中断 | 磁盘空间不足 | 检查磁盘剩余空间 |

### 6.6 其他常见问题

| 现象 | 原因 | 解决 |
|------|------|------|
| Maven 编译报错 | 依赖版本冲突 | 运行 `mvn dependency:tree` 检查冲突 |
| `getInstance()` 返回 null | 类加载顺序问题 | 确认 DCL 单例类未被序列化破坏 |
| 温度精度异常 | 未使用 NumUtil | 所有温度值使用 `NumUtil.roundTemp()` 统一精度 |
| 日志文件不滚动 | logback.xml 配置错误 | 确认 `maxFileSize` 和 `maxHistory` 配置正确 |

---

## 七、Git 提交规范

### 7.1 提交信息格式

```
[模块] 简短描述

详细说明（可选）

- 模块: utils | service | config
- 类型: feat | fix | refactor | docs | test
```

### 7.2 示例

```
[utils] 新增 DateUtil、NumUtil、JsonUtil 工具类

[service] 实现 ExcelReportService 和 PdfReportService

[config] 更新 appsettings.json 和 logback.xml

[fix] 修复 CsvDataService 批量写入并发问题

[docs] 补充 README 文档和调用示例
```

### 7.3 模块修改红线 ⚠️

| 禁止操作 | 说明 |
|---------|------|
| ❌ 修改 `com.iso11820.core.*` | 核心引擎层（其他组员负责） |
| ❌ 修改 `com.iso11820.dao.*` | 数据持久层（其他组员负责） |
| ❌ 修改 `com.iso11820.ui.*` | UI 界面层（其他组员负责） |
| ❌ 修改 `com.iso11820.entity.*` | 数据库实体（其他组员负责） |
| ✅ 修改 `com.iso11820.utils.*` | 本模块 — 工具层 |
| ✅ 修改 `com.iso11820.service.*` | 本模块 — 服务层 |
| ✅ 修改 `pom.xml` | 仅添加依赖，不修改 compile 配置 |
| ✅ 修改 `appsettings.json` | 仅新增配置项，不删除已有项 |
| ✅ 修改 `logback.xml` | 仅新增 logger，不修改根配置 |

---

## 八、快速开始

### 8.1 最简调用（3 行代码）

```java
// 1. 构造数据
ExportTestInfo info = new ExportTestInfo();
info.setTestId("20260630-143000");
// ... 填充其他字段 ...

// 2. 一键导出（自动判断生成 Excel+PDF）
boolean ok = ReportExportService.getInstance().exportReport(info);
```

### 8.2 完整流程（可直接运行）

```bash
# 运行全流程集成测试
mvn compile exec:java -Dexec.mainClass="com.iso11820.service.Round4FullIntegrationExample"
```

### 8.3 各轮示例入口

```bash
# 第 1 轮: 配置 + 日志 + 日期 + 数值 + JSON
mvn compile exec:java -Dexec.mainClass="com.iso11820.utils.Round1UsageExample"

# 第 2 轮: CSV 读写 + 文件路径管理
mvn compile exec:java -Dexec.mainClass="com.iso11820.service.Round2UsageExample"

# 第 3 轮: Excel + PDF 导出
mvn compile exec:java -Dexec.mainClass="com.iso11820.service.Round3UsageExample"

# 第 4 轮: 全流程集成
mvn compile exec:java -Dexec.mainClass="com.iso11820.service.Round4FullIntegrationExample"
```

---

## 附录：文件清单完整索引

```
src/main/java/com/project/
├── utils/
│   ├── AppConfig.java                  # 配置管理单例
│   ├── LogUtil.java                    # 日志工具门面
│   ├── DateUtil.java                   # 日期时间工具
│   ├── NumUtil.java                    # 数值工具
│   ├── JsonUtil.java                   # JSON 工具
│   ├── FilePathManageUtil.java         # 文件路径统一管理
│   ├── Round1UsageExample.java         # 第1轮示例
│   └── package-info.java
├── service/
│   ├── ReportExportService.java        # ⭐ 统一导出门面
│   ├── CsvDataService.java             # CSV 读写服务
│   ├── ExcelReportService.java         # Excel 导出服务
│   ├── PdfReportService.java           # PDF 导出服务
│   ├── Round2UsageExample.java         # 第2轮示例
│   ├── Round3UsageExample.java         # 第3轮示例
│   ├── Round4FullIntegrationExample.java # 第4轮全流程
│   ├── package-info.java
│   ├── entity/
│   │   └── DataPoint.java              # 时序温度实体
│   └── model/
│       └── ExportTestInfo.java         # 导出数据实体
src/main/resources/
├── appsettings.json                    # 配置文件
└── logback.xml                         # 日志配置
```

---

> **文档版本**: 1.0 | **最后更新**: 2026-06-30 | **维护者**: 工具服务开发组