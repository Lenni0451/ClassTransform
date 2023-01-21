package net.lenni0451.classtransform.utils.loader;

import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * Util class to assist with in memory resources.
 */
class URLEnumeration implements Enumeration<URL> {

    private final URL[] values;

    private int index = 0;

    URLEnumeration(final URL... values) {
        this.values = values;
    }

    @Override
    public boolean hasMoreElements() {
        return this.index < this.values.length;
    }

    @Override
    public URL nextElement() {
        if (!this.hasMoreElements()) throw new NoSuchElementException();
        return this.values[this.index++];
    }

}
