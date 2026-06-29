# ISO 11820 数据持久层使用手册

> **版本**: 1.0  
> **更新日期**: 2026-06-29  
> **目标读者**: 核心层开发、UI 层开发、测试工程师  
> **包路径**: `com.iso11820.dao`

---

## 一、环境依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| `org.xerial:sqlite-jdbc` | 3.45.1.0 | SQLite JDBC 驱动 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.16.1 | JSON 序列化（校准记录） |

已在 `pom.xml` 中配置，无需额外安装。

---

## 二、数据库初始化

### 2.1 自动初始化

首次调用任意 DAO 方法时，系统自动执行：

1. 创建 `Data/ISO11820.db` 数据库文件
2. 执行 `sql/schema.sql` 建表脚本（CREATE TABLE IF NOT EXISTS，幂等）
3. 插入初始数据（INSERT ... WHERE NOT EXISTS，防重复）
4. 检查数据库版本并执行升级（`system_config` 表的 `db_version`）

```java
// 无需手动初始化，首次调用自动完成
OperatorDao dao = new OperatorDaoImpl();
boolean ok = dao.login("admin", "123456"); // 自动建表
```

### 2.2 自定义数据库路径

```java
// 必须在首次调用 DAO 之前设置
DbUtil.setDbPath("D:/MyApp/custom.db");
```

### 2.3 数据库版本管理

- 当前版本存储在 `system_config` 表中（`config_key = 'db_version'`）
- 新增字段/表时，在 `DbUtil.checkAndUpgrade()` 中添加升级方法
- 升级脚本幂等执行，重复调用不会报错

---

## 三、DAO 层架构

```
Dao接口 (定义业务方法)
    ↑
DaoImpl (extends BaseDao<T> implements Dao接口)
    ↑
BaseDao<T> (反射映射 + PreparedStatement 封装)
    ↑
DbUtil (连接管理 + 事务 + 版本管理)
```

### 3.1 核心类职责

| 类 | 位置 | 职责 |
|----|------|------|
| `DbUtil` | `dao.util` | 连接获取、建表初始化、事务管理、版本升级、资源关闭 |
| `BaseDao<T>` | `dao` | 泛型 CRUD 基类：查询、更新、批量执行、反射映射 |
| `DaoException` | `dao` | 自定义运行时异常 |
| `DaoChecker` | `dao.util` | 数据完整性校验（存在性、字段合法性） |
| `EntityConverter` | `dao.support` | 实体 ↔ DTO 双向转换 |

---

## 四、各 DAO 方法速查

### 4.1 OperatorDao（操作员表）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `login(username, pwd)` | `boolean` | 验证用户名密码 |
| `getByUsername(username)` | `Operators` | 按用户名查询完整信息 |

```java
OperatorDao dao = new OperatorDaoImpl();
if (dao.login("admin", "123456")) {
    Operators op = dao.getByUsername("admin");
    System.out.println("角色: " + op.getUsertype());
}
```

### 4.2 ApparatusDao（设备表）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getById(apparatusId)` | `Apparatus` | 按ID查询设备 |
| `listAll()` | `List<Apparatus>` | 查询所有设备 |
| `updateConstPower(id, constPower)` | `int` | 更新恒功率值 |

### 4.3 ProductMasterDao（样品表）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `insert(product)` | `int` | 新增样品 |
| `update(product)` | `int` | 更新样品信息 |
| `deleteById(productId)` | `int` | 删除样品 |
| `getById(productId)` | `ProductMaster` | 按编号查询 |
| `fuzzySearch(keyword)` | `List<ProductMaster>` | 按编号/名称模糊查询 |
| `listAll()` | `List<ProductMaster>` | 查询所有样品 |

### 4.4 SensorsDao（传感器表）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `listByGroup(sensorgroup)` | `List<Sensors>` | 按分组查询（采集/校准/备用） |
| `getById(sensorId)` | `Sensors` | 按通道ID查询 |
| `updateOutputValue(id, outVal, inVal)` | `int` | 更新单个传感器实时值 |
| `batchUpdateOutputValue(list)` | `int[]` | 批量更新传感器实时值 |

```java
SensorsDao dao = new SensorsDaoImpl();
// 每 800ms 批量更新 5 个核心通道
List<Sensors> list = dao.listByGroup("采集");
for (Sensors s : list) {
    s.setOutputvalue(simulator.getCurrentValue(s.getSensorid()));
}
int[] results = dao.batchUpdateOutputValue(list);
```

### 4.5 TestMasterDao（试验记录表）⭐

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `insert(testMaster)` | `int` | 新建试验（统计字段初始为0） |
| `getByKey(pid, tid)` | `TestMaster` | 按联合主键查询 |
| `updateResult(testMaster)` | `int` | 更新试验结果（非事务） |
| `updateResultWithTransaction(testMaster)` | `boolean` | 事务版更新（原子性保证） |
| `hasUnfinishedTest()` | `boolean` | 是否存在未保存试验 |
| `getUnfinishedTest()` | `TestMaster` | 查询未保存试验（LIMIT 1） |
| `updateFlag(pid, tid, flag)` | `int` | 单独更新标记（"10000000"=已保存） |
| `queryByCondition(...)` | `List<TestMaster>` | 多条件分页查询 |
| `countByDateRange(from, to)` | `long` | 统计日期范围内试验数 |

