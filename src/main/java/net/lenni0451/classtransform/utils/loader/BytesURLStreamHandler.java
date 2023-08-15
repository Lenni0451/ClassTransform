package net.lenni0451.classtransform.utils.loader;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Util class to assist with in memory resources.
 */
class BytesURLStreamHandler extends URLStreamHandler {

    private final byte[] bytes;

    BytesURLStreamHandler(final byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    protected URLConnection openConnection(final URL url) {
        return new BytesURLConnection(url, this.bytes);
    }

}
