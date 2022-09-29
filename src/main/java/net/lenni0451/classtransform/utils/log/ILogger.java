package net.lenni0451.classtransform.utils.log;

import java.util.Arrays;

public interface ILogger {

    void info(final String message, final Object... args);

    void warn(final String message, final Object... args);

    void error(final String message, final Object... args);


    default String format(final String message, Object... args) {
        if (args.length == 0) return message;
        if (args[args.length - 1] instanceof Throwable) args = Arrays.copyOfRange(args, 0, args.length - 1);
        if (args.length == 0) return message;
        return String.format(message, args);
    }

}
