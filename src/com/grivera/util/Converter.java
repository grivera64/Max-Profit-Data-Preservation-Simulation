package com.grivera.util;

public class Converter {
    public static double microJoulesToCents(double microJoules) {
        return (microJoules / 100_000_000.0) * 2;
    }

    public static double centsToMicroJoules(double cents) {
        return (cents / 2) * 100_000_000.0;
    }
}
