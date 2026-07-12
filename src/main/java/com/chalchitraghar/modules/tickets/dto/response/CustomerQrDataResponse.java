package com.chalchitraghar.modules.tickets.dto.response;
import java.time.LocalDateTime;
public record CustomerQrDataResponse(String ticketReference,Integer qrVersion,LocalDateTime issuedAt) {}
