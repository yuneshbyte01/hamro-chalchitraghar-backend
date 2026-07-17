package com.chalchitraghar.modules.reporting.export;

import java.io.*;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class XlsxReportExporter implements ReportExporter {
    @Override
    public ReportExportResult export(ExportTable table, String baseFileName) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet("Report");
            sheet.createFreezePane(0, 1);
            Row header = sheet.createRow(0);
            for (int i = 0; i < table.headers().size(); i++)
                header.createCell(i).setCellValue(table.headers().get(i));
            int rowNumber = 1;
            for (var values : table.rows()) {
                Row row = sheet.createRow(rowNumber++);
                for (int i = 0; i < values.size(); i++) write(row.createCell(i), values.get(i));
            }
            workbook.write(output);
            workbook.dispose();
            return new ReportExportResult(
                    output.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    baseFileName + ".xlsx",
                    table.rows().size());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate XLSX report", exception);
        }
    }

    private void write(Cell cell, Object value) {
        if (value == null) return;
        if (value instanceof BigDecimal decimal) cell.setCellValue(decimal.doubleValue());
        else if (value instanceof Number number) cell.setCellValue(number.doubleValue());
        else cell.setCellValue(safe(value.toString()));
    }

    private String safe(String value) {
        String clean = value.replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", "");
        return !clean.isEmpty() && "=+-@".indexOf(clean.charAt(0)) >= 0 ? "'" + clean : clean;
    }
}
