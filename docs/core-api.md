# ISO 11820 核心层 API 对接文档

> **版本**: 1.0  
> **更新日期**: 2026-06-29  
> **目标读者**: 持久层开发、UI 层开发、测试工程师  
> **包路径**: `com.iso11820.core` / `com.iso11820.core.simulation`

---

## 一、包架构总览

```
com.iso11820.core
├── TestState              (enum)     试验状态枚举
├── SensorData             (class)    5 通道温度数据实体
├── TestContext            (class)    试验上下文实体
├── TestStateMachine       (class)    状态机——状态流转校验与自动回退
├── DataChangeListener     (interface) UI 解耦回调接口
├── SystemMessage          (record)   系统消息（时间戳 + 内容）
├── TestMaster             (class)    试验主控制器——顶层业务编排
│   └── TestResult         (record)   试验完成后的全量统计结果
│
└── simulation
    └── SensorSimulator    (class)    温度仿真引擎——4 阶段算法
```

**依赖方向**: 上层 → 下层，下层不感知上层  
`TestMaster` → `TestStateMachine` + `SensorSimulator` → `SensorData` / `TestState`

---

## 二、核心类职责说明

### 2.1 TestState（枚举）

| 枚举值 | 中文名 | 含义 |
|--------|--------|------|
| `IDLE` | 空闲 | 初始状态，等待开始升温 |
| `PREPARING` | 升温中 | 炉温从初始温度升至 750°C |
| `READY` | 就绪 | 温度稳定在 745~755°C，可开始记录 |
| `RECORDING` | 记录中 | 正在逐秒记录温度数据 |
| `COMPLETE` | 完成 | 记录阶段结束，等待保存试验记录 |

**关键方法**:

```java
String getDisplayName()                   // 返回中文名称，如 "升温中"
boolean canTransitionTo(TestState target) // 判断是否允许切换到目标状态
```

### 2.2 SensorData（类）

5 通道温度快照，单位 °C，保留 1 位小数。线程安全（volatile 字段）。

| 字段 | 通道名 | 含义 |
|------|--------|------|
| `tf1` | 炉温1 (TF1) | 加热炉内主温度，目标 750°C |
| `tf2` | 炉温2 (TF2) | 加热炉内副温度，独立噪声 |
| `ts` | 表面温 (TS) | 样品表面温度 |
| `tc` | 中心温 (TC) | 样品中心温度 |
| `tCal` | 校准温 (TCal) | 标定用，= TF1 + 噪声×2，不画曲线 |

**关键方法**:

```java
SensorData()                                              // 全 0 初始值
SensorData(double tf1, double tf2, double ts, double tc, double tCal) // 全参数构造
SensorData(SensorData other)                              // 拷贝构造
double getTf1() / setTf1(double)                          // 各通道 getter/setter
void setAll(double tf1, double tf2, double ts, double tc, double tCal) // 批量更新
void copyFrom(SensorData source)                          // 快照复制
static String format(double value)                        // 格式化为 "750.0"
```

### 2.3 TestContext（类）

试验上下文实体，存储一次试验的核心标识和参数。

| 字段 | 类型 | 可变性 | 说明 |
|------|------|--------|------|
| `productId` | String | final | 样品编号 |
| `testId` | String | final | 试验 ID（yyyyMMdd-HHmmss） |
| `ambientTemp` | double | volatile | 环境温度 °C |
| `ambientHumidity` | double | volatile | 环境湿度 % |
| `preWeight` | double | volatile | 试验前质量 g |
| `postWeight` | double | volatile | 试验后质量 g |
| `currentState` | TestState | volatile | 当前状态 |
| `recordedDuration` | int | volatile | 已记录秒数 |

### 2.4 TestStateMachine（类）

状态机基类，管理状态流转校验与自动回退。UI 层通常不直接使用，由 TestMaster 内部持有。

**关键方法**:

