package com.chalchitraghar.modules.halls.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.halls.dto.response.AdminSeatLayoutResponse;
import com.chalchitraghar.modules.halls.dto.response.AdminSeatTemplateSummaryResponse;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import com.chalchitraghar.modules.seats.enums.SeatType;

/** Maps hall seat templates to dedicated admin response contracts. */
@Component
public class SeatTemplateMapper {

    public AdminSeatTemplateSummaryResponse toSummary(SeatTemplate template) {
        if (template == null) {
            return null;
        }
        return new AdminSeatTemplateSummaryResponse(
                template.getId(),
                template.getRowLabel(),
                template.getSeatNumber(),
                template.getSeatCode(),
                template.getSeatType(),
                template.getPositionIndex());
    }

    public AdminSeatLayoutResponse toLayoutResponse(Hall hall, List<SeatTemplate> templates) {
        if (hall == null) {
            return null;
        }
        List<AdminSeatTemplateSummaryResponse> summaries = templates.stream()
                .map(this::toSummary)
                .toList();
        int premiumSeats = (int) templates.stream()
                .filter(template -> template.getSeatType() == SeatType.PREMIUM)
                .count();
        int platinumSeats = (int) templates.stream()
                .filter(template -> template.getSeatType() == SeatType.PLATINUM)
                .count();
        return new AdminSeatLayoutResponse(
                hall.getId(),
                hall.getName(),
                hall.getCapacity(),
                templates.size(),
                premiumSeats,
                platinumSeats,
                summaries);
    }
}
