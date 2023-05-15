package net.lenni0451.classtransform.utils.log;

public interface ILogger {

    void info(final String message, final Object[] args, final Throwable exception);

    void warn(final String message, final Object[] args, final Throwable exception);

    void error(final String message, final Object[] args, final Throwable exception);

}
