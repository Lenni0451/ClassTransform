package net.lenni0451.classtransform.utils.log;

import net.lenni0451.classtransform.utils.log.impl.SysoutLogger;

import java.util.Arrays;

public class Logger {

    public static final String NAME = "ClassTransform";
    public static ILogger LOGGER = new SysoutLogger();

    public static void info(final String message, final Object... args) {
        MessageArgs messageArgs = resolve(args);
        LOGGER.info(message, messageArgs.args, messageArgs.exception);
    }

    public static void warn(final String message, final Object... args) {
        MessageArgs messageArgs = resolve(args);
        LOGGER.warn(message, messageArgs.args, messageArgs.exception);
    }

    public static void error(final String message, final Object... args) {
        MessageArgs messageArgs = resolve(args);
        LOGGER.error(message, messageArgs.args, messageArgs.exception);
    }


    private static MessageArgs resolve(final Object[] args) {
        final MessageArgs messageArgs = new MessageArgs();
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            messageArgs.exception = (Throwable) args[args.length - 1];
            messageArgs.args = Arrays.copyOfRange(args, 0, args.length - 1);
        } else {
            messageArgs.args = args;
        }
        return messageArgs;
    }


    private static class MessageArgs {
        private Object[] args;
        private Throwable exception;
    }

}
