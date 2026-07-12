package com.chalchitraghar.modules.tickets.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record TicketScanRequest(@NotBlank @Size(max=500) String qrToken) {}
