package com.chalchitraghar.modules.tickets.service.impl;

import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.entity.TicketValidation;
import com.chalchitraghar.modules.tickets.enums.ValidationResult;
import com.chalchitraghar.modules.tickets.repository.TicketValidationRepository;
import com.chalchitraghar.modules.tickets.service.TicketValidationQueryService;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketValidationQueryServiceImpl implements TicketValidationQueryService {
    private final TicketValidationRepository repo;

    public PageResponse<TicketValidationHistoryResponse> search(
            int page,
            int size,
            String ref,
            String result,
            Long by,
            Long show,
            LocalDateTime from,
            LocalDateTime to,
            String device,
            String location) {
        if (page < 0 || size < 1 || size > 200)
            throw new IllegalArgumentException("Invalid page or size");
        ValidationResult parsed =
                result == null || result.isBlank()
                        ? null
                        : ValidationResult.valueOf(result.trim().toUpperCase());
        var spec =
                (org.springframework.data.jpa.domain.Specification<TicketValidation>)
                        (r, q, b) -> {
                            var p =
                                    new java.util.ArrayList<
                                            jakarta.persistence.criteria.Predicate>();
                            if (ref != null && !ref.isBlank())
                                p.add(
                                        b.equal(
                                                r.get("ticket").get("ticketReference"),
                                                ref.trim().toUpperCase()));
                            if (parsed != null) p.add(b.equal(r.get("result"), parsed));
                            if (by != null) p.add(b.equal(r.get("validatedBy").get("id"), by));
                            if (show != null)
                                p.add(
                                        b.equal(
                                                r.get("ticket")
                                                        .get("booking")
                                                        .get("show")
                                                        .get("id"),
                                                show));
                            if (from != null)
                                p.add(b.greaterThanOrEqualTo(r.get("validationTime"), from));
                            if (to != null) p.add(b.lessThanOrEqualTo(r.get("validationTime"), to));
                            if (device != null && !device.isBlank())
                                p.add(
                                        b.equal(
                                                b.lower(r.get("deviceId")),
                                                device.trim().toLowerCase()));
                            if (location != null && !location.isBlank())
                                p.add(
                                        b.like(
                                                b.lower(r.get("location")),
                                                "%" + location.trim().toLowerCase() + "%"));
                            return b.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
                        };
        Page<TicketValidation> x =
                repo.findAll(
                        spec,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "validationTime")));
        return PageResponse.from(
                x,
                x.stream()
                        .map(
                                v ->
                                        new TicketValidationHistoryResponse(
                                                v.getResult(),
                                                v.getReason(),
                                                v.getValidationTime(),
                                                v.getValidatedBy().getId(),
                                                v.getValidatedBy().getName(),
                                                v.getDeviceId(),
                                                v.getLocation(),
                                                v.getRequestId()))
                        .toList());
    }
}
