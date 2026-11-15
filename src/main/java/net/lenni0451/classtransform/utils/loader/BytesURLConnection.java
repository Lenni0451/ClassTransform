package net.lenni0451.classtransform.utils.loader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * URL connection exposing an in-memory byte array through the {@code x-buffer} protocol.
 * <p>
 * Connecting is a no-op and each call to {@link #getInputStream()} returns a new stream over the same
 * array. The array is not copied, so callers must not modify it while the URL is in use.
 */
class BytesURLConnection extends URLConnection {

    private final byte[] data;

    BytesURLConnection(final URL url, final byte[] data) {
        super(url);

        this.data = data;
    }

    @Override
    public void connect() {
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.data);
    }

}
