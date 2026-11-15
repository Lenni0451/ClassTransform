package net.lenni0451.classtransform.utils.log;

import net.lenni0451.classtransform.utils.log.impl.SysoutLogger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

/**
 * Static facade for all logging of ClassTransform.
 *
 * <p>Messages are forwarded unchanged to the mutable {@link #LOGGER} field, which defaults to a {@link SysoutLogger}.
 * Replacing it with another {@link ILogger} redirects or silences the output of the whole library. The field is not
 * volatile and its assignment is not synchronized, so a logger should be installed during setup, before classes are
 * transformed from other threads. The facade itself holds no further state.
 */
@ParametersAreNonnullByDefault
public class Logger {

    /**
     * The name of the library for use in log output.
     */
    public static final String NAME = "ClassTransform";
    /**
     * The logger that receives all messages, a {@link SysoutLogger} by default.
     */
    public static ILogger LOGGER = new SysoutLogger();

    /**
     * Logs an informational message through {@link #LOGGER}.
     *
     * @param message The message, optionally containing {@code {}} placeholders
     * @param args    The arguments for the placeholders, optionally followed by a throwable
     */
    public static void info(final String message, final Object... args) {
        LOGGER.info(message, args);
    }

    /**
     * Logs a warning through {@link #LOGGER}.
     *
     * @param message The message, optionally containing {@code {}} placeholders
     * @param args    The arguments for the placeholders, optionally followed by a throwable
     */
    public static void warn(final String message, final Object... args) {
        LOGGER.warn(message, args);
    }

    /**
     * Logs an error through {@link #LOGGER}.
     *
     * @param message The message, optionally containing {@code {}} placeholders
     * @param args    The arguments for the placeholders, optionally followed by a throwable
     */
    public static void error(final String message, final Object... args) {
        LOGGER.error(message, args);
    }


    /**
     * Splits a trailing throwable off the arguments of a log message.
     *
     * <p>If the last element of the array is a {@link Throwable}, it becomes the exception of the result and the
     * remaining elements are copied into a new array. Otherwise the given array is stored as-is, without copying.
     *
     * @param args The arguments passed to a log method
     * @return The resolved format arguments and exception, never {@code null}
     */
    public static MessageArgs resolve(final Object[] args) {
        final MessageArgs messageArgs = new MessageArgs();
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            messageArgs.exception = (Throwable) args[args.length - 1];
            messageArgs.args = Arrays.copyOfRange(args, 0, args.length - 1);
        } else {
            messageArgs.args = args;
        }
        return messageArgs;
    }


    /**
     * Holds the format arguments and optional throwable produced by {@link Logger#resolve(Object[])}.
     */
    public static class MessageArgs {
        private Object[] args;
        private Throwable exception;

        /**
         * Gets the format arguments without the trailing throwable.
         *
         * <p>The returned array is the internal array, not a copy, so changes to it affect this instance.
         *
         * @return The format arguments, empty if the message had none
         */
        public Object[] getArgs() {
            return this.args;
        }

        /**
         * Gets the throwable that was passed as the last argument.
         *
         * @return The throwable or {@code null} if none was supplied
         */
        public Throwable getException() {
            return this.exception;
        }

        /**
         * Checks whether a throwable was supplied with the message.
         *
         * @return If a throwable is present
         */
        public boolean hasException() {
            return this.exception != null;
        }
    }

}