**queryByCondition 参数说明**：

```java
List<TestMaster> queryByCondition(
    String startDate,        // 开始日期（含），null=不限制
    String endDate,          // 结束日期（含），null=不限制
    String productIdKeyword, // 样品编号关键字（模糊），null=不限制
    String operator,         // 操作员（精确匹配），null=不限制
    int pageNum,             // 页码（从1开始）
    int pageSize             // 每页记录数
)
```

### 4.6 CalibrationRecordsDao（校准记录表）

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `insert(record)` | `int` | 新增校准记录 |
| `getById(id)` | `CalibrationRecords` | 按ID查询 |
| `getByIdWithJson(id)` | `CalibrationRecords` | 按ID查询（自动反序列化JSON） |
| `insertWithJson(r, map1, map2)` | `int` | 新增（自动序列化Map→JSON） |
| `listByDateRange(from, to)` | `List<CalibrationRecords>` | 按日期范围查询 |
| `listByApparatusId(id)` | `List<CalibrationRecords>` | 按设备ID查询 |
| `getLatestByApparatusId(id)` | `CalibrationRecords` | 最新校准记录 |
| `listByOperator(op)` | `List<CalibrationRecords>` | 按操作员查询 |

```java
CalibrationRecordsDao dao = new CalibrationRecordsDaoImpl();

// 写入：自动序列化 Map → JSON
CalibrationRecords r = new CalibrationRecords();
r.setId(UUID.randomUUID().toString());
r.setCalibrationDate("2026-06-29T10:00:00");
r.setCalibrationType("Surface");
r.setApparatusId(0);
r.setOperator("admin");
// ... 填充其他字段 ...

Map<String, Double> tempData = Map.of("A1", 750.0, "A2", 749.5, "B1", 750.2);
dao.insertWithJson(r, tempData, null);

// 查询：自动反序列化 JSON → 验证
CalibrationRecords result = dao.getByIdWithJson(r.getId());
```

---

## 五、事务使用规范

```java
TestMasterDao dao = new TestMasterDaoImpl();

// 方式1：使用内置事务方法（推荐）
boolean success = dao.updateResultWithTransaction(testMaster);

// 方式2：手动管理事务
Connection conn = null;
try {
    conn = DbUtil.beginTransaction();
    // ... 执行多个数据库操作 ...
    DbUtil.commitTransaction(conn);
} catch (Exception e) {
    DbUtil.rollbackTransaction(conn);
    throw new DaoException("事务失败", e);
} finally {
    DbUtil.closeTransactionConnection(conn);
}
```

> ⚠️ **注意**：事务内必须复用同一个 Connection，禁止在事务内调用 DAO 的普通方法（会获取新连接）。

---

## 六、批量操作

```java
SensorsDao dao = new SensorsDaoImpl();

// 每 800ms 批量更新 5 个核心通道
List<Sensors> sensorList = dao.listByGroup("采集");
// 更新温度值...
int[] results = dao.batchUpdateOutputValue(sensorList);
// results = [1, 1, 1, 1, 1] 表示 5 个通道全部更新成功
```

批量操作比逐条更新减少约 80% 的数据库开销。

---

## 七、实体转换（EntityConverter）

```java
// 1. DAO 查询结果 → 业务 DTO
TestMaster tm = dao.getByKey("P001", "20260629-120000");
TestResultDTO dto = EntityConverter.toTestResultDTO(tm);

// 2. 业务参数 → 实体（新建试验）
TestResultDTO dto = new TestResultDTO();
dto.setProductId("P001");
// ... 填充字段 ...
TestMaster tm = EntityConverter.toTestMaster(dto);
dao.insert(tm);

// 3. 传感器列表 → 5通道温度快照
List<Sensors> sensorList = sensorDao.listByGroup("采集");
SensorDataDTO tempSnapshot = EntityConverter.toSensorDataDTO(sensorList);
System.out.println("炉温1: " + tempSnapshot.getTf1() + "°C");
```

---

## 八、数据校验（DaoChecker）

```java
// 新建试验前校验
if (!DaoChecker.checkProductExists("P001")) {
    throw new DaoException("样品编号不存在");
}

// 全字段合法性校验
DaoChecker.validateTestMaster(testMaster); // 非法数据直接抛异常
```

---

## 九、常见问题排查

### 9.1 数据库文件路径问题

```
错误: "获取数据库连接失败: Data/ISO11820.db"
解决: 确保工作目录下存在 Data 目录，或调用 DbUtil.setDbPath() 设置绝对路径
```

### 9.2 外键约束报错

```
错误: "FOREIGN KEY constraint failed"
原因: testmaster.productid 引用的 productmaster 记录不存在
解决: 新建试验前先调用 DaoChecker.checkProductExists() 校验
```

