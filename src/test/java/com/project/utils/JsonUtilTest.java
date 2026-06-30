package com.project.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonUtil 单元测试 —— JSON 序列化/反序列化工具类全覆盖。
 * <p>
 * 覆盖：toJson/toObject/toList/toMap/文件读写/安全方法/isValidJson/边界条件。
 * </p>
 *
 * @author ISO11820 Development Team
 * @version 1.0
 * @since 2026-06-30
 */
@DisplayName("JsonUtil JSON 工具类测试")
class JsonUtilTest {

    // ==================== 序列化 ====================

    @Nested
    @DisplayName("toJson — 对象 → JSON")
    class ToJsonTests {

        @Test
        @DisplayName("简单对象序列化")
        void simpleObject() {
            TestBean bean = new TestBean("test", 42);
            String json = JsonUtil.toJson(bean);
            assertTrue(json.contains("\"name\":\"test\""));
            assertTrue(json.contains("\"value\":42"));
        }

        @Test
        @DisplayName("null 对象抛异常")
        void nullObjectThrows() {
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toJson(null));
        }

        @Test
        @DisplayName("toPrettyJson 美化输出")
        void prettyJson() {
            TestBean bean = new TestBean("test", 42);
            String pretty = JsonUtil.toPrettyJson(bean);
            assertTrue(pretty.contains("\n"));
            assertTrue(pretty.contains("  "));
        }

