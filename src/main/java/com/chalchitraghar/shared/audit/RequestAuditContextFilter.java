package com.chalchitraghar.shared.audit;

import com.chalchitraghar.shared.config.AuditRequestContextProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.InetAddress;
import java.time.*;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class RequestAuditContextFilter extends OncePerRequestFilter {
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]+");
    private final AuditRequestContextProperties properties;
    private final Clock clock;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        String requestId =
                validId(
                        request.getHeader(properties.getRequestIdHeader()),
                        properties.getMaxRequestIdLength());
        if (requestId == null) requestId = UUID.randomUUID().toString();
        String correlationId =
                validId(
                        request.getHeader(properties.getCorrelationIdHeader()),
                        properties.getMaxCorrelationIdLength());
        if (correlationId == null) correlationId = requestId;
        String path = safePath(request.getRequestURI());
        var context =
                new RequestAuditContext(
                        requestId,
                        correlationId,
                        ip(request),
                        userAgent(request),
                        bounded(request.getMethod(), 16),
                        path,
                        "HTTP",
                        LocalDateTime.now(clock));
        RequestAuditContextHolder.set(context);
        response.setHeader(properties.getRequestIdHeader(), requestId);
        response.setHeader(properties.getCorrelationIdHeader(), correlationId);
        MDC.put("requestId", requestId);
        MDC.put("correlationId", correlationId);
        MDC.put("httpMethod", context.httpMethod());
        MDC.put("requestPath", path);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
            RequestAuditContextHolder.clear();
        }
    }

    private String validId(String value, int max) {
        if (value != null) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= max && SAFE_ID.matcher(trimmed).matches())
                return trimmed;
        }
        return null;
    }

    private String userAgent(HttpServletRequest request) {
        if (!properties.isCaptureUserAgent()) return null;
        return bounded(clean(request.getHeader("User-Agent")), properties.getMaxUserAgentLength());
    }

    private String ip(HttpServletRequest request) {
        if (!properties.isCaptureIp()) return null;
        String candidate = request.getRemoteAddr();
        if (properties.isTrustForwardedHeaders()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null) candidate = forwarded.split(",", 2)[0].trim();
            else {
                String standard = request.getHeader("Forwarded");
                if (standard != null) candidate = forwardedFor(standard);
            }
        }
        try {
            InetAddress address = InetAddress.getByName(candidate);
            return properties.isMaskIp() ? mask(address) : address.getHostAddress();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String forwardedFor(String header) {
        for (String part : header.split(";")) {
            String value = part.trim();
            if (value.regionMatches(true, 0, "for=", 0, 4)) {
                value = value.substring(4).replace("\"", "");
                if (value.startsWith("[") && value.contains("]"))
                    return value.substring(1, value.indexOf(']'));
                int colon = value.lastIndexOf(':');
                return colon > 0 && value.indexOf(':') == colon ? value.substring(0, colon) : value;
            }
        }
        return "";
    }

    private String mask(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) bytes[3] = 0;
        else for (int i = 8; i < bytes.length; i++) bytes[i] = 0;
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (Exception impossible) {
            return null;
        }
    }

    private String safePath(String value) {
        String path = value == null ? null : value.split(";", 2)[0];
        return bounded(clean(path), 500);
    }

    private String clean(String value) {
        return value == null ? null : value.replaceAll("[\\p{Cntrl}]", "").trim();
    }

    private String bounded(String value, int max) {
        return value == null ? null : value.substring(0, Math.min(value.length(), max));
    }
}
