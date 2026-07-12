package com.chalchitraghar.modules.tickets.service; public interface TicketDeliveryService { void queueAndAttempt(Long bookingId); void attempt(Long deliveryId); int retryBatch(); }