```java
TestState getCurrentState()                        // 获取当前状态
boolean isState(TestState state)                   // 判断是否处于指定状态
boolean isActive()                                 // 是否活跃状态（非 IDLE/COMPLETE）
boolean transitionTo(TestState target)             // 合法流转校验 + 切换
void forceTransitionTo(TestState target)           // 强制切换（跳过校验）
void reset()                                       // 重置到 IDLE
boolean isTemperatureStable(double tf1)            // 745~755°C 范围检查
boolean checkAndHandleReadiness(double tf1)        // READY 温度不达标自动回退
void addStateChangeListener(Consumer<StateChangeEvent>)  // 注册状态变更监听
```

### 2.5 SensorSimulator（类）

温度仿真引擎，实现 4 阶段算法（升温/稳定/记录/降温）。每调用一次 `update()` 执行一次 800ms 迭代。由 TestMaster 内部持有，UI 层通常无需直接操作。

**关键方法**:

```java
SensorData update()                          // 执行一次迭代，返回最新温度快照
boolean isStable()                           // 炉温是否稳定
boolean hasReachedStableThreshold()          // 是否达到稳定门槛（747°C）
void setCurrentState(TestState state)        // 设置试验状态
void reset()                                 // 重置到初始温度
SensorData getCurrentData()                  // 获取温度快照副本
```

### 2.6 SystemMessage（record）

不可变消息记录，含时间戳（HH:mm:ss）和消息内容。

```java
record SystemMessage(String time, String message)
static SystemMessage now(String message)     // 自动使用当前时间
String toString()                            // "18:28:14  温度已稳定，可以开始记录"
```

### 2.7 DataChangeListener（接口）

`@FunctionalInterface`，UI 层通过此接口接收实时数据推送。

```java
void onDataChanged(SensorData data, TestState state, int recordedDuration, String latestMessage)
```

---

## 三、TestMaster 全部公共方法

> **核心原则**: 所有 `boolean` 返回值的控制方法均幂等设计——非法状态调用返回 `false`，不抛异常。

### 3.1 构造方法

```java
TestMaster()
```
默认构造。仿真参数：目标 750°C、升温 40°C/s、噪声 ±0.5°C、稳定阈值 3°C、初始 25°C。随机种子。

```java
TestMaster(long seed)
```
指定随机种子，用于可重现的单元测试。

---

### 3.2 生命周期控制

#### `boolean startHeating()`

**功能**: 从 IDLE 进入 PREPARING，启动 800ms 定时调度器。

**前置条件**: 当前状态 = IDLE

**返回值**: `true` 成功；`false` 当前不是 IDLE、调度器已运行、或 COMPLETE 未保存

**状态影响**: IDLE → PREPARING

**生成消息**: `"开始升温，系统升温中"`

**阻塞条件（ISO 11820 保存保护）**: 若当前为 COMPLETE 且 `isSaved() == false`，返回 false 并生成消息 `"试验已完成但未保存，请先保存试验记录"`

```java
// 调用示例
TestMaster master = new TestMaster();
if (master.startHeating()) {
    System.out.println("升温已启动");
} else {
    System.out.println("无法启动升温: " + master.getCurrentState().getDisplayName());
}
```

---

#### `boolean startRecording(int durationSeconds)`

**功能**: 从 READY 进入 RECORDING，开始逐秒记录温度数据。

**前置条件**: 当前状态 = READY

**入参**:
| 参数 | 类型 | 说明 |
|------|------|------|
| `durationSeconds` | int | 目标记录时长（秒），标准 60 分钟传 `TestMaster.STANDARD_DURATION_SECONDS`（3600） |

**返回值**: `true` 成功；`false` 当前不是 READY、或 COMPLETE 未保存

**状态影响**: READY → RECORDING

**副作用**:
- 计算恒功率值（READY 阶段功率采样队列的平均值）
- 清空时序数据缓存
- 重置统计数据累加器
- 重置温漂校验时刻

**生成消息**: `"开始记录，计时开始"`

**异常**: `IllegalArgumentException` — 如果 `durationSeconds <= 0`

```java
// 标准 60 分钟试验
master.startRecording(TestMaster.STANDARD_DURATION_SECONDS);

// 自定义 10 分钟快速试验
master.startRecording(600);
```

