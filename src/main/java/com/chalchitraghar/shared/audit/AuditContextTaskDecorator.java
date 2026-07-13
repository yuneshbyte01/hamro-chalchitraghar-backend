package com.chalchitraghar.shared.audit;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class AuditContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        RequestAuditContext context = RequestAuditContextHolder.current().orElse(null);
        return () -> {
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();
            RequestAuditContext previous = RequestAuditContextHolder.current().orElse(null);
            try {
                if (mdc != null) MDC.setContextMap(mdc);
                if (context != null) RequestAuditContextHolder.set(context);
                task.run();
            } finally {
                MDC.clear();
                RequestAuditContextHolder.clear();
                if (previousMdc != null) MDC.setContextMap(previousMdc);
                if (previous != null) RequestAuditContextHolder.set(previous);
            }
        };
    }
}
