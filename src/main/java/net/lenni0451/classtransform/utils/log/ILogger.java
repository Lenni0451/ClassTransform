package net.lenni0451.classtransform.utils.log;

public interface ILogger {

    void info(final String message, final Object... args);

    void warn(final String message, final Object... args);

    void error(final String message, final Object... args);

}
