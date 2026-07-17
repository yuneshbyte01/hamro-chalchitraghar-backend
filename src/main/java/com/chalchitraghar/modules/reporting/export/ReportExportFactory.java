package com.chalchitraghar.modules.reporting.export;

import com.chalchitraghar.modules.reporting.enums.ReportExportFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportExportFactory {
    private final CsvReportExporter csv;
    private final XlsxReportExporter xlsx;

    public ReportExporter get(ReportExportFormat format) {
        return format == ReportExportFormat.CSV ? csv : xlsx;
    }
}
