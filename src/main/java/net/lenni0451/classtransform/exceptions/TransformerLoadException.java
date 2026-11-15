package net.lenni0451.classtransform.exceptions;

/**
 * An exception which is thrown when a transformer class could not be loaded or initialized.
 */
public class TransformerLoadException extends RuntimeException {

    /**
     * Create an exception for a transformer which failed to load.
     *
     * @param name  The name of the transformer which failed
     * @param cause The underlying failure
     */
    public TransformerLoadException(final String name, final Throwable cause) {
        super("Failed to load transformer: " + name, cause);
    }

}
