package net.lenni0451.classtransform.utils.log;

import java.util.Arrays;

/**
 * The logger interface used by ClassTransform.<br>
 * I did not choose any other logging framework because I don't want to force the user to use a specific one.
 */
public interface ILogger {

    /**
     * Print an info message.
     *
     * @param message The message
     * @param args    The arguments
     */
    void info(final String message, final Object... args);

    /**
     * Print a warning message.
     *
     * @param message The message
     * @param args    The arguments
     */
    void warn(final String message, final Object... args);

    /**
     * Print an error message.
     *
     * @param message The message
     * @param args    The arguments
     */
    void error(final String message, final Object... args);


    /**
     * Format a message with the given arguments.<br>
     * Trailing exceptions are skipped.
     *
     * @param message The message
     * @param args    The arguments
     * @return The formatted message
     */
    default String format(final String message, Object... args) {
        if (args.length == 0) return message;
        if (args[args.length - 1] instanceof Throwable) args = Arrays.copyOfRange(args, 0, args.length - 1);
        if (args.length == 0) return message;
        return String.format(message, args);
    }

}
