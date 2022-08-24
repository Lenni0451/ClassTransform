package net.lenni0451.classtransform.test;

import java.util.Random;

public class VCalculator {

    private double pi = Math.PI;

    public int add(final int i1, final int i2) {
        return i1 + i2;
    }

    public int subtract(final int i1, final int i2) {
        return i1 - i2;
    }

    public double divide(final double d1, final double d2) {
        if (d2 == 0) throw new ArithmeticException("Division by zero");
        return d1 / d2;
    }

    public double pow2(final int i) {
        return Math.pow(i, 2);
    }

    public int rint() {
        Random rnd = new Random();
        return rnd.nextInt();
    }

    public int rbint(final int bound) {
        Random rnd = new Random();
        return rnd.nextInt(bound);
    }

    public double getPi() {
        return this.pi;
    }

    public void setPi(final double pi) {
        this.pi = pi;
    }

}
