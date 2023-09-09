package net.lenni0451.classtransform.exceptions;

public class TransformerLoadException extends RuntimeException {

    public TransformerLoadException(final String name, final Throwable cause) {
        super("Failed to load transformer: " + name, cause);
    }

}
