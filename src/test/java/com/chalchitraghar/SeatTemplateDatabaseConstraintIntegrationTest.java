package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class SeatTemplateDatabaseConstraintIntegrationTest extends AbstractIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void duplicateSeatCodeIsPrevented() {
        Hall hall = saveHall("Duplicate Code Hall", Status.ACTIVE);
        insertTemplate(hall.getId(), "A", 1, "A1", 0);
        assertThatThrownBy(() -> insertTemplate(hall.getId(), "B", 2, "A1", 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateRowAndSeatNumberIsPrevented() {
        Hall hall = saveHall("Duplicate Row Seat Hall", Status.ACTIVE);
        insertTemplate(hall.getId(), "A", 1, "A1", 0);
        assertThatThrownBy(() -> insertTemplate(hall.getId(), "A", 1, "DIFFERENT", 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePositionIsPrevented() {
        Hall hall = saveHall("Duplicate Position Hall", Status.ACTIVE);
        insertTemplate(hall.getId(), "A", 1, "A1", 0);
        assertThatThrownBy(() -> insertTemplate(hall.getId(), "B", 2, "B2", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertTemplate(Long hallId, String row, int number, String code, int position) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(
                """
                INSERT INTO seat_templates
                    (created_at, updated_at, hall_id, row_label, seat_number, seat_code, seat_type, position_index)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                now,
                now,
                hallId,
                row,
                number,
                code,
                "PREMIUM",
                position);
    }
}
