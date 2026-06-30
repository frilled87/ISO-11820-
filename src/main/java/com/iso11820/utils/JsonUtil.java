package com.iso11820.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * <h1>JSON 工具类 — 纯静态方法，线程安全</h1>
 *
 * <p>封装 Jackson {@link ObjectMapper}，提供 JSON 字符串 ↔ 实体/集合的高效转换。
 * 内部 ObjectMapper 为单例，所有方法线程安全，异常均已友好捕获。</p>
 *
 * <h2>核心能力</h2>
 * <table>
 *   <tr><th>分类</th><th>方法</th><th>说明</th></tr>
 *   <tr><td>实体→JSON</td><td>{@link #toJson(Object)}</td><td>对象序列化为 JSON 字符串</td></tr>
 *   <tr><td>JSON→实体</td><td>{@link #toObject(String, Class)}</td><td>JSON 字符串反序列化为对象</td></tr>
 *   <tr><td>JSON→List</td><td>{@link #toList(String, Class)}</td><td>JSON 数组反序列化为 List</td></tr>
 *   <tr><td>JSON→Map</td><td>{@link #toMap(String)}</td><td>JSON 对象反序列化为 Map</td></tr>
 *   <tr><td>文件操作</td><td>{@link #toJsonFile(Object, Path)}</td><td>对象序列化写入文件</td></tr>
 *   <tr><td>文件操作</td><td>{@link #fromJsonFile(Path, Class)}</td><td>从文件读取反序列化</td></tr>
 *   <tr><td>安全读取</td><td>{@link #toObjectOrNull(String, Class)}</td><td>反序列化失败返回 null</td></tr>
 * </table>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 *   // 实体 → JSON
 *   String json = JsonUtil.toJson(sensorData);
 *
 *   // JSON → 实体
 *   SensorData data = JsonUtil.toObject(json, SensorData.class);
 *
 *   // JSON → 实体（失败返回 null）
 *   SensorData data = JsonUtil.toObjectOrNull(json, SensorData.class);
 *
 *   // JSON 数组 → List
 *   List<SensorData> list = JsonUtil.toList(jsonArray, SensorData.class);
 *
 *   // 写入文件
 *   JsonUtil.toJsonFile(data, Path.of("./data.json"));
 *
 *   // 从文件读取
 *   SensorData data = JsonUtil.fromJsonFile(Path.of("./data.json"), SensorData.class);
 * }</pre>
 *
 * @author Project Team - Utility Layer
 * @version 1.0
 * @since 2026-06-30
 */
public final class JsonUtil {

    // ============================================================
    //  单例 ObjectMapper（线程安全，延迟初始化）
    // ============================================================

    /** 内部持有类，利用 JVM 类加载机制保证线程安全的延迟初始化 */
    private static final class MapperHolder {
        static final ObjectMapper INSTANCE = createMapper();
    }

    /**
     * 创建并配置 ObjectMapper。
     * <ul>
     *   <li>忽略未知字段（向前兼容）</li>
     *   <li>禁用日期序列化为时间戳</li>
     *   <li>注册 Java 8 时间模块（LocalDateTime 等）</li>
     * </ul>
     */
    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 忽略 JSON 中存在但实体中没有的字段，避免 Deserialization 异常
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 日期序列化为 ISO 字符串而非时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 注册 Java 8 时间模块
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * 获取全局共享的 ObjectMapper 实例。
     *
     * @return 配置好的 ObjectMapper
     */
    public static ObjectMapper getMapper() {
        return MapperHolder.INSTANCE;
    }

    JsonUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ============================================================
    //  序列化（对象 → JSON）
    // ============================================================

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param object 待序列化对象，不能为 null
     * @return JSON 字符串
     * @throws IllegalArgumentException object 为 null
     * @throws RuntimeException 序列化失败时抛出（含原始异常信息）
     */
    public static String toJson(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("序列化对象不能为 null");
        }
        try {
            return MapperHolder.INSTANCE.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败: " + object.getClass().getSimpleName(), e);
        }
    }

    /**
     * 将对象序列化为格式化的 JSON 字符串（美化输出）。
     *
     * @param object 待序列化对象，不能为 null
     * @return 格式化的 JSON 字符串
     * @throws IllegalArgumentException object 为 null
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("序列化对象不能为 null");
        }
        try {
            return MapperHolder.INSTANCE.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败: " + object.getClass().getSimpleName(), e);
        }
    }

    // ============================================================
    //  反序列化（JSON → 对象）
    // ============================================================

    /**
     * 将 JSON 字符串反序列化为指定类型对象。
     *
     * @param json  JSON 字符串，不能为 null/空白
     * @param clazz 目标类型，不能为 null
     * @param <T>   泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException json 或 clazz 为 null/空白
     * @throws RuntimeException JSON 格式错误或类型不匹配时抛出
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON 字符串不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("目标类型不能为 null");
        }
        try {
            return MapperHolder.INSTANCE.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败，目标类型: " + clazz.getSimpleName(), e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型对象，失败返回 null。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 反序列化后的对象，或 null
     */
    public static <T> T toObjectOrNull(String json, Class<T> clazz) {
        try {
            return toObject(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将 JSON 字符串反序列化为泛型类型（如 {@code List<SensorData>}）。
     *
     * <pre>{@code
     * List<SensorData> list = JsonUtil.toObject(json, new TypeReference<List<SensorData>>() {});
     * }</pre>
     *
     * @param json          JSON 字符串，不能为 null/空白
     * @param typeReference 泛型类型引用，不能为 null
     * @param <T>           泛型类型
     * @return 反序列化后的对象
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON 字符串不能为空");
        }
        if (typeReference == null) {
            throw new IllegalArgumentException("typeReference 不能为 null");
        }
        try {
            return MapperHolder.INSTANCE.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化失败", e);
        }
    }

    // ============================================================
    //  集合反序列化
    // ============================================================

    /**
     * 将 JSON 数组字符串反序列化为 List。
     *
     * <pre>{@code
     * String json = "[{\"name\":\"A\"},{\"name\":\"B\"}]";
     * List<MyEntity> list = JsonUtil.toList(json, MyEntity.class);
     * }</pre>
     *
     * @param json  JSON 数组字符串
     * @param clazz 元素类型
     * @param <T>   泛型类型
     * @return List 集合
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON 字符串不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("元素类型不能为 null");
        }
        try {
            return MapperHolder.INSTANCE.readValue(json,
                    MapperHolder.INSTANCE.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化为 List 失败，元素类型: " + clazz.getSimpleName(), e);
        }
    }

    /**
     * 将 JSON 对象字符串反序列化为 Map。
     *
     * <pre>{@code
     * Map<String, Object> map = JsonUtil.toMap("{\"key\":\"value\"}");
     * </pre>
     *
     * @param json JSON 对象字符串
     * @return Map&lt;String, Object&gt;
     */
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON 字符串不能为空");
        }
        try {
            return MapperHolder.INSTANCE.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 反序列化为 Map 失败", e);
        }
    }

    // ============================================================
    //  文件操作
    // ============================================================

    /**
     * 将对象序列化并写入 JSON 文件。
     * 自动创建父目录，使用 UTF-8 编码。
     *
     * @param object   待写入对象，不能为 null
     * @param filePath 文件路径，不能为 null
     * @throws IllegalArgumentException 参数为 null
     * @throws RuntimeException 写入失败时抛出
     */
    public static void toJsonFile(Object object, Path filePath) {
        if (object == null) {
            throw new IllegalArgumentException("序列化对象不能为 null");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("文件路径不能为 null");
        }
        try {
            // 确保父目录存在
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MapperHolder.INSTANCE.writerWithDefaultPrettyPrinter()
                    .writeValue(filePath.toFile(), object);
        } catch (IOException e) {
            throw new RuntimeException("JSON 文件写入失败: " + filePath, e);
        }
    }

    /**
     * 从 JSON 文件读取并反序列化为对象。
     *
     * @param filePath 文件路径，不能为 null
     * @param clazz    目标类型，不能为 null
     * @param <T>      泛型类型
     * @return 反序列化后的对象
     * @throws IllegalArgumentException 参数为 null
     * @throws RuntimeException 文件不存在或反序列化失败时抛出
     */
    public static <T> T fromJsonFile(Path filePath, Class<T> clazz) {
        if (filePath == null) {
            throw new IllegalArgumentException("文件路径不能为 null");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("目标类型不能为 null");
        }
        if (!Files.exists(filePath)) {
            throw new RuntimeException("JSON 文件不存在: " + filePath);
        }
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return toObject(content, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON 文件读取失败: " + filePath, e);
        }
    }

    /**
     * 从 classpath 资源读取 JSON 并反序列化。
     *
     * @param resourcePath classpath 资源路径，如 {@code "appsettings.json"}
     * @param clazz        目标类型
     * @param <T>          泛型类型
     * @return 反序列化后的对象
     */
    public static <T> T fromResource(String resourcePath, Class<T> clazz) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("资源路径不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("目标类型不能为 null");
        }
        try (InputStream is = JsonUtil.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("classpath 资源不存在: " + resourcePath);
            }
            return MapperHolder.INSTANCE.readValue(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException("从 classpath 资源读取 JSON 失败: " + resourcePath, e);
        }
    }

    // ============================================================
    //  JSON 节点操作
    // ============================================================

    /**
     * 将 JSON 字符串解析为 JsonNode 树。
     *
     * @param json JSON 字符串
     * @return JsonNode 根节点
     */
    public static JsonNode parseTree(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON 字符串不能为空");
        }
        try {
            return MapperHolder.INSTANCE.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 解析为节点树失败", e);
        }
    }

    /**
     * 判断字符串是否为合法 JSON。
     *
     * @param str 待校验字符串
     * @return true 表示合法 JSON
     */
    public static boolean isValidJson(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        try {
            MapperHolder.INSTANCE.readTree(str);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}