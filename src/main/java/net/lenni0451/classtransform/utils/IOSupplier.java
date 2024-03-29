package net.lenni0451.classtransform.utils;

import java.io.IOException;

@FunctionalInterface
public interface IOSupplier<T> {

    T get() throws IOException;

}
