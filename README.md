# ISO 11820 建筑材料不燃性试验仿真系统

> **ISO 11820:2022** — 建筑材料不燃性试验的计算机仿真实现  
> 无需真实硬件，用软件模拟整个试验流程，生成标准格式报告  

---

## 一、项目简介

在建材防火实验室中，需要把建筑材料样品放入加热炉，加热到 **750°C**，记录 **60 分钟**的温度数据，判断材料是否"不燃"。

本项目用 **JavaFX 桌面应用** 仿真出整个试验流程——温度数据由仿真引擎自动生成，用户按照真实操作流程走一遍，最终生成 CSV / Excel / PDF 三种标准格式报告。

---

## 二、技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java 17 |
| 构建 | Maven 3.9+ |
| UI | JavaFX 21 + FXML |
| 数据库 | SQLite（sqlite-jdbc 3.45） |
| 图表 | XChart 3.8 |
| Excel | Apache POI 5.2 |
| PDF | iText7 7.2 |
| JSON | Jackson 2.16 |
| 日志 | SLF4J + Logback |
| 测试 | JUnit 5 + JaCoCo |

---

## 三、团队成员

| 角色 | 姓名 | 负责模块 | 说明 |
|:----:|------|----------|------|
| 1 | **颜瑞** | 业务核心层 | 仿真引擎（SensorSimulator）、状态机（TestStateMachine）、试验主控制器（TestMaster） |
| 2 | **陈培钦** | 数据持久层 | DAO 接口与实现、SQLite 数据库设计、schema.sql 建表脚本、DbUtil 连接管理 |
| 3 | **杨晋宇** | UI 层 | JavaFX FXML 界面设计、Controller 控制器、用户交互逻辑 |
| 4 | **王润廷** | 工具层 + 服务层 | AppConfig 配置管理、日志工具、DateUtil/NumUtil/JsonUtil、FilePathManageUtil；CsvDataService、ExcelReportService、PdfReportService 导出服务 |
| 5 | **柯康锐** | 测试 + 文档 | 单元测试编写、集成测试验证、JaCoCo 覆盖率、开发文档维护、README 编写 |

---

## 四、架构分层

```
┌──────────────────────────────────────────────────┐
│                  UI 层（杨晋宇）                    │
│  LoginView / MainView / NewTestDialog /           │
│  TestRecordDialog / SettingsDialog                │
│  FXML + Controller                                │
└────────────────────┬─────────────────────────────┘
                     │ 调用
┌────────────────────▼─────────────────────────────┐
│              业务核心层（颜瑞）                      │
│  TestMaster / TestStateMachine / SensorSimulator  │
│  TestState / SensorData / SystemMessage           │
└────────────────────┬─────────────────────────────┘
                     │ 调用
┌────────────────────▼─────────────────────────────┐
│              服务层（王润廷）                        │
│  CsvDataService / ExcelReportService /            │
│  PdfReportService / ReportExportService           │
└────────┬──────────────────────┬──────────────────┘
         │                      │
┌────────▼──────────┐  ┌───────▼───────────────────┐
│ 数据层（陈培钦）    │  │ 工具层（王润廷）             │
│ BaseDao / DAO接口  │  │ AppConfig / LogUtil /     │
│ DAO实现 / DbUtil   │  │ DateUtil / NumUtil /      │
│ Entity / schema.sql│  │ JsonUtil / FilePathUtil   │
└────────┬──────────┘  └───────────────────────────┘
         │
┌────────▼──────────┐
│  SQLite 数据库     │
│  ISO11820.db      │
└───────────────────┘

测试 + 文档（柯康锐）：JUnit 5 单元测试、JaCoCo 覆盖率、开发文档维护
```

**设计原则**：上层依赖下层，下层不感知上层；数据通过事件（DataChangeListener）向上传递；所有 UI 更新通过 `Platform.runLater()` 切换线程。

---

## 五、功能演示

### 完整试验流程

```
1. 启动程序 → 登录界面（管理员 admin / 123456）
2. 新建试验 → 填写样品信息 → 保存
3. 开始升温 → 炉温从 25°C 升至 750°C（仿真）
4. 温度稳定 → 自动变为"就绪"状态
5. 开始记录 → 计时器启动，每秒记录温度
6. 停止记录 → 填写试验后质量 → 保存试验记录
7. 记录查询 → 查看历史试验数据
8. 导出报告 → Excel / PDF 文件生成
```

### 核心功能清单