---

#### `boolean stopRecording()`

**功能**: 手动停止记录，从 RECORDING 切换到 COMPLETE。

**前置条件**: 当前状态 = RECORDING

**返回值**: `true` 成功停止并进入 COMPLETE；`false` 当前不是 RECORDING 或记录时长不足 30 秒

**状态影响**: RECORDING → COMPLETE（≥30 秒）或 RECORDING → READY（< 30 秒）

**ISO 11820 有效数据判定**: 若累计记录时长 < 30 秒，视为无效试验：
- 清空时序数据缓存和统计数据
- 强制回退到 READY
- 生成消息 `"记录时长不足30秒，试验无效，退回就绪状态"`
- 返回 `false`

**生成消息**: `"用户手动停止记录"`（成功时）

```java
if (master.stopRecording()) {
    TestMaster.TestResult result = master.getTestResult();
    System.out.println("试验完成: " + result.toSummaryString());
} else {
    System.out.println("停止失败，当前状态: " + master.getCurrentState().getDisplayName());
}
```

---

#### `void stopHeating()`

**功能**: 停止加热，关闭调度器，强制进入 IDLE，触发降温逻辑。

**前置条件**: 无（可从任意状态调用）

**返回值**: void

**状态影响**: 任意 → IDLE

**副作用**:
- 关闭调度器（优雅等待 2 秒）
- 清空功率采样队列
- 仿真引擎切换为 IDLE（降温算法）

**生成消息**: `"停止加热"`

```java
master.stopHeating();  // 可从任意状态调用
```

---

#### `void reset()`

**功能**: 重置全部状态，清空所有缓存，恢复到初始状态。

**前置条件**: 无（可从任意状态调用）

**阻塞条件（ISO 11820 保存保护）**: 若当前为 COMPLETE 且 `isSaved() == false`，方法直接返回不执行任何操作，并生成消息 `"试验已完成但未保存，请先保存试验记录再重置"`

**副作用**:
- 关闭调度器
- 清空 `saveFlag`（保存状态标记）
- 清空功率采样队列和恒功率值
- 清空时序数据缓存、消息队列、统计数据
- 重置仿真引擎到初始温度（25°C）
- 重置状态机到 IDLE

**生成消息**: `"系统已重置"`

---

### 3.3 保存状态管理

#### `boolean isSaved()`

**功能**: 查询试验是否已保存。

**返回值**: `true` 如果 `saveFlag == "10000000"`（对应数据库 `testmaster.flag` 字段）

```java
if (master.getCurrentState() == TestState.COMPLETE && !master.isSaved()) {
    // 提示用户保存试验记录
}
```

---

#### `void markSaved()`

**功能**: 标记试验为已保存状态。

**副作用**: 将 `saveFlag` 置为 `"10000000"`，标记后允许新建试验或重置。

```java
// 持久层保存试验记录后调用
master.markSaved();
```

---

### 3.4 数据查询

#### 温度数据

```java
SensorData getCurrentData()       // 当前 5 通道温度快照（只读副本）
List<SensorData> getRecordedData() // 记录阶段采集的时序温度数据（不可变列表）
int getRecordedDataCount()         // 时序数据点数
```

#### 状态查询

```java
TestState getCurrentState()        // 当前试验状态
boolean isSchedulerRunning()       // 调度器是否正在运行
```

#### 记录时长

```java
int getRecordedDuration()          // 已记录时长（秒），非 RECORDING 下为 0
int getTargetDurationSeconds()     // 目标记录时长（秒）
```

#### 系统消息

```java
List<SystemMessage> getMessages()  // 消息队列（不可变列表）
int getMessageCount()              // 消息数量
String getLatestMessage()          // 最新一条消息内容，无消息时为 null
```

#### 恒功率值

```java
double getConstantPower()          // 恒功率基准值（kW），保留 1 位小数
```

#### 温漂值

```java
double getTempDrift()              // 温漂值（°C/10min），保留 2 位小数；未计算时返回 Double.MAX_VALUE
```

