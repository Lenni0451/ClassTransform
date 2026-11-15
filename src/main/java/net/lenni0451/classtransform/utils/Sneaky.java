package net.lenni0451.classtransform.utils;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utilities for throwing checked exceptions without declaring them.
 */
public class Sneaky {

    /**
     * Throw a checked throwable without declaring it.
     *
     * @param <T> The declared exception type
     * @param t   The throwable to throw
     * @throws T Always thrown
     */
    public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    /**
     * Wrap a throwing supplier in a supplier which rethrows checked exceptions.
     *
     * @param <T>      The supplied type
     * @param supplier The throwing supplier
     * @return The wrapped supplier
     */
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

    /**
     * Wrap a throwing function in a function which rethrows checked exceptions.
     *
     * @param <T>      The input type
     * @param <R>      The result type
     * @param function The throwing function
     * @return The wrapped function
     */
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


    /**
     * A supplier whose {@link #get()} method may throw any throwable.
     *
     * @param <T> The supplied type
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        /**
         * Get the value.
         *
         * @return The value
         * @throws Throwable If the value could not be supplied
         */
        T get() throws Throwable;
    }

    /**
     * A function whose {@link #apply} method may throw any throwable.
     *
     * @param <T> The input type
     * @param <R> The result type
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        /**
         * Apply the function.
         *
         * @param t The input value
         * @return The result
         * @throws Throwable If the function failed
         */
        R apply(T t) throws Throwable;
    }

}
