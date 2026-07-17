package com.chalchitraghar.modules.reporting.export;

import java.util.List;

public record ExportTable(List<String> headers, List<List<Object>> rows) {}
