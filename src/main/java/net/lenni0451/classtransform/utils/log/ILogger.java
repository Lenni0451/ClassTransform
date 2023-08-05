package net.lenni0451.classtransform.utils.log;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface ILogger {

    void info(final String message, final Object... args);

    void warn(final String message, final Object... args);

    void error(final String message, final Object... args);

}
