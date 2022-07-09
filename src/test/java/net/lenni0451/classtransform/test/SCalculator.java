package net.lenni0451.classtransform.test;

import java.util.Random;

public class SCalculator {

    private static double pi = Math.PI;

    public static int add(final int i1, final int i2) {
        return i1 + i2;
    }

    public static int subtract(final int i1, final int i2) {
        return i1 - i2;
    }

    public static double divide(final double d1, final double d2) {
        if (d2 == 0) throw new ArithmeticException("Division by zero");
        return d1 / d2;
    }

    public static double pow2(final int i) {
        return Math.pow(i, 2);
    }

    public static int rint() {
        Random rnd = new Random();
        return rnd.nextInt();
    }

    public static double getPi() {
        return pi;
    }

    public static void setPi(final double pi) {
        SCalculator.pi = pi;
    }

}
