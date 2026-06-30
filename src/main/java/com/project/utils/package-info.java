/**
 * <h1>com.project.utils — 工具层</h1>
 *
 * <p>本包提供全局通用的工具类，所有类均为线程安全设计，遵循以下规范：</p>
 * <ul>
 *   <li>构造器私有化，禁止实例化</li>
 *   <li>单例类使用双重检查锁定（DCL）</li>
 *   <li>所有公共方法入参做 null/blank 校验</li>
 *   <li>异常统一捕获并记录日志，不抛裸异常</li>
 *   <li>不依赖核心业务层、DAO 层、UI 层</li>
 * </ul>
 *
 * <h2>包含类</h2>
 * <table>
 *   <tr><th>类名</th><th>职责</th></tr>
 *   <tr><td>{@link com.project.utils.AppConfig}</td><td>应用配置管理单例（读取 appsettings.json）</td></tr>
 *   <tr><td>{@link com.project.utils.LogUtil}</td><td>日志工具门面（封装 SLF4J/Logback）</td></tr>
 *   <tr><td>{@link com.project.utils.DateUtil}</td><td>日期时间工具（格式化、解析、时间戳、时间差）</td></tr>
 *   <tr><td>{@link com.project.utils.NumUtil}</td><td>数值工具（精度控制、空值转换、数字校验）</td></tr>
 *   <tr><td>{@link com.project.utils.JsonUtil}</td><td>JSON 工具（序列化/反序列化、文件读写）</td></tr>
 *   <tr><td>{@link com.project.utils.FilePathManageUtil}</td><td>文件路径统一管理（跨平台路径拼接、目录创建）</td></tr>
 * </table>
 *
 * @author Project Team - Utility Layer
 * @version 1.1
 * @since 2026-06-30
 */
package com.project.utils;