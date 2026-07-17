package com.chalchitraghar.modules.reporting.export;

public interface ReportExporter {
    ReportExportResult export(ExportTable table, String baseFileName);
}
