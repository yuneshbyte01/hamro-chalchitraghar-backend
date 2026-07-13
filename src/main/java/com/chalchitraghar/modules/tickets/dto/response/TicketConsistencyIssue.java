package com.chalchitraghar.modules.tickets.dto.response;

public record TicketConsistencyIssue(
        String code, String ticketReference, String bookingReference, String message) {}
