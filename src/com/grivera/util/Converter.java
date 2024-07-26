package com.grivera.util;

public class Converter {
    public static double microJoulesToCents(double microJoules) {
        return microJoules / 10_000_000.0 * 2;
    }
}
