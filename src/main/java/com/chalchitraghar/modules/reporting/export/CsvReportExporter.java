package com.chalchitraghar.modules.reporting.export;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class CsvReportExporter implements ReportExporter {
    @Override
    public ReportExportResult export(ExportTable table, String baseFileName) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        line(csv, new ArrayList<>(table.headers()));
        table.rows().forEach(row -> line(csv, row));
        return new ReportExportResult(
                csv.toString().getBytes(StandardCharsets.UTF_8),
                "text/csv;charset=UTF-8",
                baseFileName + ".csv",
                table.rows().size());
    }

    private void line(StringBuilder out, List<?> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) out.append(',');
            String value = values.get(index) == null ? "" : values.get(index).toString();
            value = value.replaceAll("[\\p{Cc}&&[^\\r\\n\\t]]", "");
            if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
            out.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        out.append("\r\n");
    }
}
