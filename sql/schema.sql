-- ============================================================
-- ISO 11820 建筑材料不燃性试验仿真系统 — 数据库建表脚本
-- 数据库引擎：SQLite
-- 版本：1.0
-- 日期：2026-06-29
-- ============================================================

-- ============================================================
-- 一、操作员表（无主键约束）
-- ============================================================
CREATE TABLE IF NOT EXISTS "operators" (
    "userid"    TEXT NOT NULL,   -- 用户ID（1=管理员, 2=试验员）
    "username"  TEXT NOT NULL,   -- 登录用户名（admin / experimenter）
    "pwd"       TEXT NOT NULL,   -- 明文密码
    "usertype"  TEXT NOT NULL    -- 角色：admin / operator
);


-- ============================================================
-- 二、设备表
-- ============================================================
CREATE TABLE IF NOT EXISTS "apparatus" (
    "apparatusid"   INTEGER NOT NULL CONSTRAINT "PK_apparatus" PRIMARY KEY,  -- 设备ID
    "innernumber"   TEXT    NOT NULL,   -- 设备内部编号，如 FURNACE-01
    "apparatusname" TEXT    NOT NULL,   -- 设备名称，如 一号试验炉
    "checkdatef"    date    NOT NULL,   -- 检定有效期开始
    "checkdatet"    date    NOT NULL,   -- 检定有效期结束
    "pidport"       TEXT    NOT NULL,   -- PID串口，如 COM9
    "powerport"     TEXT    NOT NULL,   -- 功率串口，如 COM9
    "constpower"    INTEGER NULL        -- 上次记录的恒功率值（可空）
);


-- ============================================================
-- 三、样品表
-- ============================================================
CREATE TABLE IF NOT EXISTS "productmaster" (
    "productid"   TEXT  NOT NULL CONSTRAINT "PK_productmaster" PRIMARY KEY,  -- 样品编号
    "productname" TEXT  NOT NULL,   -- 样品名称，如 岩棉隔热板
    "specific"    TEXT  NOT NULL,   -- 规格型号，如 100×50×25mm
    "diameter"    REAL  NOT NULL,   -- 直径（mm）
    "height"      REAL  NOT NULL,   -- 高度（mm）
    "flag"        TEXT  NULL        -- 备用字段
);


-- ============================================================
-- 四、试验记录表（核心表）⭐
-- ============================================================
CREATE TABLE IF NOT EXISTS "testmaster" (

    -- ===== 基本信息 =====
    "productid"        TEXT    NOT NULL,   -- 样品编号（联合主键 + 外键）
    "testid"           TEXT    NOT NULL,   -- 试验ID，格式 yyyyMMdd-HHmmss
    "testdate"         date    NOT NULL,   -- 试验日期
    "ambtemp"          REAL    NOT NULL,   -- 环境温度（°C）
    "ambhumi"          REAL    NOT NULL,   -- 环境湿度（%）
    "according"        TEXT    NOT NULL,   -- 试验依据，如 ISO 11820:2022
    "operator"         TEXT    NOT NULL,   -- 操作员用户名
    "apparatusid"      TEXT    NOT NULL,   -- 设备编号
    "apparatusname"    TEXT    NOT NULL,   -- 设备名称（冗余，省去关联查询）
    "apparatuschkdate" date    NOT NULL,   -- 设备检定日期
    "rptno"            TEXT    NOT NULL,   -- 报告编号

    -- ===== 质量数据 =====
    "preweight"        REAL    NOT NULL,   -- 试验前质量（g）
    "postweight"       REAL    NOT NULL,   -- 试验后质量（g）
    "lostweight"       REAL    NOT NULL,   -- 失重量 = preweight - postweight
    "lostweight_per"   REAL    NOT NULL,   -- 失重率（%），判定项

    -- ===== 试验过程 =====
    "totaltesttime"    INTEGER NOT NULL,   -- 总试验时长（秒）
    "constpower"       INTEGER NOT NULL,   -- 恒功率值（0~25600）
    "phenocode"        TEXT    NOT NULL,   -- 现象编码（勾选项序列化字符串）
    "flametime"        INTEGER NOT NULL,   -- 火焰开始时刻（秒，无火焰填0）
    "flameduration"    INTEGER NOT NULL,   -- 火焰持续时间（秒，无火焰填0）

    -- ===== 各通道温度最大值 =====
    "maxtf1"           REAL    NOT NULL,   -- 炉温1最大值（°C）
    "maxtf2"           REAL    NOT NULL,   -- 炉温2最大值
    "maxts"            REAL    NOT NULL,   -- 表面温最大值
    "maxtc"            REAL    NOT NULL,   -- 中心温最大值
    "maxtf1_time"      INTEGER NOT NULL,   -- 炉温1最大值时刻（秒）
    "maxtf2_time"      INTEGER NOT NULL,
    "maxts_time"       INTEGER NOT NULL,
    "maxtc_time"       INTEGER NOT NULL,

    -- ===== 各通道温度最终值（试验结束时刻）=====
    "finaltf1"         REAL    NOT NULL,
    "finaltf2"         REAL    NOT NULL,
    "finalts"          REAL    NOT NULL,
    "finaltc"          REAL    NOT NULL,
    "finaltf1_time"    INTEGER NOT NULL,
    "finaltf2_time"    INTEGER NOT NULL,
    "finalts_time"     INTEGER NOT NULL,
    "finaltc_time"     INTEGER NOT NULL,

    -- ===== 温升（结束值 - 开始值）=====
    "deltatf1"         REAL    NOT NULL,   -- 炉温1温升
    "deltatf2"         REAL    NOT NULL,   -- 炉温2温升
    "deltatf"          REAL    NOT NULL,   -- 综合温升（°C），判定项，取表面温升
    "deltats"          REAL    NOT NULL,   -- 表面温温升
    "deltatc"          REAL    NOT NULL,   -- 中心温温升

    -- ===== 备注 =====
    "memo"             TEXT    NULL,       -- 备注（可空）
    "flag"             TEXT    NULL,       -- 完成标记（"10000000"=已保存）

    CONSTRAINT "PK_testmaster" PRIMARY KEY ("productid", "testid"),
    CONSTRAINT "FK_testmaster_productmaster" FOREIGN KEY ("productid")
        REFERENCES "productmaster" ("productid")
);

