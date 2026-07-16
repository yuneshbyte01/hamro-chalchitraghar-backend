package com.chalchitraghar.modules.reporting.repository;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReportingAnalyticsRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public List<DailyMoneyRow> dailyPayments(
            LocalDateTime start, LocalDateTime end, String currency) {
        return dailyMoney(
                "payments",
                "completed_at",
                "status IN ('SUCCESS','REFUNDED')",
                start,
                end,
                currency);
    }

    public List<DailyMoneyRow> dailyRefunds(
            LocalDateTime start, LocalDateTime end, String currency) {
        return dailyMoney("refunds", "processed_at", "status = 'SUCCEEDED'", start, end, currency);
    }

    private List<DailyMoneyRow> dailyMoney(
            String table,
            String timestamp,
            String status,
            LocalDateTime start,
            LocalDateTime end,
            String currency) {
        String sql =
                "SELECT CAST("
                        + timestamp
                        + " AS DATE) report_day, currency, COUNT(*) row_count, COALESCE(SUM(amount),0) amount "
                        + "FROM "
                        + table
                        + " WHERE "
                        + status
                        + " AND "
                        + timestamp
                        + " >= :start AND "
                        + timestamp
                        + " < :end AND (:currency IS NULL OR currency=:currency) "
                        + "GROUP BY CAST("
                        + timestamp
                        + " AS DATE), currency ORDER BY report_day,currency";
        return jdbc.query(
                sql,
                params(start, end).addValue("currency", currency),
                (rs, n) ->
                        new DailyMoneyRow(
                                rs.getObject("report_day", LocalDate.class),
                                rs.getString("currency"),
                                rs.getLong("row_count"),
                                rs.getBigDecimal("amount")));
    }

    public List<DailyBookingRow> dailyBookings(LocalDateTime start, LocalDateTime end) {
        return jdbc.query(
                """
                SELECT CAST(booking_time AS DATE) report_day, status, COUNT(*) row_count
                FROM bookings WHERE booking_time >= :start AND booking_time < :end
                GROUP BY CAST(booking_time AS DATE), status ORDER BY report_day,status
                """,
                params(start, end),
                (rs, n) ->
                        new DailyBookingRow(
                                rs.getObject("report_day", LocalDate.class),
                                BookingStatus.valueOf(rs.getString("status")),
                                rs.getLong("row_count")));
    }

    public OccupancyTotals occupancyTotals(
            LocalDate start, LocalDate end, Long movieId, Long hallId, ShowStatus status) {
        String filters = showFilters(movieId, hallId, status);
        return jdbc.queryForObject(
                "SELECT COUNT(*) total_shows,COALESCE(SUM(capacity),0) capacity,COALESCE(SUM(sold),0) sold FROM ("
                        + occupancySelect()
                        + filters
                        + " GROUP BY sh.id,m.id,m.title,h.id,h.name,sh.show_date,sh.show_time,sh.status) x",
                showParams(start, end, movieId, hallId, status),
                (rs, n) ->
                        new OccupancyTotals(
                                rs.getLong("total_shows"),
                                rs.getLong("capacity"),
                                rs.getLong("sold")));
    }

    public PageRows<OccupancyRow> occupancyPage(
            LocalDate start,
            LocalDate end,
            Long movieId,
            Long hallId,
            ShowStatus status,
            OccupancySort sort,
            ReportSortDirection direction,
            int page,
            int size) {
        String filters = showFilters(movieId, hallId, status);
        String order =
                switch (sort) {
                    case SHOW_DATE -> "show_date";
                    case OCCUPANCY -> "CASE WHEN capacity=0 THEN 0 ELSE sold*1.0/capacity END";
                    case SOLD_SEATS -> "sold";
                    case CAPACITY -> "capacity";
                    case MOVIE_TITLE -> "movie_title";
                    case HALL_NAME -> "hall_name";
                };
        MapSqlParameterSource p =
                showParams(start, end, movieId, hallId, status)
                        .addValue("limit", size)
                        .addValue("offset", page * size);
        long total =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM shows sh WHERE "
                                + simpleShowFilters(movieId, hallId, status),
                        p,
                        Long.class);
        List<OccupancyRow> rows =
                jdbc.query(
                        "SELECT * FROM ("
                                + occupancySelect()
                                + filters
                                + " GROUP BY sh.id,m.id,m.title,h.id,h.name,sh.show_date,sh.show_time,sh.status) x ORDER BY "
                                + order
                                + " "
                                + direction.name()
                                + ", show_id ASC LIMIT :limit OFFSET :offset",
                        p,
                        this::occupancyRow);
        return new PageRows<>(rows, total);
    }

    public PageRows<DimensionRow> dimensionPage(
            Dimension dimension,
            LocalDate start,
            LocalDate end,
            Long movieId,
            Long hallId,
            ShowStatus status,
            String currency,
            String sort,
            ReportSortDirection direction,
            int page,
            int size) {
        String id = dimension.idExpression();
        String group = dimension.groupExpressions();
        String filters = performanceFilters(dimension, movieId, hallId, status);
        String select =
                "SELECT "
                        + dimension.selectExpressions()
                        + ",COUNT(DISTINCT sh.id) show_count,COUNT(DISTINCT b.id) booking_count,"
                        + "COUNT(DISTINCT bs.seat_id) sold,COUNT(DISTINCT s.id) capacity FROM shows sh "
                        + "JOIN movies m ON m.id=sh.movie_id JOIN halls h ON h.id=sh.hall_id "
                        + "LEFT JOIN seats s ON s.show_id=sh.id LEFT JOIN bookings b ON b.show_id=sh.id AND b.status='CONFIRMED' "
                        + "LEFT JOIN booking_seats bs ON bs.booking_id=b.id WHERE "
                        + filters
                        + " GROUP BY "
                        + group;
        String order = performanceOrder(sort, dimension, id, currency);
        MapSqlParameterSource p =
                performanceParams(start, end, movieId, hallId, status, currency)
                        .addValue("limit", size)
                        .addValue("offset", page * size);
        long total =
                jdbc.queryForObject(
                        "SELECT COUNT(DISTINCT "
                                + id
                                + ") FROM shows sh JOIN movies m ON m.id=sh.movie_id JOIN halls h ON h.id=sh.hall_id WHERE "
                                + filters,
                        p,
                        Long.class);
        List<DimensionRow> rows =
                jdbc.query(
                        select
                                + " ORDER BY "
                                + order
                                + " "
                                + direction.name()
                                + ", dimension_name ASC, dimension_id ASC LIMIT :limit OFFSET :offset",
                        p,
                        this::dimensionRow);
        return new PageRows<>(rows, total);
    }

    public List<DimensionMoneyRow> dimensionMoney(
            Dimension dimension,
            Collection<Long> ids,
            LocalDate start,
            LocalDate end,
            String currency,
            boolean refunds) {
        if (ids.isEmpty()) return List.of();
        String table = refunds ? "refunds r JOIN payments p ON p.id=r.payment_id" : "payments p";
        String status = refunds ? "r.status='SUCCEEDED'" : "p.status IN ('SUCCESS','REFUNDED')";
        String amount = refunds ? "r.amount" : "p.amount";
        String moneyCurrency = refunds ? "r.currency" : "p.currency";
        String sql =
                "SELECT "
                        + dimension.moneyIdExpression()
                        + " dimension_id,"
                        + moneyCurrency
                        + " currency,COALESCE(SUM("
                        + amount
                        + "),0) amount FROM "
                        + table
                        + " JOIN bookings b ON b.id=p.booking_id JOIN shows sh ON sh.id=b.show_id WHERE "
                        + status
                        + " AND sh.show_date>=:startDate AND sh.show_date<=:endDate AND sh.status<>'CANCELLED' AND "
                        + dimension.moneyIdExpression()
                        + " IN (:ids) AND (:currency IS NULL OR "
                        + moneyCurrency
                        + "=:currency) GROUP BY "
                        + dimension.moneyIdExpression()
                        + ","
                        + moneyCurrency;
        return jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("ids", ids)
                        .addValue("startDate", start)
                        .addValue("endDate", end)
                        .addValue("currency", currency),
                (rs, n) ->
                        new DimensionMoneyRow(
                                rs.getLong("dimension_id"),
                                rs.getString("currency"),
                                rs.getBigDecimal("amount"),
                                refunds));
    }

    private String performanceOrder(
            String sort, Dimension dimension, String dimensionId, String currency) {
        return switch (sort) {
            case "TICKETS_SOLD" -> "sold";
            case "OCCUPANCY" ->
                    "CASE WHEN COUNT(DISTINCT s.id)=0 THEN 0 ELSE COUNT(DISTINCT bs.seat_id)*1.0/COUNT(DISTINCT s.id) END";
            case "SHOW_COUNT" -> "show_count";
            case "AVERAGE_ATTENDANCE" ->
                    "CASE WHEN COUNT(DISTINCT sh.id)=0 THEN 0 ELSE COUNT(DISTINCT bs.seat_id)*1.0/COUNT(DISTINCT sh.id) END";
            case "BOOKING_COUNT" -> "booking_count";
            case "TITLE", "HALL_NAME" -> "dimension_name";
            case "SHOW_DATE" -> "show_date";
            case "REVENUE" ->
                    "(SELECT COALESCE(SUM(rp.amount),0) FROM payments rp JOIN bookings rb ON rb.id=rp.booking_id JOIN shows rs ON rs.id=rb.show_id WHERE rp.status IN ('SUCCESS','REFUNDED') AND "
                            + dimension.revenueCorrelation()
                            + " AND (:currency IS NULL OR rp.currency=:currency))";
            default -> throw new IllegalArgumentException("Unsupported performance sort");
        };
    }

    private String performanceFilters(
            Dimension dimension, Long movieId, Long hallId, ShowStatus status) {
        StringBuilder sql =
                new StringBuilder("sh.show_date>=:startDate AND sh.show_date<=:endDate ");
        if (dimension != Dimension.SHOW || status == null)
            sql.append("AND sh.status<>'CANCELLED' ");
        else sql.append("AND sh.status=:status ");
        if (movieId != null) sql.append("AND sh.movie_id=:movieId ");
        if (hallId != null) sql.append("AND sh.hall_id=:hallId ");
        return sql.toString();
    }

    private MapSqlParameterSource performanceParams(
            LocalDate start,
            LocalDate end,
            Long movieId,
            Long hallId,
            ShowStatus status,
            String currency) {
        return new MapSqlParameterSource()
                .addValue("startDate", start)
                .addValue("endDate", end)
                .addValue("movieId", movieId)
                .addValue("hallId", hallId)
                .addValue("status", status == null ? null : status.name())
                .addValue("currency", currency);
    }

    private String occupancySelect() {
        return "SELECT sh.id show_id,m.id movie_id,m.title movie_title,h.id hall_id,h.name hall_name,sh.show_date,sh.show_time,sh.status show_status,COUNT(DISTINCT s.id) capacity,COUNT(DISTINCT bs.seat_id) sold FROM shows sh JOIN movies m ON m.id=sh.movie_id JOIN halls h ON h.id=sh.hall_id LEFT JOIN seats s ON s.show_id=sh.id LEFT JOIN bookings b ON b.show_id=sh.id AND b.status='CONFIRMED' LEFT JOIN booking_seats bs ON bs.booking_id=b.id WHERE ";
    }

    private String showFilters(Long movieId, Long hallId, ShowStatus status) {
        return simpleShowFilters(movieId, hallId, status);
    }

    private String simpleShowFilters(Long movieId, Long hallId, ShowStatus status) {
        StringBuilder sql =
                new StringBuilder("sh.show_date>=:startDate AND sh.show_date<=:endDate ");
        if (status == null) sql.append("AND sh.status<>'CANCELLED' ");
        else sql.append("AND sh.status=:status ");
        if (movieId != null) sql.append("AND sh.movie_id=:movieId ");
        if (hallId != null) sql.append("AND sh.hall_id=:hallId ");
        return sql.toString();
    }

    private MapSqlParameterSource showParams(
            LocalDate start, LocalDate end, Long movieId, Long hallId, ShowStatus status) {
        return new MapSqlParameterSource()
                .addValue("startDate", start)
                .addValue("endDate", end)
                .addValue("movieId", movieId)
                .addValue("hallId", hallId)
                .addValue("status", status == null ? null : status.name());
    }

    private MapSqlParameterSource params(LocalDateTime start, LocalDateTime end) {
        return new MapSqlParameterSource().addValue("start", start).addValue("end", end);
    }

    private OccupancyRow occupancyRow(ResultSet rs, int n) throws SQLException {
        return new OccupancyRow(
                rs.getLong("show_id"),
                rs.getLong("movie_id"),
                rs.getString("movie_title"),
                rs.getLong("hall_id"),
                rs.getString("hall_name"),
                rs.getObject("show_date", LocalDate.class),
                rs.getObject("show_time", LocalTime.class),
                ShowStatus.valueOf(rs.getString("show_status")),
                rs.getLong("capacity"),
                rs.getLong("sold"));
    }

    private DimensionRow dimensionRow(ResultSet rs, int n) throws SQLException {
        return new DimensionRow(
                rs.getLong("dimension_id"),
                rs.getString("dimension_name"),
                rs.getString("dimension_status"),
                rs.getLong("configured_capacity"),
                rs.getObject("movie_id") == null ? null : rs.getLong("movie_id"),
                rs.getString("movie_title"),
                rs.getObject("hall_id") == null ? null : rs.getLong("hall_id"),
                rs.getString("hall_name"),
                rs.getObject("show_date", LocalDate.class),
                rs.getObject("show_time", LocalTime.class),
                rs.getLong("show_count"),
                rs.getLong("booking_count"),
                rs.getLong("sold"),
                rs.getLong("capacity"));
    }

    public enum Dimension {
        MOVIE(
                "m.id",
                "m.id dimension_id,m.title dimension_name,m.status dimension_status,0 configured_capacity,NULL movie_id,NULL movie_title,NULL hall_id,NULL hall_name,NULL show_date,NULL show_time",
                "m.id,m.title,m.status",
                "sh.movie_id",
                "rs.movie_id=m.id"),
        HALL(
                "h.id",
                "h.id dimension_id,h.name dimension_name,h.status dimension_status,h.capacity configured_capacity,NULL movie_id,NULL movie_title,NULL hall_id,NULL hall_name,NULL show_date,NULL show_time",
                "h.id,h.name,h.status,h.capacity",
                "sh.hall_id",
                "rs.hall_id=h.id"),
        SHOW(
                "sh.id",
                "sh.id dimension_id,CAST(sh.id AS VARCHAR) dimension_name,sh.status dimension_status,0 configured_capacity,m.id movie_id,m.title movie_title,h.id hall_id,h.name hall_name,sh.show_date,sh.show_time",
                "sh.id,sh.status,m.id,m.title,h.id,h.name,sh.show_date,sh.show_time",
                "sh.id",
                "rs.id=sh.id");

        private final String idExpression;
        private final String selectExpressions;
        private final String groupExpressions;
        private final String moneyIdExpression;
        private final String revenueCorrelation;

        Dimension(
                String idExpression,
                String selectExpressions,
                String groupExpressions,
                String moneyIdExpression,
                String revenueCorrelation) {
            this.idExpression = idExpression;
            this.selectExpressions = selectExpressions;
            this.groupExpressions = groupExpressions;
            this.moneyIdExpression = moneyIdExpression;
            this.revenueCorrelation = revenueCorrelation;
        }

        public String idExpression() {
            return idExpression;
        }

        public String selectExpressions() {
            return selectExpressions;
        }

        public String groupExpressions() {
            return groupExpressions;
        }

        public String moneyIdExpression() {
            return moneyIdExpression;
        }

        public String revenueCorrelation() {
            return revenueCorrelation;
        }
    }

    public record DailyMoneyRow(LocalDate day, String currency, long count, BigDecimal amount) {}

    public record DailyBookingRow(LocalDate day, BookingStatus status, long count) {}

    public record OccupancyTotals(long shows, long capacity, long sold) {}

    public record OccupancyRow(
            long showId,
            long movieId,
            String movieTitle,
            long hallId,
            String hallName,
            LocalDate showDate,
            LocalTime showTime,
            ShowStatus status,
            long capacity,
            long sold) {}

    public record DimensionRow(
            long id,
            String name,
            String status,
            long configuredCapacity,
            Long movieId,
            String movieTitle,
            Long hallId,
            String hallName,
            LocalDate showDate,
            LocalTime showTime,
            long showCount,
            long bookingCount,
            long sold,
            long capacity) {}

    public record DimensionMoneyRow(long id, String currency, BigDecimal amount, boolean refund) {}

    public record PageRows<T>(List<T> content, long total) {}
}
