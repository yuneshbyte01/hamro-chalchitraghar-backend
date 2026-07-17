package com.chalchitraghar.modules.reporting.analytics;

import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnalyticsQueryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public List<PatternRow> bookingPatterns(
            ReportingDateRange range, BookingPatternGrouping grouping) {
        String key =
                switch (grouping) {
                    case HOUR_OF_DAY -> "EXTRACT(HOUR FROM b.booking_time)";
                    case DAY_OF_WEEK -> "EXTRACT(DOW FROM b.booking_time)";
                    case DATE -> "CAST(b.booking_time AS DATE)";
                };
        return jdbc.query(
                "SELECT "
                        + key
                        + " bucket, COUNT(*) total, SUM(CASE WHEN b.status='CONFIRMED' THEN 1 ELSE 0 END) confirmed, SUM(CASE WHEN b.status='CANCELLED' THEN 1 ELSE 0 END) cancelled FROM bookings b WHERE b.booking_time >= :start AND b.booking_time < :end GROUP BY "
                        + key
                        + " ORDER BY "
                        + key,
                time(range),
                (rs, n) ->
                        new PatternRow(
                                rs.getString("bucket"),
                                rs.getLong("total"),
                                rs.getLong("confirmed"),
                                rs.getLong("cancelled")));
    }

    public CustomerRow customers(ReportingDateRange range) {
        return jdbc.queryForObject(
                """
            SELECT COUNT(*) any_customer,
              SUM(CASE WHEN confirmed_count > 0 THEN 1 ELSE 0 END) confirmed_customer,
              SUM(CASE WHEN confirmed_count = 1 THEN 1 ELSE 0 END) first_time,
              SUM(CASE WHEN confirmed_count >= 2 THEN 1 ELSE 0 END) repeat_customer,
              COALESCE(SUM(confirmed_count),0) confirmed_bookings,
              COALESCE(SUM(confirmed_seats),0) confirmed_seats,
              SUM(CASE WHEN cancelled_count > 0 THEN 1 ELSE 0 END) cancellation_customer
            FROM (
              SELECT b.user_id,
                SUM(CASE WHEN b.status='CONFIRMED' THEN 1 ELSE 0 END) confirmed_count,
                SUM(CASE WHEN b.status='CANCELLED' THEN 1 ELSE 0 END) cancelled_count,
                COALESCE(SUM(CASE WHEN b.status='CONFIRMED' THEN (SELECT COUNT(*) FROM booking_seats bs WHERE bs.booking_id=b.id) ELSE 0 END),0) confirmed_seats
              FROM bookings b WHERE b.booking_time >= :start AND b.booking_time < :end GROUP BY b.user_id
            ) x
            """,
                time(range),
                (rs, n) ->
                        new CustomerRow(
                                rs.getLong("any_customer"),
                                rs.getLong("confirmed_customer"),
                                rs.getLong("first_time"),
                                rs.getLong("repeat_customer"),
                                rs.getLong("confirmed_bookings"),
                                rs.getLong("confirmed_seats"),
                                rs.getLong("cancellation_customer")));
    }

    public Map<String, Long> bookingStatuses(ReportingDateRange range) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(
                "SELECT status,COUNT(*) count FROM bookings WHERE booking_time>=:start AND booking_time<:end GROUP BY status",
                time(range),
                rs -> {
                    result.put(rs.getString(1), rs.getLong(2));
                });
        return result;
    }

    public Map<String, Long> paymentStatuses(ReportingDateRange range) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(
                "SELECT status,COUNT(*) count FROM payments WHERE COALESCE(completed_at,failed_at,expired_at,cancelled_at,initiated_at,created_at)>=:start AND COALESCE(completed_at,failed_at,expired_at,cancelled_at,initiated_at,created_at)<:end GROUP BY status",
                time(range),
                rs -> {
                    result.put(rs.getString(1), rs.getLong(2));
                });
        return result;
    }

    public List<RefundCurrencyRow> refundCurrencies(ReportingDateRange range, String currency) {
        MapSqlParameterSource p = params(range).addValue("currency", currency);
        return jdbc.query(
                """
          SELECT currencies.currency, COALESCE(r.amount,0) refunded, COALESCE(r.count,0) refund_count, COALESCE(p.amount,0) gross
          FROM (SELECT currency FROM payments WHERE status IN ('SUCCESS','REFUNDED') AND completed_at>=:start AND completed_at<:end UNION SELECT currency FROM refunds WHERE status='SUCCEEDED' AND processed_at>=:start AND processed_at<:end) currencies
          LEFT JOIN (SELECT currency,SUM(amount) amount,COUNT(*) count FROM refunds WHERE status='SUCCEEDED' AND processed_at>=:start AND processed_at<:end GROUP BY currency) r ON r.currency=currencies.currency
          LEFT JOIN (SELECT currency,SUM(amount) amount FROM payments WHERE status IN ('SUCCESS','REFUNDED') AND completed_at>=:start AND completed_at<:end GROUP BY currency) p ON p.currency=currencies.currency
          WHERE (:currency IS NULL OR currencies.currency=:currency) ORDER BY currencies.currency
          """,
                p,
                (rs, n) ->
                        new RefundCurrencyRow(
                                rs.getString("currency"),
                                rs.getBigDecimal("refunded"),
                                rs.getLong("refund_count"),
                                rs.getBigDecimal("gross")));
    }

    public RefundSummaryRow refundSummary(ReportingDateRange range) {
        return jdbc.queryForObject(
                """
          SELECT SUM(CASE WHEN status='SUCCEEDED' THEN 1 ELSE 0 END) succeeded,
            SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) failed,
            SUM(CASE WHEN status IN ('REQUESTED','APPROVED','PROCESSING','MANUAL_REVIEW') THEN 1 ELSE 0 END) pending,
            COUNT(DISTINCT CASE WHEN status='SUCCEEDED' THEN booking_id END) refunded_bookings
          FROM refunds WHERE requested_at>=:start AND requested_at<:end
          """,
                time(range),
                (rs, n) ->
                        new RefundSummaryRow(
                                rs.getLong("succeeded"),
                                rs.getLong("failed"),
                                rs.getLong("pending"),
                                rs.getLong("refunded_bookings")));
    }

    public long recognizedPaidBookings(ReportingDateRange range) {
        return Optional.ofNullable(
                        jdbc.queryForObject(
                                "SELECT COUNT(DISTINCT booking_id) FROM payments WHERE status IN ('SUCCESS','REFUNDED') AND completed_at>=:start AND completed_at<:end",
                                time(range),
                                Long.class))
                .orElse(0L);
    }

    public String topRefundReason(ReportingDateRange range) {
        List<String> rows =
                jdbc.query(
                        "SELECT reason FROM refunds WHERE requested_at>=:start AND requested_at<:end GROUP BY reason ORDER BY COUNT(*) DESC,reason ASC",
                        time(range),
                        (rs, n) -> rs.getString(1));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Map<String, Long> refundBreakdown(ReportingDateRange range, String column) {
        if (!Set.of("refund_method", "provider").contains(column))
            throw new IllegalArgumentException("Unsupported refund dimension");
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query(
                "SELECT COALESCE(CAST("
                        + column
                        + " AS VARCHAR),'UNSPECIFIED'),COUNT(*) FROM refunds WHERE requested_at>=:start AND requested_at<:end GROUP BY "
                        + column
                        + " ORDER BY 1",
                time(range),
                rs -> {
                    result.put(rs.getString(1), rs.getLong(2));
                });
        return result;
    }

    public List<SeatRow> seats(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            String seatType,
            SeatDimension dimension) {
        String expression =
                switch (dimension) {
                    case TYPE -> "CAST(s.seat_type AS VARCHAR)";
                    case ROW -> "s.row_label";
                    case HALL -> "h.name";
                };
        MapSqlParameterSource p =
                new MapSqlParameterSource()
                        .addValue("start", range.startDate())
                        .addValue("end", range.endDate())
                        .addValue("movieId", movieId)
                        .addValue("hallId", hallId)
                        .addValue("seatType", seatType);
        return jdbc.query(
                "SELECT "
                        + expression
                        + " dimension,COUNT(DISTINCT s.id) generated,COUNT(DISTINCT CASE WHEN b.status='CONFIRMED' THEN bs.seat_id END) sold FROM shows sh JOIN halls h ON h.id=sh.hall_id JOIN seats s ON s.show_id=sh.id LEFT JOIN booking_seats bs ON bs.seat_id=s.id LEFT JOIN bookings b ON b.id=bs.booking_id WHERE sh.show_date BETWEEN :start AND :end AND sh.status<>'CANCELLED' AND (:movieId IS NULL OR sh.movie_id=:movieId) AND (:hallId IS NULL OR sh.hall_id=:hallId) AND (:seatType IS NULL OR s.seat_type=:seatType) GROUP BY "
                        + expression
                        + " ORDER BY "
                        + expression,
                p,
                (rs, n) ->
                        new SeatRow(
                                rs.getString("dimension"),
                                rs.getLong("generated"),
                                rs.getLong("sold")));
    }

    public List<SeatMoneyRow> seatMoney(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            String seatType,
            SeatDimension dimension) {
        String expression =
                switch (dimension) {
                    case TYPE -> "CAST(s.seat_type AS VARCHAR)";
                    case ROW -> "s.row_label";
                    case HALL -> "h.name";
                };
        MapSqlParameterSource p =
                new MapSqlParameterSource()
                        .addValue("start", range.startDate())
                        .addValue("end", range.endDate())
                        .addValue("movieId", movieId)
                        .addValue("hallId", hallId)
                        .addValue("seatType", seatType);
        return jdbc.query(
                "SELECT "
                        + expression
                        + " dimension,b.currency,SUM(bs.unit_price) sold_value,COUNT(*) sold FROM shows sh JOIN halls h ON h.id=sh.hall_id JOIN seats s ON s.show_id=sh.id JOIN booking_seats bs ON bs.seat_id=s.id JOIN bookings b ON b.id=bs.booking_id AND b.status='CONFIRMED' WHERE sh.show_date BETWEEN :start AND :end AND sh.status<>'CANCELLED' AND (:movieId IS NULL OR sh.movie_id=:movieId) AND (:hallId IS NULL OR sh.hall_id=:hallId) AND (:seatType IS NULL OR s.seat_type=:seatType) GROUP BY "
                        + expression
                        + ",b.currency ORDER BY "
                        + expression
                        + ",b.currency",
                p,
                (rs, n) ->
                        new SeatMoneyRow(
                                rs.getString("dimension"),
                                rs.getString("currency"),
                                rs.getBigDecimal("sold_value"),
                                rs.getLong("sold")));
    }

    public DataQualityRow dataQuality() {
        return jdbc.queryForObject(
                """
      SELECT
       (SELECT COUNT(*) FROM bookings b WHERE b.status='CONFIRMED' AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.booking_id=b.id AND p.status IN ('SUCCESS','REFUNDED'))) missing_payment,
       (SELECT COUNT(*) FROM payments p JOIN bookings b ON b.id=p.booking_id WHERE p.status IN ('SUCCESS','REFUNDED') AND b.status<>'CONFIRMED') payment_state,
       (SELECT COUNT(*) FROM (SELECT booking_id FROM payments WHERE status IN ('SUCCESS','REFUNDED') GROUP BY booking_id HAVING COUNT(*)>1) x) duplicate_payment,
       (SELECT COUNT(*) FROM report_deliveries WHERE status='FAILED') failed_delivery
      """,
                Map.of(),
                (rs, n) ->
                        new DataQualityRow(
                                rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)));
    }

    private Map<String, Object> time(ReportingDateRange r) {
        return Map.of("start", r.startInclusive(), "end", r.endExclusive());
    }

    private MapSqlParameterSource params(ReportingDateRange r) {
        return new MapSqlParameterSource()
                .addValue("start", r.startInclusive())
                .addValue("end", r.endExclusive());
    }

    public enum SeatDimension {
        TYPE,
        ROW,
        HALL
    }

    public record PatternRow(String bucket, long total, long confirmed, long cancelled) {}

    public record CustomerRow(
            long any,
            long confirmed,
            long firstTime,
            long repeat,
            long confirmedBookings,
            long confirmedSeats,
            long cancellations) {}

    public record RefundCurrencyRow(
            String currency, BigDecimal refunded, long refundCount, BigDecimal gross) {}

    public record RefundSummaryRow(
            long succeeded, long failed, long pending, long refundedBookings) {}

    public record SeatRow(String dimension, long generated, long sold) {}

    public record SeatMoneyRow(String dimension, String currency, BigDecimal value, long sold) {}

    public record DataQualityRow(
            long missingPayment, long paymentState, long duplicatePayment, long failedDelivery) {}
}