        @Test
        @DisplayName("toPrettyJson(null) 抛异常")
        void prettyJsonNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toPrettyJson(null));
        }
    }

    // ==================== 反序列化 ====================

    @Nested
    @DisplayName("toObject — JSON → 对象")
    class ToObjectTests {

        @Test
        @DisplayName("JSON 字符串反序列化")
        void jsonToObject() {
            String json = "{\"name\":\"hello\",\"value\":99}";
            TestBean bean = JsonUtil.toObject(json, TestBean.class);
            assertEquals("hello", bean.getName());
            assertEquals(99, bean.getValue());
        }

        @Test
        @DisplayName("JSON 为空抛异常")
        void nullJsonThrows() {
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toObject(null, TestBean.class));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toObject("", TestBean.class));
        }

        @Test
        @DisplayName("目标类型为 null 抛异常")
        void nullClassThrows() {
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toObject("{}", null));
        }

        @Test
        @DisplayName("格式错误 JSON 抛异常")
        void malformedJsonThrows() {
            assertThrows(RuntimeException.class, () -> JsonUtil.toObject("{bad json}", TestBean.class));
        }
    }

    @Nested
    @DisplayName("toObjectOrNull — 安全反序列化")
    class ToObjectOrNullTests {

        @Test
        @DisplayName("正常反序列化")
        void normalDeserialization() {
            TestBean bean = JsonUtil.toObjectOrNull("{\"name\":\"ok\",\"value\":1}", TestBean.class);
            assertNotNull(bean);
            assertEquals("ok", bean.getName());
        }

        @Test
        @DisplayName("格式错误返回 null")
        void malformedReturnsNull() {
            assertNull(JsonUtil.toObjectOrNull("bad json", TestBean.class));
        }
    }

    @Nested
    @DisplayName("toList — JSON 数组 → List")
    class ToListTests {

        @Test
        @DisplayName("JSON 数组反序列化")
        void jsonArrayToList() {
            String json = "[{\"name\":\"A\",\"value\":1},{\"name\":\"B\",\"value\":2}]";
            List<TestBean> list = JsonUtil.toList(json, TestBean.class);
            assertEquals(2, list.size());
            assertEquals("A", list.get(0).getName());
            assertEquals("B", list.get(1).getName());
        }

        @Test
        @DisplayName("空 JSON 数组")
        void emptyArray() {
            List<TestBean> list = JsonUtil.toList("[]", TestBean.class);
            assertTrue(list.isEmpty());
        }

        @Test
        @DisplayName("null 参数抛异常")
        void nullParamsThrow() {
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toList(null, TestBean.class));
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toList("[]", null));
        }
    }

    @Nested
    @DisplayName("toMap — JSON 对象 → Map")
    class ToMapTests {

        @Test
        @DisplayName("JSON 对象转 Map")
        void jsonToMap() {
            Map<String, Object> map = JsonUtil.toMap("{\"key\":\"value\",\"num\":42}");
            assertEquals("value", map.get("key"));
            assertEquals(42, map.get("num"));
        }

        @Test
        @DisplayName("null 参数抛异常")
        void nullParamThrows() {
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.toMap(null));
        }
    }

    // ==================== 文件操作 ====================

    @Nested
    @DisplayName("文件操作")
    class FileOperations {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("toJsonFile 写入 JSON 文件")
        void writeJsonFile() {
            TestBean bean = new TestBean("file", 100);
            Path filePath = tempDir.resolve("test.json");

            JsonUtil.toJsonFile(bean, filePath);
            assertTrue(filePath.toFile().exists());
            assertTrue(filePath.toFile().length() > 0);
        }

        @Test
        @DisplayName("fromJsonFile 读取 JSON 文件")
        void readJsonFile() {
            TestBean original = new TestBean("file", 100);
            Path filePath = tempDir.resolve("read.json");
            JsonUtil.toJsonFile(original, filePath);

            TestBean loaded = JsonUtil.fromJsonFile(filePath, TestBean.class);
            assertEquals("file", loaded.getName());
            assertEquals(100, loaded.getValue());
        }

        @Test
        @DisplayName("fromJsonFile 文件不存在抛异常")
        void fileNotFoundThrows() {
            assertThrows(RuntimeException.class,
                    () -> JsonUtil.fromJsonFile(tempDir.resolve("nonexistent.json"), TestBean.class));
        }

        @Test
        @DisplayName("toJsonFile(null) 抛异常")
        void writeNullObjectThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> JsonUtil.toJsonFile(null, tempDir.resolve("test.json")));
        }
    }

    // ==================== JSON 校验 ====================

    @Nested
    @DisplayName("isValidJson")
    class IsValidJsonTests {

        @Test
        @DisplayName("合法 JSON 返回 true")
        void validJson() {
            assertTrue(JsonUtil.isValidJson("{\"key\":\"value\"}"));
            assertTrue(JsonUtil.isValidJson("[1,2,3]"));
            assertTrue(JsonUtil.isValidJson("\"string\""));
            assertTrue(JsonUtil.isValidJson("123"));
        }

        @Test
        @DisplayName("非法 JSON 返回 false")
        void invalidJson() {
            assertFalse(JsonUtil.isValidJson("{bad}"));
            assertFalse(JsonUtil.isValidJson(""));
            assertFalse(JsonUtil.isValidJson(null));
        }
    }

    @Nested
    @DisplayName("parseTree")
    class ParseTreeTests {

        @Test
        @DisplayName("解析 JSON 为节点树")
        void parseTree() {
            var node = JsonUtil.parseTree("{\"name\":\"test\"}");
            assertEquals("test", node.get("name").asText());
        }

        @Test
        @DisplayName("null 参数抛异常")
        void nullThrows() {
            assertThrows(IllegalArgumentException.class, () -> JsonUtil.parseTree(null));
        }
    }

    // ==================== 边界条件 ====================

    @Nested
    @DisplayName("边界条件")
    class BoundaryConditions {

        @Test
        @DisplayName("getMapper 返回非 null 的单例")
        void getMapperReturnsMapper() {
            assertNotNull(JsonUtil.getMapper());
            // 两次调用返回同一实例
            assertSame(JsonUtil.getMapper(), JsonUtil.getMapper());
        }

        @Test
        @DisplayName("TObject with TypeReference")
        void toObjectWithTypeReference() {
            String json = "{\"name\":\"ref\",\"value\":88}";
            TestBean bean = JsonUtil.toObject(json, new com.fasterxml.jackson.core.type.TypeReference<TestBean>() {});
            assertEquals("ref", bean.getName());
        }

        @Test
        @DisplayName("工具类不允许实例化")
        void cannotInstantiate() {
            assertThrows(UnsupportedOperationException.class, JsonUtil::new);
        }

        @Test
        @DisplayName("未知字段忽略（不抛异常）")
        void unknownFieldsIgnored() {
            String json = "{\"name\":\"ok\",\"value\":1,\"extraField\":\"shouldBeIgnored\"}";
            TestBean bean = JsonUtil.toObject(json, TestBean.class);
            assertEquals("ok", bean.getName());
        }
    }

    // ==================== 测试 Bean ====================

    public static class TestBean {
        private String name;
        private int value;

        public TestBean() {}

        public TestBean(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}