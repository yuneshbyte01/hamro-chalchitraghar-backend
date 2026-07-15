package com.chalchitraghar.modules.payments.dto.response;

public record TicketStatusSummary(long issued, long checkedIn, long revoked, long expired) {}
