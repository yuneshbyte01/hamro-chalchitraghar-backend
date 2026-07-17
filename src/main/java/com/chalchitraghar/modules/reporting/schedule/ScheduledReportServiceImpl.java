package com.chalchitraghar.modules.reporting.schedule;

import com.chalchitraghar.modules.reporting.config.ReportingOperationsProperties;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.enums.*;
import com.chalchitraghar.modules.reporting.export.*;
import com.chalchitraghar.modules.reporting.schedule.dto.*;
import com.chalchitraghar.modules.reporting.schedule.entity.*;
import com.chalchitraghar.modules.reporting.schedule.repository.*;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.response.PageResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduledReportServiceImpl implements ScheduledReportService {
    private final ScheduledReportRepository schedules;
    private final ReportDeliveryRepository deliveries;
    private final ScheduledReportPeriodCalculator periods;
    private final ReportingExportService exports;
    private final ReportAttachmentMailSender mail;
    private final ReportingOperationsProperties properties;
    private final MeterRegistry metrics;
    private final Clock clock;

    @Transactional
    public ScheduledReportResponse create(ScheduledReportRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        ScheduledReport report = new ScheduledReport();
        apply(report, request);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        if (report.isEnabled())
            report.setNextRunAt(periods.nextRun(report.getScheduleFrequency(), now.minusDays(1)));
        return response(schedules.save(report));
    }

    @Transactional(readOnly = true)
    public List<ScheduledReportResponse> list() {
        return schedules.findByDeletedFalseOrderByNameAsc().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public ScheduledReportResponse get(long id) {
        return response(entity(id));
    }

    @Transactional
    public ScheduledReportResponse update(long id, ScheduledReportRequest request) {
        ScheduledReport value = entity(id);
        apply(value, request);
        value.setUpdatedAt(LocalDateTime.now(clock));
        if (value.isEnabled() && value.getNextRunAt() == null)
            value.setNextRunAt(
                    periods.nextRun(
                            value.getScheduleFrequency(), LocalDateTime.now(clock).minusDays(1)));
        return response(value);
    }

    @Transactional
    public ScheduledReportResponse enable(long id, boolean enabled) {
        ScheduledReport value = entity(id);
        value.setEnabled(enabled);
        value.setNextRunAt(
                enabled
                        ? periods.nextRun(
                                value.getScheduleFrequency(), LocalDateTime.now(clock).minusDays(1))
                        : null);
        value.setUpdatedAt(LocalDateTime.now(clock));
        return response(value);
    }

    @Transactional
    public void delete(long id) {
        ScheduledReport value = entity(id);
        value.setDeleted(true);
        value.setEnabled(false);
        value.setNextRunAt(null);
        value.setUpdatedAt(LocalDateTime.now(clock));
    }

    @Transactional
    public ReportDeliveryResponse run(long id) {
        return response(generate(entity(id)));
    }

    @Transactional
    public int dispatchDue() {
        var due =
                schedules
                        .findByEnabledTrueAndDeletedFalseAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
                                LocalDateTime.now(clock),
                                PageRequest.of(0, properties.schedule().batchSize()));
        due.forEach(this::generate);
        return due.size();
    }

    @Transactional
    public int retryFailed() {
        var due =
                deliveries
                        .findByStatusAndAttemptCountLessThanAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                                ReportDeliveryStatus.FAILED,
                                properties.schedule().maxAttempts(),
                                LocalDateTime.now(clock),
                                PageRequest.of(0, properties.schedule().batchSize()));
        due.forEach(this::attempt);
        return due.size();
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportDeliveryResponse> deliveries(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException(
                    "page must be non-negative and size must be between 1 and 100");
        var result =
                deliveries.findAll(
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "createdAt")
                                        .and(Sort.by(Sort.Direction.DESC, "id"))));
        return PageResponse.from(result, result.getContent().stream().map(this::response).toList());
    }

    @Transactional(readOnly = true)
    public ReportDeliveryResponse delivery(long id) {
        return response(
                deliveries
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Report delivery", id)));
    }

    private ReportDelivery generate(ScheduledReport schedule) {
        var period = periods.completed(schedule.getScheduleFrequency(), clock);
        String key = key(schedule, period);
        ReportDelivery delivery =
                deliveries
                        .findByIdempotencyKey(key)
                        .orElseGet(() -> createDelivery(schedule, period, key));
        if (delivery.getStatus() != ReportDeliveryStatus.SENT) attempt(delivery);
        LocalDateTime now = LocalDateTime.now(clock);
        schedule.setLastRunAt(now);
        schedule.setNextRunAt(periods.nextRun(schedule.getScheduleFrequency(), now));
        schedule.setUpdatedAt(now);
        return delivery;
    }

    private ReportDelivery createDelivery(
            ScheduledReport schedule, ScheduledReportPeriodCalculator.Period period, String key) {
        LocalDateTime now = LocalDateTime.now(clock);
        ReportDelivery d =
                ReportDelivery.builder()
                        .scheduledReport(schedule)
                        .periodStart(period.start())
                        .periodEnd(period.end())
                        .format(schedule.getDeliveryFormat())
                        .recipientEmail(schedule.getRecipientEmail())
                        .status(ReportDeliveryStatus.PENDING)
                        .attemptCount(0)
                        .idempotencyKey(key)
                        .fileName(
                                baseName(schedule, period)
                                        + extension(schedule.getDeliveryFormat()))
                        .build();
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        return deliveries.save(d);
    }

    private void attempt(ReportDelivery delivery) {
        if (delivery.getStatus() == ReportDeliveryStatus.SENT
                || delivery.getAttemptCount() >= properties.schedule().maxAttempts()) return;
        LocalDateTime now = LocalDateTime.now(clock);
        if (delivery.getAttemptCount() > 0)
            metrics.counter("report_delivery_retries_total", "format", delivery.getFormat().name())
                    .increment();
        delivery.setStatus(ReportDeliveryStatus.PROCESSING);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(now);
        delivery.setUpdatedAt(now);
        try {
            ReportExportResult file = export(delivery);
            delivery.setFileName(file.fileName());
            delivery.setFileSizeBytes((long) file.content().length);
            if (properties.schedule().emailEnabled())
                mail.send(delivery.getRecipientEmail(), subject(delivery), body(delivery), file);
            delivery.setStatus(ReportDeliveryStatus.SENT);
            delivery.setSentAt(now);
            delivery.setFailureReason(null);
            metrics.counter(
                            "report_delivery_total",
                            "status",
                            "sent",
                            "format",
                            delivery.getFormat().name())
                    .increment();
        } catch (Exception exception) {
            delivery.setStatus(ReportDeliveryStatus.FAILED);
            delivery.setFailureReason("Scheduled report generation or delivery failed");
            delivery.setNextAttemptAt(now.plus(properties.schedule().retryDelay()));
            metrics.counter("report_delivery_failures_total", "format", delivery.getFormat().name())
                    .increment();
        }
    }

    private ReportExportResult export(ReportDelivery d) {
        Timer.Sample sample = Timer.start(metrics);
        ScheduledReport s = d.getScheduledReport();
        ReportingDateRange range =
                ReportingDateRange.of(d.getPeriodStart(), d.getPeriodEnd(), clock);
        try {
            ReportExportResult result =
                    switch (s.getReportType()) {
                        case REVENUE, DASHBOARD_SUMMARY ->
                                exports.revenue(
                                        range, s.getCurrency(), ReportGrouping.DAY, d.getFormat());
                        case BOOKINGS -> exports.bookings(range, ReportGrouping.DAY, d.getFormat());
                        case OCCUPANCY ->
                                exports.occupancy(
                                        range,
                                        null,
                                        null,
                                        null,
                                        OccupancySort.SHOW_DATE,
                                        ReportSortDirection.ASC,
                                        d.getFormat());
                        case MOVIE_PERFORMANCE ->
                                exports.movies(
                                        range,
                                        s.getCurrency(),
                                        MoviePerformanceSort.TITLE,
                                        ReportSortDirection.ASC,
                                        d.getFormat());
                        case HALL_PERFORMANCE ->
                                exports.halls(
                                        range,
                                        s.getCurrency(),
                                        HallPerformanceSort.HALL_NAME,
                                        ReportSortDirection.ASC,
                                        d.getFormat());
                        case SHOW_PERFORMANCE ->
                                exports.shows(
                                        range,
                                        null,
                                        null,
                                        null,
                                        s.getCurrency(),
                                        ShowPerformanceSort.SHOW_DATE,
                                        ReportSortDirection.ASC,
                                        d.getFormat());
                    };
            metrics.counter(
                            "report_generation_total",
                            "reportType",
                            s.getReportType().name(),
                            "format",
                            d.getFormat().name())
                    .increment();
            return result;
        } finally {
            sample.stop(
                    metrics.timer(
                            "report_generation_duration",
                            "reportType",
                            s.getReportType().name(),
                            "format",
                            d.getFormat().name()));
        }
    }

    private void apply(ScheduledReport report, ScheduledReportRequest r) {
        report.setName(r.name().trim());
        report.setReportType(r.reportType());
        report.setScheduleFrequency(r.scheduleFrequency());
        report.setDeliveryFormat(r.deliveryFormat());
        report.setRecipientEmail(r.recipientEmail().trim().toLowerCase(Locale.ROOT));
        report.setCurrency(
                r.currency() == null ? null : r.currency().trim().toUpperCase(Locale.ROOT));
        report.setEnabled(r.enabled());
    }

    private ScheduledReport entity(long id) {
        ScheduledReport value =
                schedules
                        .findById(id)
                        .filter(v -> !v.isDeleted())
                        .orElseThrow(() -> new ResourceNotFoundException("Scheduled report", id));
        return value;
    }

    private ScheduledReportResponse response(ScheduledReport v) {
        return new ScheduledReportResponse(
                v.getId(),
                v.getName(),
                v.getReportType(),
                v.getScheduleFrequency(),
                v.getDeliveryFormat(),
                v.getRecipientEmail(),
                v.getCurrency(),
                v.isEnabled(),
                v.getLastRunAt(),
                v.getNextRunAt());
    }

    private ReportDeliveryResponse response(ReportDelivery v) {
        return new ReportDeliveryResponse(
                v.getId(),
                v.getScheduledReport().getId(),
                v.getPeriodStart(),
                v.getPeriodEnd(),
                v.getFormat(),
                v.getStatus(),
                v.getAttemptCount(),
                v.getLastAttemptAt(),
                v.getSentAt(),
                v.getFailureReason(),
                v.getFileName(),
                v.getFileSizeBytes());
    }

    private String key(ScheduledReport s, ScheduledReportPeriodCalculator.Period p) {
        try {
            String value =
                    s.getId()
                            + "|"
                            + s.getReportType()
                            + "|"
                            + p.start()
                            + "|"
                            + p.end()
                            + "|"
                            + s.getDeliveryFormat()
                            + "|"
                            + s.getRecipientEmail();
            return java.util.HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String baseName(ScheduledReport s, ScheduledReportPeriodCalculator.Period p) {
        return s.getReportType().name().toLowerCase(Locale.ROOT).replace('_', '-')
                + "-report-"
                + p.start()
                + "-to-"
                + p.end();
    }

    private String extension(ReportExportFormat f) {
        return f == ReportExportFormat.CSV ? ".csv" : ".xlsx";
    }

    private String subject(ReportDelivery d) {
        return "Hamro Chalchitraghar "
                + d.getScheduledReport().getScheduleFrequency()
                + " "
                + d.getScheduledReport().getReportType()
                + " Report — "
                + d.getPeriodStart()
                + " to "
                + d.getPeriodEnd();
    }

    private String body(ReportDelivery d) {
        return "Attached is the "
                + d.getScheduledReport().getReportType()
                + " report for "
                + d.getPeriodStart()
                + " through "
                + d.getPeriodEnd()
                + ". Generated in "
                + clock.getZone()
                + ".";
    }
}