---

### 3.5 统计数据查询

#### 最大值及出现时间（°C / 秒）

```java
double getMaxTf1()    int getMaxTf1Time()
double getMaxTf2()    int getMaxTf2Time()
double getMaxTs()     int getMaxTsTime()
double getMaxTc()     int getMaxTcTime()
```

#### 最小值（°C）

```java
double getMinTf1()    double getMinTf2()
double getMinTs()     double getMinTc()
```

#### 最终值（°C）

```java
double getFinalTf1()  double getFinalTf2()
double getFinalTs()   double getFinalTc()
```

#### 温升（°C，最终值 − 初始值）

```java
double getDeltaTf1()  double getDeltaTf2()
double getDeltaTs()   double getDeltaTc()
double getDeltaTf()   // 综合温升，取表面温升（文档规定）
```

#### 平均值（°C）

```java
double getAvgTf1()    double getAvgTf2()
double getAvgTs()     double getAvgTc()
```

#### 监听器管理

```java
void addDataChangeListener(DataChangeListener listener)
void removeDataChangeListener(DataChangeListener listener)
int getListenerCount()
```

#### 内部组件（高级用例）

```java
TestStateMachine getStateMachine()   // 内部状态机实例
SensorSimulator getSimulator()       // 内部仿真引擎实例
```

---

### 3.6 试验结果封装

#### `TestResult getTestResult()`

**功能**: 获取试验完成后的全量统计结果封装。

**返回值**: `TestResult` 对象（COMPLETE 状态下）；`null`（非 COMPLETE 状态下）

```java
TestMaster.TestResult result = master.getTestResult();
if (result != null) {
    System.out.println("炉温1 最大值: " + result.maxTf1() + "°C @" + result.maxTf1Time() + "s");
    System.out.println("综合温升: " + result.deltaTf() + "°C");
    System.out.println("恒功率: " + result.constantPower() + "kW");
    System.out.println(result.toSummaryString());
}
```

---

## 四、DataChangeListener 监听器回调说明

### 4.1 接口签名

```java
@FunctionalInterface
public interface DataChangeListener {
    void onDataChanged(
        SensorData data,          // 当前 5 通道温度快照（不可变副本）
        TestState state,          // 当前试验状态
        int recordedDuration,     // 已记录时长（秒），非 RECORDING 下为 0
        String latestMessage      // 本轮迭代的最新系统消息，无新消息时为 null
    );
}
```

### 4.2 回调时机

- 每次仿真迭代完成后（每 800ms）
- 控制方法（start/stop）执行完成后
- 在**调度线程**内同步调用（非 UI 线程）

### 4.3 线程安全约定

> ⚠️ **核心层不做 UI 线程切换**。回调在后台线程 `TestMaster-Scheduler` 中触发，UI 层必须自行处理跨线程刷新。

**JavaFX 示例**:

```java
master.addDataChangeListener((data, state, duration, msg) -> {
    Platform.runLater(() -> {
        // 以下代码在 JavaFX Application Thread 中执行
        tf1Label.setText(String.format("%.1f °C", data.getTf1()));
        statusLabel.setText(state.getDisplayName());
        timerLabel.setText(duration + " 秒");
        if (msg != null) {
            logArea.appendText(msg + "\n");
        }
    });
});
```

**Swing 示例**:

```java
master.addDataChangeListener((data, state, duration, msg) -> {
    SwingUtilities.invokeLater(() -> {
        // 以下代码在 Event Dispatch Thread 中执行
        tf1Label.setText(String.format("%.1f °C", data.getTf1()));
        statusLabel.setText(state.getDisplayName());
        if (msg != null) logArea.append(msg + "\n");
    });
});
```

### 4.4 回调中的 `latestMessage` 触发时机