### 9.3 空值处理

所有 DAO 方法入参允许 null，但关键字段（productid、testid）为 null 时会抛出 DaoException。查询无结果时返回 null 或空列表，调用方需做空值判断。

### 9.4 建表脚本未找到

```
错误: "建表脚本未找到: sql/schema.sql"
解决: 确保 sql/schema.sql 在 classpath 中（src/main/resources 或工作目录）
```

### 9.5 线程安全

`BaseDao` 的字段反射缓存使用 `ConcurrentHashMap`，多线程安全。但单个 Connection 不是线程安全的，不同线程应使用各自的连接。

---

## 十、联调示例：完整试验流程

```java
// ===== 1. 初始化 =====
OperatorDao operatorDao = new OperatorDaoImpl();
ProductMasterDao productDao = new ProductMasterDaoImpl();
TestMasterDao testDao = new TestMasterDaoImpl();
SensorsDao sensorDao = new SensorsDaoImpl();

// ===== 2. 登录 =====
if (!operatorDao.login("admin", "123456")) {
    throw new RuntimeException("登录失败");
}

// ===== 3. 新建试验 =====
// 3.1 确保样品存在
if (!DaoChecker.checkProductExists("20240613-001")) {
    ProductMaster p = new ProductMaster();
    p.setProductid("20240613-001");
    p.setProductname("岩棉隔热板");
    p.setSpecific("100×50×25mm");
    p.setDiameter(100.0);
    p.setHeight(50.0);
    productDao.insert(p);
}

// 3.2 创建试验记录
TestMaster tm = new TestMaster();
tm.setProductid("20240613-001");
tm.setTestid("20260629-143025");
tm.setTestdate("2026-06-29");
tm.setAmbtemp(25.0);
tm.setAmbhumi(60.0);
tm.setAccording("ISO 11820:2022");
tm.setOperator("admin");
tm.setApparatusid("FURNACE-01");
tm.setApparatusname("一号试验炉");
tm.setApparatuschkdate("2026-06-29");
tm.setRptno("20240613-001");
tm.setPreweight(50.0);

// 3.3 校验并插入
DaoChecker.validateTestMaster(tm);
testDao.insert(tm);

// ===== 4. 试验完成，保存结果 =====
tm.setPostweight(45.0);
tm.setLostweight(5.0);
tm.setLostweightPer(10.0);
tm.setTotaltesttime(3600);
tm.setConstpower(2048);
tm.setPhenocode("1,2");
tm.setFlametime(0);
tm.setFlameduration(0);
tm.setMaxtf1(750.5);
tm.setMaxtf1Time(7);
// ... 填充其他统计字段 ...
tm.setFlag("10000000");

// 使用事务保证原子性
boolean saved = testDao.updateResultWithTransaction(tm);
if (saved) {
    System.out.println("试验结果已保存");
}

// ===== 5. 查询历史 =====
List<TestMaster> history = testDao.queryByCondition(
    "2026-01-01", "2026-12-31", null, "admin", 1, 20);
for (TestMaster record : history) {
    TestResultDTO dto = EntityConverter.toTestResultDTO(record);
    System.out.println("试验: " + dto.getTestId()
        + " 温升: " + dto.getDeltaTf() + "°C"
        + " 时长: " + dto.getTotalRecordTime() + "s");
}
```

---

## 十一、单元测试运行

```bash
# 运行所有 DAO 层测试
mvn test -pl . -Dtest="com.iso11820.dao.*"

# 运行单个测试类
mvn test -Dtest="TestMasterDaoTest"

# 运行单个测试方法
mvn test -Dtest="TestMasterDaoTest#testUpdateResultWithTransactionSuccess"

# 生成覆盖率报告（需要 JaCoCo 插件）
mvn test jacoco:report
# 报告位置: target/site/jacoco/index.html
```

---

## 附录：文件清单

```
src/main/java/com/iso11820/dao/
├── DaoException.java              # 自定义异常
├── BaseDao.java                   # 泛型CRUD基类（反射映射+缓存）
├── OperatorDao.java               # 操作员接口
├── ApparatusDao.java              # 设备接口
├── ProductMasterDao.java          # 样品接口
├── SensorsDao.java                # 传感器接口
├── TestMasterDao.java             # 试验记录接口
├── CalibrationRecordsDao.java     # 校准记录接口
├── dto/
│   ├── TestResultDTO.java         # 试验结果DTO
│   ├── SensorDataDTO.java         # 传感器数据DTO
│   └── CalibrationDTO.java        # 校准数据DTO
├── support/
│   └── EntityConverter.java       # 实体↔DTO转换
├── util/
│   ├── DbUtil.java                # 连接管理+事务+版本管理
│   └── DaoChecker.java            # 数据校验
└── impl/
    ├── OperatorDaoImpl.java
    ├── ApparatusDaoImpl.java
    ├── ProductMasterDaoImpl.java
    ├── SensorsDaoImpl.java
    ├── TestMasterDaoImpl.java
    └── CalibrationRecordsDaoImpl.java
```