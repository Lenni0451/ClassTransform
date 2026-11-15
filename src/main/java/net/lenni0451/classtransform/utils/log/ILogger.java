package net.lenni0451.classtransform.utils.log;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Sink for the log messages of ClassTransform.
 *
 * <p>Messages may contain {@code {}} placeholders, which implementations replace with the supplied arguments in order.
 * The last argument may be a {@link Throwable} that belongs to the message; use {@link Logger#resolve(Object[])} to
 * separate it from the format arguments.
 *
 * <p>Implementations should be thread-safe, because ClassTransform can log from several threads at once.
 */
@ParametersAreNonnullByDefault
public interface ILogger {

    /**
     * Logs an informational message.
     *
     * @param message The message, optionally containing {@code {}} placeholders
     * @param args    The arguments for the placeholders, optionally followed by a throwable
     */
    void info(final String message, final Object... args);

    /**
     * Logs a warning.
     *
     * @param message The message, optionally containing {@code {}} placeholders
     * @param args    The arguments for the placeholders, optionally followed by a throwable
     */
    void warn(final String message, final Object... args);

    /**
     * Logs an error.
     *
     * @param message The message, optionally containing {@code {}} placeholders
     * @param args    The arguments for the placeholders, optionally followed by a throwable
     */
    void error(final String message, final Object... args);

}
