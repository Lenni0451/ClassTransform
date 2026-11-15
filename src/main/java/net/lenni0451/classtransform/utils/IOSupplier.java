package net.lenni0451.classtransform.utils;

import java.io.IOException;

/**
 * A supplier whose {@link #get()} method may throw an {@link IOException}.
 *
 * @param <T> The supplied type
 */
@FunctionalInterface
public interface IOSupplier<T> {

    /**
     * Get the value.
     *
     * @return The value
     * @throws IOException If the value could not be supplied
     */
    T get() throws IOException;

}
