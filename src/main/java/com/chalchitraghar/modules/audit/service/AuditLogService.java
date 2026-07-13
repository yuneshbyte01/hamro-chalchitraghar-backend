package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.dto.request.*;
import com.chalchitraghar.modules.audit.dto.response.*;
import com.chalchitraghar.shared.response.PageResponse;

public interface AuditLogService {
    AdminAuditLogDetailResponse append(CreateAuditLogCommand command);

    PageResponse<AdminAuditLogSummaryResponse> findAll(
            AdminAuditLogFilterRequest filters, int page, int size);

    AdminAuditLogDetailResponse findById(Long id);
}
