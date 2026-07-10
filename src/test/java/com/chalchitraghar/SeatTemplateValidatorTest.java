package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import com.chalchitraghar.modules.halls.service.SeatTemplateValidator;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.shared.exception.HallConflictException;

class SeatTemplateValidatorTest {

    private final SeatTemplateValidator validator = new SeatTemplateValidator();

    @Test
    void rejectsInvalidRow() {
        assertInvalid(template("a", 1, SeatType.PREMIUM, 0),
                "Seat row must contain one or two uppercase letters A-Z");
    }

    @Test
    void rejectsInvalidSeatNumberAndNonSequentialNumbers() {
        assertInvalid(template("A", 101, SeatType.PREMIUM, 0), "Seat number must be between 1 and 100");
        assertThatThrownBy(() -> validator.validate(List.of(
                template("A", 1, SeatType.PREMIUM, 0),
                template("A", 3, SeatType.PREMIUM, 1)), 2))
                .isInstanceOf(HallConflictException.class)
                .hasMessage("Seat numbers must be sequential within row A");
    }

    @Test
    void rejectsInvalidPosition() {
        assertInvalid(template("A", 1, SeatType.PREMIUM, 1),
                "Seat position indexes must start at 0 and be sequential without gaps");
    }

    @Test
    void rejectsInvalidSeatType() {
        assertInvalid(template("A", 1, null, 0), "Seat type must be PREMIUM or PLATINUM");
    }

    @Test
    void rejectsInvalidOrDuplicateSeatCode() {
        SeatTemplate invalid = template("A", 1, SeatType.PREMIUM, 0);
        invalid.setRowLabel("B");
        assertInvalid(invalid, "Seat code must equal row label plus seat number");
    }

    @Test
    void rejectsCapacityMismatch() {
        assertThatThrownBy(() -> validator.validate(
                List.of(template("A", 1, SeatType.PREMIUM, 0)), 2))
                .isInstanceOf(HallConflictException.class)
                .hasMessage("Generated seat count must match hall capacity");
    }

    private void assertInvalid(SeatTemplate template, String message) {
        assertThatThrownBy(() -> validator.validate(List.of(template), 1))
                .isInstanceOf(HallConflictException.class)
                .hasMessage(message);
    }

    private SeatTemplate template(String row, int number, SeatType type, int position) {
        return SeatTemplate.generated(null, row, number, type, position);
    }
}
