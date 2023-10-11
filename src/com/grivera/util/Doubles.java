package com.grivera.util;

public class Doubles {
    public static long floorRound(double d) {
        // long rounded = Math.round(d);
        // if (Math.abs(rounded - d) < 1e-3) {
        //     return rounded;
        // }
        // return (long) Math.floor(d);
        return Math.round(d);
    }
}