-- 试验记录索引
CREATE INDEX IF NOT EXISTS "IX_Testmaster_Testdate"
    ON "testmaster" ("testdate");
CREATE INDEX IF NOT EXISTS "IX_Testmaster_Operator"
    ON "testmaster" ("operator");
CREATE INDEX IF NOT EXISTS "IX_Testmaster_Testdate_Productid"
    ON "testmaster" ("testdate", "productid");


-- ============================================================
-- 五、传感器配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS "sensors" (
    "sensorid"    INTEGER NOT NULL CONSTRAINT "PK_sensors" PRIMARY KEY,  -- 传感器ID
    "sensorname"  TEXT    NOT NULL,   -- 传感器代号，如 TF1
    "dispname"    TEXT    NOT NULL,   -- 显示名，如 炉内温度1
    "sensorgroup" TEXT    NOT NULL,   -- 分组标识
    "unit"        TEXT    NOT NULL,   -- 单位，如 ℃
    "discription" TEXT    NOT NULL,   -- 描述
    "flag"        TEXT    NOT NULL,   -- 标记（启用/禁用）
    "signalzero"  REAL    NOT NULL,   -- 信号零点
    "signalspan"  REAL    NOT NULL,   -- 信号量程
    "outputzero"  REAL    NOT NULL,   -- 输出温度下限（如 0）
    "outputspan"  REAL    NOT NULL,   -- 输出温度上限（如 1000）
    "outputvalue" REAL    NOT NULL,   -- 当前温度值（运行时更新）
    "inputvalue"  REAL    NOT NULL,   -- 当前输入值（运行时更新）
    "signaltype"  INTEGER NOT NULL    -- 信号类型：4=数字量（仿真用）
);


-- ============================================================
-- 六、校准记录表（表名首字母大写）
-- ============================================================
CREATE TABLE IF NOT EXISTS "CalibrationRecords" (
    "Id"                 TEXT    NOT NULL CONSTRAINT "PK_CalibrationRecords" PRIMARY KEY,  -- GUID
    "CalibrationDate"    TEXT    NOT NULL,   -- 校准日期时间（ISO 8601字符串）
    "CalibrationType"    TEXT    NOT NULL,   -- 类型：Surface 或 Center
    "ApparatusId"        INTEGER NOT NULL,   -- 设备ID
    "Operator"           TEXT    NOT NULL,   -- 操作员
    "TemperatureData"    TEXT    NOT NULL,   -- JSON字符串，温度数据集合
    "UniformityResult"   REAL    NULL,       -- 均匀性结果
    "MaxDeviation"       REAL    NULL,       -- 最大偏差
    "AverageTemperature" REAL    NULL,       -- 平均温度
    "PassedCriteria"     INTEGER NOT NULL,   -- 是否通过：0=未通过，1=通过
    "Remarks"            TEXT    NOT NULL,   -- 备注
    "CreatedAt"          TEXT    NOT NULL,   -- 创建时间

    -- 炉壁9测温点（A/B/C层 × 1/2/3轴）
    "TempA1" REAL NULL, "TempA2" REAL NULL, "TempA3" REAL NULL,
    "TempB1" REAL NULL, "TempB2" REAL NULL, "TempB3" REAL NULL,
    "TempC1" REAL NULL, "TempC2" REAL NULL, "TempC3" REAL NULL,

    -- 计算结果
    "TAvg"        REAL NULL,   -- 总均温
    "TAvgAxis1"   REAL NULL, "TAvgAxis2" REAL NULL, "TAvgAxis3" REAL NULL,
    "TAvgLevela"  REAL NULL, "TAvgLevelb" REAL NULL, "TAvgLevelc" REAL NULL,
    "TDevAxis1"   REAL NULL, "TDevAxis2" REAL NULL, "TDevAxis3" REAL NULL,
    "TDevLevela"  REAL NULL, "TDevLevelb" REAL NULL, "TDevLevelc" REAL NULL,
    "TAvgDevAxis" REAL NULL,   -- 轴向平均偏差
    "TAvgDevLevel" REAL NULL,  -- 层向平均偏差

    "CenterTempData" TEXT NULL,   -- 中心轴JSON数据（可空）
    "Memo"           TEXT NULL    -- 备注（可空）
);

