package com.chalchitraghar.shared.audit;

import java.util.*;

public final class RequestAuditContextHolder {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private RequestAuditContextHolder() {}

    public static void set(RequestAuditContext context) {
        if (CURRENT.get() != null)
            throw new IllegalStateException("Request audit context is already set");
        CURRENT.set(new State(context));
    }

    public static Optional<RequestAuditContext> current() {
        State state = CURRENT.get();
        return state == null ? Optional.empty() : Optional.of(state.context);
    }

    public static boolean markSecurityAudit(String key) {
        State state = CURRENT.get();
        return state != null && state.securityAudits.add(key);
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static final class State {
        private final RequestAuditContext context;
        private final Set<String> securityAudits = new HashSet<>();

        private State(RequestAuditContext context) {
            this.context = context;
        }
    }
}