| 功能 | 说明 |
|------|------|
| 角色登录 | 管理员 / 试验员，密码验证 |
| 新建试验 | 样品信息、环境参数、时长模式 |
| 温度仿真 | 5 通道温度（炉温1/2、表面温、中心温、校准温），4 阶段算法 |
| 状态机 | Idle → Preparing → Ready → Recording → Complete |
| 实时显示 | 温度数值、曲线图、计时器、温漂、状态标签 |
| 系统消息 | 时间戳 + 消息内容，颜色区分事件类型 |
| 按钮状态 | 严格跟随状态机启用/禁用 |
| 试验记录 | 火焰现象、试验后质量、自动计算失重率和温升 |
| 数据导出 | CSV（自动）、Excel 3 Sheet（手动）、PDF 报告（手动） |
| 记录查询 | 日期范围、样品编号、操作员筛选，双击查看详情 |
| 设备校准 | 校准温显示、新建校准记录、历史列表 |
| 参数设置 | 目标炉温、升温速率、温度波动、标准时长，持久化保存 |

---

## 六、快速开始

### 环境要求

- **JDK 17**（必须）
- Maven 3.9+（或使用 IntelliJ IDEA 直接打开）
- Windows 10/11

### 启动程序

```bash
# 克隆仓库
git clone https://github.com/frilled87/ISO-11820-.git
cd ISO-11820-

# 编译并运行
mvn javafx:run -Djacoco.skip=true
```

### 运行测试

```bash
mvn test -Djacoco.skip=true
```

### IntelliJ IDEA 打开

1. File → Open → 选择项目根目录
2. IDEA 自动识别 Maven 项目，下载依赖
3. 右键 `ISO11820Application.java` → Run

---

## 七、项目结构

```
ISO-11820-
├── sql/
│   └── schema.sql                         # 数据库建表 + 初始数据
├── src/main/java/com/iso11820/
│   ├── ISO11820Application.java           # 程序入口
│   ├── core/                              # 业务核心层
│   │   ├── TestMaster.java                # 试验主控制器
│   │   ├── TestStateMachine.java          # 状态机
│   │   ├── TestState.java                 # 状态枚举
│   │   ├── TestContext.java               # 试验上下文
│   │   ├── SensorData.java                # 5通道温度数据
│   │   ├── SystemMessage.java             # 系统消息
│   │   ├── DataChangeListener.java        # 数据变更监听接口
│   │   └── simulation/
│   │       └── SensorSimulator.java       # 温度仿真引擎
│   ├── dao/                               # 数据持久层
│   │   ├── BaseDao.java                   # 通用CRUD基类
│   │   ├── util/DbUtil.java               # 数据库连接工具
│   │   ├── impl/                          # DAO实现
│   │   └── dto/                           # 数据传输对象
│   ├── entity/                            # 数据库实体
│   ├── service/                           # 服务层
│   │   ├── CsvDataService.java            # CSV温度数据读写
│   │   ├── ExcelReportService.java        # Excel报告导出
│   │   ├── PdfReportService.java          # PDF报告导出
│   │   └── ReportExportService.java       # 统一导出门面
│   ├── ui/                                # UI层
│   │   ├── LoginController.java           # 登录控制器
│   │   ├── MainController.java            # 主界面控制器
│   │   └── dialog/                        # 子窗口控制器
│   └── utils/                             # 工具层
│       ├── AppConfig.java                 # 配置管理
│       ├── LogUtil.java                   # 日志工具
│       ├── DateUtil.java                  # 日期时间
│       ├── NumUtil.java                   # 数值精度
│       ├── JsonUtil.java                  # JSON工具
│       └── FilePathManageUtil.java        # 文件路径管理
├── src/main/resources/
│   ├── fxml/                              # FXML界面文件
│   ├── appsettings.json                   # 配置文件
│   └── logback.xml                        # 日志配置
├── src/test/java/                         # 单元测试
├── docs/                                  # 文档
│   ├── ISO11820-开发文档.md
│   ├── DB-数据库设计.md
│   └── core-api.md
└── pom.xml                                # Maven配置
```

---

## 八、Git 提交规范

```
[模块] 简短描述

模块: ui | core | dao | utils | service | config | docs
类型: feat | fix | refactor | test
```

示例：
```
[ui] 实现参数设置窗口和按钮事件绑定
[core] 修复状态机线程安全问题
[dao] 修复updateResult空值约束异常
```

---

> **版本**: 1.0 | **日期**: 2026-07 | **课程**: 软件工程实践