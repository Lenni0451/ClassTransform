package net.lenni0451.classtransform.utils.log;

import net.lenni0451.classtransform.utils.log.impl.SysoutLogger;

import java.util.Arrays;

public class Logger {

    public static final String NAME = "ClassTransform";
    public static ILogger LOGGER = new SysoutLogger();

    public static void info(final String message, final Object... args) {
        LOGGER.info(message, args);
    }

    public static void warn(final String message, final Object... args) {
        LOGGER.warn(message, args);
    }

    public static void error(final String message, final Object... args) {
        LOGGER.error(message, args);
    }


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


    public static class MessageArgs {
        private Object[] args;
        private Throwable exception;

        public Object[] getArgs() {
            return this.args;
        }

        public Throwable getException() {
            return this.exception;
        }

        public boolean hasException() {
            return this.exception != null;
        }
    }

}
