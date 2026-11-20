package com.ecommerce.app.util;

/** Neutralises CR/LF and other control characters so user input cannot forge log lines. */
public final class LogSafe {
    private LogSafe() {}

    public static String clean(String s) {
        if (s == null) return null;
        String out = s.replaceAll("[\\p{Cntrl}]", "_");
        return out.length() > 100 ? out.substring(0, 100) : out;
    }
}