| 触发时机 | latestMessage 内容 |
|---------|-------------------|
| `startHeating()` 成功 | `"开始升温，系统升温中"` |
| 温度稳定自动切 READY | `"温度已稳定，可以开始记录"` |
| READY 温度跌出自动回退 | `"温度跌出稳定范围（炉温1=XXX°C），自动退回升温状态"` |
| `startRecording()` 成功 | `"开始记录，计时开始"` |
| 到达目标时长自动完成 | `"记录时间到达 X 秒，试验自动结束"` |
| 温漂满足提前终止 | `"满足终止条件（温漂 X °C/10min < 2.0），试验提前结束"` |
| `stopRecording()` 成功 | `"用户手动停止记录"` |
| `stopRecording()` 无效（< 30s） | `"记录时长不足30秒，试验无效，退回就绪状态"` |
| `stopHeating()` | `"停止加热"` |
| `reset()` | `"系统已重置"` |
| 保存保护拦截 | `"试验已完成但未保存，请先保存试验记录"` |
| 无新消息 | `null` |

---

## 五、状态流转完整规则表

### 5.1 合法流转矩阵

| 当前状态 ↓ / 目标状态 → | IDLE | PREPARING | READY | RECORDING | COMPLETE |
|-------------------------|:----:|:---------:|:-----:|:---------:|:--------:|
| **IDLE** | — | ✅ | ❌ | ❌ | ❌ |
| **PREPARING** | ✅ | — | ✅ | ❌ | ❌ |
| **READY** | ✅ | ✅ | — | ✅ | ❌ |
| **RECORDING** | ❌ | ✅ | ❌ | — | ✅ |
| **COMPLETE** | ❌ | ✅ | ❌ | ❌ | — |

### 5.2 流转触发方式

| 流转 | 触发方式 | 类型 |
|------|---------|------|
| IDLE → PREPARING | `startHeating()` | 手动 |
| PREPARING → READY | 炉温稳定（745~755°C 且稳定计数 > 3） | 自动 |
| PREPARING → IDLE | `stopHeating()` | 手动 |
| READY → RECORDING | `startRecording(n)` | 手动 |
| READY → PREPARING | 炉温跌出 745~755°C | 自动回退 |
| READY → IDLE | `stopHeating()` | 手动 |
| RECORDING → COMPLETE | 到达目标时长 / 温漂满足提前终止 / `stopRecording()` | 自动/手动 |
| RECORDING → PREPARING | `stopRecording()` 且记录时长 < 30 秒 | 手动（无效试验） |
| COMPLETE → PREPARING | 保存试验记录后（由上层控制） | 手动 |

### 5.3 自动终止规则

| 规则 | 条件 | 说明 |
|------|------|------|
| 强制终止 | 累计记录时长 ≥ 3600 秒 | 60 分钟无条件终止 |
| 提前终止 | 每 5 分钟校验温漂 < 2°C/10min | 满足条件自动结束 |
| 目标时长终止 | 累计记录时长 ≥ `targetDurationSeconds` | 自定义时长到达 |
| 有效数据判定 | 手动停止时 ≥ 30 秒 | 不足 30 秒退回 READY |

---

## 六、TestResult 字段说明

`TestMaster.TestResult` 是一个 `record`，包含试验完成后的全量统计数据。

### 6.1 字段一览

