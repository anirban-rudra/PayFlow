package com.paypal.transaction_service.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class PayTagUtil {
    private static final Pattern PAY_TAG_PATTERN = Pattern.compile("^@[a-z0-9][a-z0-9._-]{2,29}$");

    private PayTagUtil() {
    }

    public static String normalize(String payTag) {
        if (payTag == null) {
            return null;
        }
        return payTag.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String payTag) {
        String normalized = normalize(payTag);
        return normalized != null && PAY_TAG_PATTERN.matcher(normalized).matches();
    }
}
