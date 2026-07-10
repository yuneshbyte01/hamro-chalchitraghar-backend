package com.chalchitraghar.modules.halls.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

import com.chalchitraghar.modules.halls.dto.request.SeatTemplateSearchCriteria;
import com.chalchitraghar.modules.halls.dto.response.AdminSeatLayoutResponse;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.halls.repository.SeatTemplateRepository;
import com.chalchitraghar.modules.halls.mapper.SeatTemplateMapper;
import com.chalchitraghar.modules.halls.service.SeatLayoutService;
import com.chalchitraghar.modules.halls.service.SeatTemplateValidator;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.shared.exception.HallConflictException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatLayoutServiceImpl implements SeatLayoutService {

    private static final int FIXED_LAYOUT_CAPACITY = 188;
    private static final Set<String> TEMPLATE_SORT_FIELDS = Set.of(
            "positionIndex", "rowLabel", "seatNumber", "seatCode", "seatType");

    private final SeatTemplateRepository seatTemplateRepository;
    private final HallRepository hallRepository;
    private final SeatTemplateMapper seatTemplateMapper;
    private final SeatTemplateValidator seatTemplateValidator;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminSeatLayoutResponse getSeatLayout(Long hallId, SeatTemplateSearchCriteria criteria) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", hallId));
        List<SeatTemplate> templates = seatTemplateRepository.findByHallIdOrderByPositionIndexAsc(hallId);
        if (templates.isEmpty()) {
            throw new ResourceNotFoundException("Seat layout", hallId);
        }
        SeatTemplateSearchCriteria effectiveCriteria = criteria == null
                ? new SeatTemplateSearchCriteria(null, null, null, "positionIndex", "asc")
                : criteria;
        SeatType seatType = parseSeatType(effectiveCriteria.seatType());
        Comparator<SeatTemplate> comparator = templateComparator(
                effectiveCriteria.sortBy(), effectiveCriteria.sortDir());
        String search = normalize(effectiveCriteria.search());
        String row = normalize(effectiveCriteria.row());

        List<SeatTemplate> filteredTemplates = templates.stream()
                .filter(template -> search == null
                        || template.getSeatCode().toLowerCase(Locale.ROOT).contains(search)
                        || template.getRowLabel().toLowerCase(Locale.ROOT).contains(search))
                .filter(template -> seatType == null || template.getSeatType() == seatType)
                .filter(template -> row == null || template.getRowLabel().equalsIgnoreCase(row))
                .sorted(comparator)
                .toList();
        return seatTemplateMapper.toLayoutResponse(hall, filteredTemplates);
    }

    private SeatType parseSeatType(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return SeatType.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid seatType. Allowed values: PREMIUM, PLATINUM");
        }
    }

    private Comparator<SeatTemplate> templateComparator(String sortBy, String sortDir) {
        String field = sortBy == null || sortBy.isBlank() ? "positionIndex" : sortBy.trim();
        if (!TEMPLATE_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Invalid sortBy. Allowed values: positionIndex, rowLabel, seatNumber, seatCode, seatType");
        }
        String direction = sortDir == null || sortDir.isBlank() ? "asc" : sortDir.trim().toLowerCase(Locale.ROOT);
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new IllegalArgumentException("Invalid sortDir. Allowed values: asc, desc");
        }
        Comparator<SeatTemplate> comparator = switch (field) {
            case "rowLabel" -> Comparator.comparing(SeatTemplate::getRowLabel, String.CASE_INSENSITIVE_ORDER);
            case "seatNumber" -> Comparator.comparing(SeatTemplate::getSeatNumber);
            case "seatCode" -> Comparator.comparing(SeatTemplate::getSeatCode, String.CASE_INSENSITIVE_ORDER);
            case "seatType" -> Comparator.comparing(template -> template.getSeatType().name());
            default -> Comparator.comparing(SeatTemplate::getPositionIndex);
        };
        comparator = comparator.thenComparing(SeatTemplate::getPositionIndex);
        return direction.equals("desc") ? comparator.reversed() : comparator;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public void generateSeatTemplates(Long hallId) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", hallId));
        if (hall.getStatus() != Status.ACTIVE) {
            throw new HallConflictException("Cannot generate seat layout for an inactive hall");
        }
        if (seatTemplateRepository.existsByHallId(hallId)) {
            throw new HallConflictException("Seat layout already exists for this hall");
        }
        if (!Integer.valueOf(FIXED_LAYOUT_CAPACITY).equals(hall.getCapacity())) {
            throw new HallConflictException("Seat layout generation currently requires hall capacity to be 188");
        }
        List<SeatTemplate> templates = new java.util.ArrayList<>(FIXED_LAYOUT_CAPACITY);
        int index = 0;
        for (int seat = 1; seat <= 8; seat++) {
            templates.add(buildSeat(hall, "A", seat, SeatType.PREMIUM, index++));
        }
        for (char row = 'B'; row <= 'J'; row++) {
            for (int seat = 1; seat <= 20; seat++) {
                templates.add(buildSeat(hall, String.valueOf(row), seat, SeatType.PLATINUM, index++));
            }
        }
        seatTemplateValidator.validate(templates, hall.getCapacity());
        seatTemplateRepository.saveAll(templates);
    }

    private SeatTemplate buildSeat(Hall hall, String rowLabel, int seatNumber, SeatType seatType, int positionIndex) {
        return SeatTemplate.generated(hall, rowLabel, seatNumber, seatType, positionIndex);
    }
}
