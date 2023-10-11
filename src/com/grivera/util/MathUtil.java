package com.grivera.util;

public class MathUtil {
    public static long max(long ... longs) {
        if (longs == null) {
            throw new IllegalArgumentException("Cannot be null");
        }
        long max = longs[0];
        for (long l : longs) {
            max = Math.max(max, l);
        }
        return max;
    }

    public static long min(long ... longs) {
        if (longs == null) {
            throw new IllegalArgumentException("Cannot be null");
        }
        long min = longs[0];
        for (long l : longs) {
            min = Math.min(min, l);
        }
        return min;
    }
}
