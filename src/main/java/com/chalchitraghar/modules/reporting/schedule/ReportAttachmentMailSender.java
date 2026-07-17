package com.chalchitraghar.modules.reporting.schedule;

import com.chalchitraghar.modules.reporting.export.ReportExportResult;

public interface ReportAttachmentMailSender {
    void send(String recipient, String subject, String body, ReportExportResult attachment);
}
