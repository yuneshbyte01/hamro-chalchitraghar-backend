package com.chalchitraghar.modules.tickets.service.impl;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.tickets.dto.request.TicketSearchCriteria;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.enums.*;
import com.chalchitraghar.modules.tickets.mapper.TicketMapper;
import com.chalchitraghar.modules.tickets.repository.*;
import com.chalchitraghar.modules.tickets.service.TicketQueryService;
import com.chalchitraghar.modules.tickets.specification.TicketSpecification;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.PageResponse;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryServiceImpl implements TicketQueryService {
    private static final Set<String> SORTS =
            Set.of(
                    "id",
                    "ticketReference",
                    "status",
                    "issuedAt",
                    "checkedInAt",
                    "revokedAt",
                    "expiredAt",
                    "createdAt",
                    "updatedAt");
    private final TicketRepository repo;
    private final TicketValidationRepository validations;
    private final TicketMapper mapper;

    public PageResponse<CustomerTicketSummaryResponse> customer(
            User u, TicketSearchCriteria c, int p, int s, String sort, String dir) {
        return page(c, u.getId(), p, s, sort, dir, mapper::toCustomerSummary);
    }

    public PageResponse<StaffTicketSummaryResponse> staff(
            TicketSearchCriteria c, int p, int s, String sort, String dir) {
        return page(c, null, p, s, sort, dir, mapper::toStaffSummary);
    }

    public PageResponse<AdminTicketSummaryResponse> admin(
            TicketSearchCriteria c, int p, int s, String sort, String dir) {
        return page(c, null, p, s, sort, dir, mapper::toAdminSummary);
    }

    private <T> PageResponse<T> page(
            TicketSearchCriteria c,
            Long owner,
            int p,
            int s,
            String sort,
            String dir,
            java.util.function.Function<Ticket, T> fn) {
        if (p < 0 || s < 1 || s > 200) throw new IllegalArgumentException("Invalid page or size");
        if (!SORTS.contains(sort)) throw new IllegalArgumentException("Invalid sortBy");
        if (!dir.equalsIgnoreCase("asc") && !dir.equalsIgnoreCase("desc"))
            throw new IllegalArgumentException("Invalid sortDir");
        Page<Ticket> x =
                repo.findAll(
                        TicketSpecification.search(c, owner),
                        PageRequest.of(p, s, Sort.by(Sort.Direction.fromString(dir), sort)));
        return PageResponse.from(x, x.stream().map(fn).toList());
    }

    public TicketMetricsResponse metrics(TicketSearchCriteria c) {
        var list = repo.findAll(TicketSpecification.search(c, null));
        long issued = list.size(),
                checked =
                        list.stream().filter(t -> t.getStatus() == TicketStatus.CHECKED_IN).count(),
                rev = list.stream().filter(t -> t.getStatus() == TicketStatus.REVOKED).count(),
                exp = list.stream().filter(t -> t.getStatus() == TicketStatus.EXPIRED).count(),
                unique = list.stream().map(t -> t.getBooking().getId()).distinct().count();
        long early = validations.countByResult(ValidationResult.TOO_EARLY),
                used = validations.countByResult(ValidationResult.ALREADY_USED),
                fail = validations.countByResultNot(ValidationResult.SUCCESS);
        return new TicketMetricsResponse(
                issued,
                checked,
                rev,
                exp,
                list.isEmpty() ? 0 : 100.0 * checked / list.size(),
                unique,
                early,
                used,
                fail);
    }

    public Map<String, Long> validationSummary() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (var r : ValidationResult.values()) m.put(r.name(), validations.countByResult(r));
        return m;
    }

    public List<TicketConsistencyIssue> inconsistencies() {
        List<TicketConsistencyIssue> out = new ArrayList<>();
        for (Ticket t : repo.findAll()) {
            if (t.getBooking().getStatus() != BookingStatus.CONFIRMED)
                out.add(
                        issue(
                                "NON_CONFIRMED_BOOKING",
                                t,
                                "Ticket belongs to a non-confirmed booking"));
            if (t.getStatus() == TicketStatus.ISSUED
                    && t.getBooking().getShow().getStatus()
                            == com.chalchitraghar.modules.shows.enums.ShowStatus.CANCELLED)
                out.add(
                        issue(
                                "ISSUED_CANCELLED_SHOW",
                                t,
                                "Issued ticket belongs to a cancelled show"));
            if (t.getBookingSeat().getSeat().getSeatStatus() != SeatStatus.BOOKED)
                out.add(issue("SEAT_NOT_BOOKED", t, "Ticket seat is not BOOKED"));
        }
        return out;
    }

    private TicketConsistencyIssue issue(String c, Ticket t, String m) {
        return new TicketConsistencyIssue(
                c, t.getTicketReference(), t.getBooking().getBookingReference(), m);
    }
}
