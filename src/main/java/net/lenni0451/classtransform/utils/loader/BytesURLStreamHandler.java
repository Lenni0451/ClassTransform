package net.lenni0451.classtransform.utils.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

class BytesURLStreamHandler extends URLStreamHandler {

    private final byte[] bytes;

    BytesURLStreamHandler(final byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new BytesURLConnection(url, bytes);
    }

}
