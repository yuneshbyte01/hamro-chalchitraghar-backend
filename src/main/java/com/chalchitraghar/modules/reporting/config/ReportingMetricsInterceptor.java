package com.chalchitraghar.modules.reporting.config;

import io.micrometer.core.instrument.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.*;

@Component
@RequiredArgsConstructor
public class ReportingMetricsInterceptor implements HandlerInterceptor {
    private static final String SAMPLE = ReportingMetricsInterceptor.class.getName() + ".sample";
    private final MeterRegistry registry;

    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(SAMPLE, Timer.start(registry));
        return true;
    }

    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        Object value = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String reportType = value == null ? "unknown" : value.toString();
        String status = Integer.toString(response.getStatus());
        registry.counter("report_requests_total", "reportType", reportType, "status", status)
                .increment();
        Timer.Sample sample = (Timer.Sample) request.getAttribute(SAMPLE);
        if (sample != null)
            sample.stop(
                    registry.timer(
                            "report_request_duration", "reportType", reportType, "status", status));
        if (exception != null || response.getStatus() >= 500)
            registry.counter("report_query_failures_total", "reportType", reportType).increment();
    }
}
