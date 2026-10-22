package com.materialistic;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Money {
    public static final long SCALE = 10_000L;

    private Money() {
    }

    public static long units(double worth, int count) {
        return Math.max(0L, Math.round(worth * count * SCALE));
    }

    public static String format(long units) {
        if (units <= 0L) {
            return "0.00";
        }

        return BigDecimal.valueOf(units, 4)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    public static String formatShort(long units) {
        return String.format(Locale.ROOT, "%,.2f", units / (double) SCALE);
    }

    public static String currency(long units) {
        return formatShort(units) + "c";
    }
}
