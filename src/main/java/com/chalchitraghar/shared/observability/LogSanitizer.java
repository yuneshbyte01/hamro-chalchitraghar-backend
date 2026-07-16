package com.chalchitraghar.shared.observability;

/** Prevents external text from forging log lines or creating unbounded log fields. */
public final class LogSanitizer {
    private static final int MAX_LENGTH = 300;

    private LogSanitizer() {}

    public static String safe(String value) {
        if (value == null) return "";
        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), MAX_LENGTH));
        for (int i = 0; i < value.length() && sanitized.length() < MAX_LENGTH; i++) {
            char character = value.charAt(i);
            sanitized.append(Character.isISOControl(character) ? ' ' : character);
        }
        return sanitized.toString();
    }
}
