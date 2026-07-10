package com.chalchitraghar.modules.halls.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.shared.exception.HallConflictException;

/** Validates a complete generated hall layout before it is persisted. */
@Component
public class SeatTemplateValidator {

    public void validate(List<SeatTemplate> templates, int hallCapacity) {
        if (templates == null || templates.size() != hallCapacity) {
            throw new HallConflictException("Generated seat count must match hall capacity");
        }

        Set<String> seatCodes = new HashSet<>();
        Set<Integer> positions = new HashSet<>();
        Map<String, Set<Integer>> numbersByRow = new TreeMap<>();

        for (int index = 0; index < templates.size(); index++) {
            SeatTemplate template = templates.get(index);
            String row = template.getRowLabel();
            if (row == null || !row.matches("[A-Z]{1,2}")) {
                throw new HallConflictException("Seat row must contain one or two uppercase letters A-Z");
            }
            Integer seatNumber = template.getSeatNumber();
            if (seatNumber == null || seatNumber < 1 || seatNumber > 100) {
                throw new HallConflictException("Seat number must be between 1 and 100");
            }
            if (template.getSeatType() != SeatType.PREMIUM && template.getSeatType() != SeatType.PLATINUM) {
                throw new HallConflictException("Seat type must be PREMIUM or PLATINUM");
            }
            String expectedCode = row + seatNumber;
            if (!expectedCode.equals(template.getSeatCode())) {
                throw new HallConflictException("Seat code must equal row label plus seat number");
            }
            if (!seatCodes.add(template.getSeatCode())) {
                throw new HallConflictException("Seat codes must be unique within a hall");
            }
            if (template.getPositionIndex() == null
                    || template.getPositionIndex() != index
                    || !positions.add(template.getPositionIndex())) {
                throw new HallConflictException("Seat position indexes must start at 0 and be sequential without gaps");
            }
            if (!numbersByRow.computeIfAbsent(row, ignored -> new HashSet<>()).add(seatNumber)) {
                throw new HallConflictException("Seat numbers must be unique within each row");
            }
        }

        numbersByRow.forEach((row, numbers) -> {
            for (int expected = 1; expected <= numbers.size(); expected++) {
                if (!numbers.contains(expected)) {
                    throw new HallConflictException("Seat numbers must be sequential within row " + row);
                }
            }
        });
    }
}