-- 校准记录索引
CREATE INDEX IF NOT EXISTS "IX_CalibrationRecord_Date"
    ON "CalibrationRecords" ("CalibrationDate");
CREATE INDEX IF NOT EXISTS "IX_CalibrationRecord_Operator"
    ON "CalibrationRecords" ("Operator");


-- ============================================================
-- 七、初始数据
-- ============================================================

-- 7.1 操作员（2个默认账号）
INSERT INTO operators (userid, username, pwd, usertype)
SELECT '1', 'admin', '123456', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM operators WHERE username = 'admin');

INSERT INTO operators (userid, username, pwd, usertype)
SELECT '2', 'experimenter', '123456', 'operator'
WHERE NOT EXISTS (SELECT 1 FROM operators WHERE username = 'experimenter');

-- 7.2 设备（1台默认设备）
INSERT INTO apparatus (apparatusid, innernumber, apparatusname, checkdatef, checkdatet, pidport, powerport, constpower)
SELECT 0, 'FURNACE-01', '一号试验炉', date('now'), date('now', '+1 year'), 'COM9', 'COM9', 2048
WHERE NOT EXISTS (SELECT 1 FROM apparatus WHERE apparatusid = 0);

-- 7.3 传感器（5个核心业务通道 + 12个备用通道）
-- 核心通道
INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 0, 'Sensor0', '炉温1', '采集', '℃', '炉温1', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 0);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 1, 'Sensor1', '炉温2', '采集', '℃', '炉温2', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 1);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 2, 'Sensor2', '表面温度', '采集', '℃', '表面温度', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 2);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 3, 'Sensor3', '中心温度', '采集', '℃', '中心温度', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 3);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 16, 'Sensor16', '校准温度', '校准', '℃', '校准温度', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 16);

-- 备用通道（4~15）
INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 4, 'Sensor4', '备用通道5', '备用', '℃', '备用通道5', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 4);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 5, 'Sensor5', '备用通道6', '备用', '℃', '备用通道6', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 5);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 6, 'Sensor6', '备用通道7', '备用', '℃', '备用通道7', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 6);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 7, 'Sensor7', '备用通道8', '备用', '℃', '备用通道8', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 7);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 8, 'Sensor8', '备用通道9', '备用', '℃', '备用通道9', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 8);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 9, 'Sensor9', '备用通道10', '备用', '℃', '备用通道10', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 9);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 10, 'Sensor10', '备用通道11', '备用', '℃', '备用通道11', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 10);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 11, 'Sensor11', '备用通道12', '备用', '℃', '备用通道12', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 11);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 12, 'Sensor12', '备用通道13', '备用', '℃', '备用通道13', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 12);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 13, 'Sensor13', '备用通道14', '备用', '℃', '备用通道14', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 13);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 14, 'Sensor14', '备用通道15', '备用', '℃', '备用通道15', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 14);

INSERT INTO sensors (sensorid, sensorname, dispname, sensorgroup, unit, discription, flag, signalzero, signalspan, outputzero, outputspan, outputvalue, inputvalue, signaltype)
SELECT 15, 'Sensor15', '备用通道16', '备用', '℃', '备用通道16', '启用', 0, 0, 0, 1000, 0, 0, 4
WHERE NOT EXISTS (SELECT 1 FROM sensors WHERE sensorid = 15);


-- ============================================================
-- 八、系统配置表（数据库版本管理）
-- ============================================================
CREATE TABLE IF NOT EXISTS "system_config" (
    "config_key"   TEXT NOT NULL CONSTRAINT "PK_system_config" PRIMARY KEY,  -- 配置键
    "config_value" TEXT NOT NULL                                              -- 配置值
);

-- 初始配置：数据库版本号
INSERT INTO system_config (config_key, config_value)
SELECT 'db_version', '1.0'
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'db_version');