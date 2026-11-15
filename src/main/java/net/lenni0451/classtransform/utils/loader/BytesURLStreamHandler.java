package net.lenni0451.classtransform.utils.loader;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Stream handler creating {@code x-buffer} URLs whose content is held in memory.
 * <p>
 * The handler is attached to the URL instance only. Such URLs cannot be reconstructed from their string
 * form because the protocol is not registered with the JVM.
 */
class BytesURLStreamHandler extends URLStreamHandler {

    static URL createURL(final String name, final byte[] bytes) {
        try {
            return new URL("x-buffer", null, -1, name, new BytesURLStreamHandler(bytes));
        } catch (MalformedURLException e) {
            throw new RuntimeException("This should never have happened", e);
        }
    }


    private final byte[] bytes;

    private BytesURLStreamHandler(final byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    protected URLConnection openConnection(final URL url) {
        return new BytesURLConnection(url, this.bytes);
    }

}
