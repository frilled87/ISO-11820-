/**
 * <h1>com.project.service — 服务层</h1>
 *
 * <p>本包提供文件服务和数据导出服务，所有服务类均为线程安全单例。
 * 不依赖核心业务层、DAO 层、UI 层代码。</p>
 *
 * <h2>包含类</h2>
 * <table>
 *   <tr><th>类名</th><th>职责</th></tr>
 *   <tr><td>{@link com.project.service.ReportExportService}</td><td>⭐ 统一导出门面（一键导出 Excel+PDF）</td></tr>
 *   <tr><td>{@link com.project.service.CsvDataService}</td><td>CSV 温度数据读写服务</td></tr>
 *   <tr><td>{@link com.project.service.ExcelReportService}</td><td>Excel 试验报告导出服务（3 Sheet）</td></tr>
 *   <tr><td>{@link com.project.service.PdfReportService}</td><td>PDF 试验报告导出服务（A4 中文）</td></tr>
 *   <tr><td>{@link com.project.service.entity.DataPoint}</td><td>时序温度数据点实体</td></tr>
 *   <tr><td>{@link com.project.service.model.ExportTestInfo}</td><td>试验报告数据实体（导出用）</td></tr>
 * </table>
 *
 * @author Project Team - Utility Layer
 * @version 3.0
 * @since 2026-06-30
 */
package com.project.service;