| 字段 | 类型 | 说明 |
|------|------|------|
| `maxTf1` | double | 炉温1 最大值（°C） |
| `maxTf1Time` | int | 炉温1 最大值出现时间（秒） |
| `maxTf2` | double | 炉温2 最大值（°C） |
| `maxTf2Time` | int | 炉温2 最大值出现时间（秒） |
| `maxTs` | double | 表面温 最大值（°C） |
| `maxTsTime` | int | 表面温 最大值出现时间（秒） |
| `maxTc` | double | 中心温 最大值（°C） |
| `maxTcTime` | int | 中心温 最大值出现时间（秒） |
| `minTf1` | double | 炉温1 最小值（°C） |
| `minTf2` | double | 炉温2 最小值（°C） |
| `minTs` | double | 表面温 最小值（°C） |
| `minTc` | double | 中心温 最小值（°C） |
| `finalTf1` | double | 炉温1 最终值（°C） |
| `finalTf2` | double | 炉温2 最终值（°C） |
| `finalTs` | double | 表面温 最终值（°C） |
| `finalTc` | double | 中心温 最终值（°C） |
| `deltaTf1` | double | 炉温1 温升（°C） |
| `deltaTf2` | double | 炉温2 温升（°C） |
| `deltaTs` | double | 表面温 温升（°C） |
| `deltaTc` | double | 中心温 温升（°C） |
| `deltaTf` | double | **综合温升**（°C），取表面温升 |
| `avgTf1` | double | 炉温1 平均值（°C） |
| `avgTf2` | double | 炉温2 平均值（°C） |
| `avgTs` | double | 表面温 平均值（°C） |
| `avgTc` | double | 中心温 平均值（°C） |
| `totalRecordTime` | int | 总记录时长（秒） |
| `constantPower` | double | 恒功率值（kW），保留 1 位小数 |
| `tempDrift` | double | 温漂值（°C/10min），保留 2 位小数 |

### 6.2 使用示例

```java
TestMaster.TestResult r = master.getTestResult();
if (r == null) return;

// 持久层：写入 testmaster 表
String sql = """
    UPDATE testmaster SET
        maxtf1 = ?, maxtf1_time = ?,
        maxtf2 = ?, maxtf2_time = ?,
        maxts  = ?, maxts_time  = ?,
        maxtc  = ?, maxtc_time  = ?,
        finaltf1 = ?, finaltf2 = ?, finalts = ?, finaltc = ?,
        deltatf1 = ?, deltatf2 = ?, deltatf = ?, deltats = ?, deltatc = ?,
        totaltesttime = ?, constpower = ?
    WHERE productid = ? AND testid = ?
    """;

// 判定结论（ISO 11820 简化版）
boolean passed = r.deltaTf() <= 50.0
              && r.totalRecordTime() >= 60;

// 打印摘要
System.out.println(r.toSummaryString());
// 输出: maxTf1=750.5°C@7s, deltaTf=103.9°C, avgTf1=750.0°C,
//       duration=3600s, constPower=130.0kW, drift=0.05°C/10min
```

---

## 七、线程安全与调用注意事项

### 7.1 线程模型

```
┌─────────────────────────────────────────────────────┐
│                   UI Thread                          │
│  (JavaFX Application Thread / Swing EDT)            │
│                                                     │
│  调用控制方法: startHeating(), stopRecording(), ...  │
│  读取数据: getCurrentData(), getTestResult(), ...    │
│  注册/移除监听器: addDataChangeListener(), ...       │
└──────────────────┬──────────────────────────────────┘
                   │ synchronized (tickLock)
                   ▼
┌─────────────────────────────────────────────────────┐
│             TestMaster-Scheduler (后台线程)          │
│                                                     │
│  onTick() 每 800ms 执行:                            │
│    1. simulator.update()                            │
│    2. 状态检查 & 自动切换                            │
│    3. 数据追加 & 统计更新                            │
│    4. 锁外通知所有 DataChangeListener                │
└─────────────────────────────────────────────────────┘
```

### 7.2 关键约定

| # | 约定 | 说明 |
|---|------|------|
| 1 | **UI 线程切换** | 核心层回调在后台线程，UI 必须用 `Platform.runLater()` / `SwingUtilities.invokeLater()` 切换 |
| 2 | **控制方法互斥** | `startHeating()`/`startRecording()`/`stopRecording()`/`stopHeating()`/`reset()` 与 `onTick()` 通过 `tickLock` 互斥 |
| 3 | **监听器锁外调用** | 避免回调中调用控制方法导致死锁 |
| 4 | **数据只读副本** | `getCurrentData()` 和 `getRecordedData()` 返回不可变副本，外部修改不影响内部状态 |
| 5 | **幂等设计** | 所有 `boolean` 控制方法非法状态返回 `false`，不抛异常；`void` 方法可从任意状态调用 |
| 6 | **调度器生命周期** | `startHeating()` 创建守护线程调度器，`stopHeating()`/`reset()` 优雅关闭（等 2 秒） |
| 7 | **保存保护** | COMPLETE 且未保存时，`startHeating()`/`startRecording()`/`reset()` 被拦截 |
| 8 | **统计数据可用性** | 仅在 COMPLETE 状态下统计数据有效；`getTestResult()` 在非 COMPLETE 下返回 `null` |

