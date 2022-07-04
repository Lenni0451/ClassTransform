package net.lenni0451.classtransform.utils.loader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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
        return new ByteArrayInputStream(data);
    }

}
