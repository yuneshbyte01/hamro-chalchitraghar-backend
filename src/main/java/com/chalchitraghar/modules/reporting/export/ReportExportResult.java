package com.chalchitraghar.modules.reporting.export;

public record ReportExportResult(
        byte[] content, String contentType, String fileName, int rowCount) {}