### 7.3 典型生命周期

```java
// 1. 创建控制器
TestMaster master = new TestMaster();

// 2. 注册 UI 监听器（必须在 startHeating 之前注册）
master.addDataChangeListener((data, state, duration, msg) -> {
    Platform.runLater(() -> { /* UI 更新 */ });
});

// 3. 开始升温 → 等待自动 READY
master.startHeating();

// 4. 用户点击"开始记录"（在 READY 状态下）
if (master.getCurrentState() == TestState.READY) {
    master.startRecording(3600);
}

// 5. 等待自动完成（或手动停止）
// ... 3600 秒后自动 COMPLETE，或用户点击"停止记录" ...

// 6. 获取结果
TestMaster.TestResult result = master.getTestResult();

// 7. 持久层保存后标记
master.markSaved();

// 8. 停止加热/重置
master.stopHeating();
// 或 master.reset();  // 完全重置
```

### 7.4 注意事项

- **不要在监听器回调中调用控制方法**：回调在锁外执行，但调用控制方法会尝试获取 `tickLock`，可能导致意外行为。如需在回调中触发操作，使用 `Platform.runLater()` 延迟到下一帧。
- **`getTestResult()` 仅在 COMPLETE 时返回非 null**：在 PREPARING/READY/RECORDING 状态下调用返回 `null`。
- **`tempDrift` 需要 ≥ 1 分钟数据**：数据不足时返回 `Double.MAX_VALUE`，UI 层应判断并显示为 "—" 或 "数据不足"。
- **`reset()` 会清空 `saveFlag`**：重置后需要重新保存试验记录。
- **调度器线程为守护线程**：JVM 退出时自动终止，不会阻止程序关闭。

---

## 附录 A：常量速查

| 常量 | 位置 | 值 | 说明 |
|------|------|---|------|
| `TestMaster.STANDARD_DURATION_SECONDS` | TestMaster | 3600 | 标准 60 分钟试验 |
| `TestStateMachine.STABLE_TEMP_MIN` | TestStateMachine | 745.0 | 稳定范围下限 °C |
| `TestStateMachine.STABLE_TEMP_MAX` | TestStateMachine | 755.0 | 稳定范围上限 °C |
| `SensorSimulator` 默认目标温度 | SensorSimulator | 750.0 | 目标炉温 °C |
| `SensorSimulator` 默认升温速率 | SensorSimulator | 40.0 | °C/s |
| `SensorSimulator` 默认温度波动 | SensorSimulator | 0.5 | °C |
| 调度器间隔 | TestMaster | 800ms | 硬件采集周期 |

## 附录 B：系统消息完整列表

| 触发条件 | 消息内容 |
|---------|---------|
| `startHeating()` | `开始升温，系统升温中` |
| PREPARING→READY 自动 | `温度已稳定，可以开始记录` |
| READY→PREPARING 自动回退 | `温度跌出稳定范围（炉温1=XXX.X°C），自动退回升温状态` |
| `startRecording()` | `开始记录，计时开始` |
| 到达目标时长 | `记录时间到达 X 秒，试验自动结束` |
| 温漂提前终止 | `满足终止条件（温漂 X.XX ℃/10min < 2.0 ℃/10min），试验提前结束` |
| `stopRecording()` 成功 | `用户手动停止记录` |
| `stopRecording()` 无效 | `记录时长不足30秒，试验无效，退回就绪状态` |
| `stopHeating()` | `停止加热` |
| `reset()` | `系统已重置` |
| 保存保护拦截 | `试验已完成但未保存，请先保存试验记录` |
| 保存保护拦截（reset） | `试验已完成但未保存，请先保存试验记录再重置` |