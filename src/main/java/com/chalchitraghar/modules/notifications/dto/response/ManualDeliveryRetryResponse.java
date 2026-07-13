package com.chalchitraghar.modules.notifications.dto.response;

public record ManualDeliveryRetryResponse(Long deliveryId, boolean queued, String status) {}
