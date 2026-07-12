package com.chalchitraghar.modules.tickets.service;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor public class ExpiredTicketCleanupJob { private final TicketOperationsService service; @Scheduled(fixedDelayString="${app.tickets.expiry-reconciliation-interval-ms:60000}") public int run(){return service.expireBatch();} }
