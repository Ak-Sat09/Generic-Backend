package com.buytogether.common.text;

import java.util.Locale;

public final class Text {

    private Text() {
    }

    /** Trims and collapses inner whitespace: "  Tata   Nexon " becomes "Tata Nexon". */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    /** Lower case form used for exact, case insensitive matching in queries. */
    public static String key(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.toLowerCase(Locale.ROOT);
    }
}
