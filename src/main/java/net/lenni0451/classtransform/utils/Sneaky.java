package net.lenni0451.classtransform.utils;

import java.util.function.Function;
import java.util.function.Supplier;

public class Sneaky {

    public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public static <T> Supplier<T> sneakySupply(final ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable t) {
                sneakyThrow(t);
                return null;
            }
        };
    }

    public static <T, R> Function<T, R> sneakyFunction(final ThrowingFunction<T, R> function) {
        return (t) -> {
            try {
                return function.apply(t);
            } catch (Throwable e) {
                sneakyThrow(e);
                return null;
            }
        };
    }


    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Throwable;
    }

}
