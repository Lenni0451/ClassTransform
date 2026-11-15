package net.lenni0451.classtransform.utils.log.impl;

import net.lenni0451.classtransform.utils.log.ILogger;
import net.lenni0451.classtransform.utils.log.Logger.MessageArgs;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lenni0451.classtransform.utils.log.Logger.resolve;

/**
 * Logger implementation that prints to {@link System#out} and {@link System#err}.
 *
 * <p>Informational messages go to the standard output stream, warnings and errors to the standard error stream. Every
 * line is prefixed with the level in brackets, and a trailing {@link Throwable} is printed as a stack trace to the same
 * stream. The streams are looked up on each call, so replacing {@code System.out} or {@code System.err} is honored.
 */
@ParametersAreNonnullByDefault
public class SysoutLogger implements ILogger {

    private static final Pattern ARG_PATTERN = Pattern.compile("\\{}");

    @Override
    public void info(String message, Object... args) {
        MessageArgs messageArgs = resolve(args);
        System.out.println(this.format("INFO ", message, messageArgs.getArgs()));
        if (messageArgs.hasException()) messageArgs.getException().printStackTrace(System.out);
    }

    @Override
    public void warn(String message, Object... args) {
        MessageArgs messageArgs = resolve(args);
        System.err.println(this.format("WARN ", message, messageArgs.getArgs()));
        if (messageArgs.hasException()) messageArgs.getException().printStackTrace(System.err);
    }

    @Override
    public void error(String message, Object... args) {
        MessageArgs messageArgs = resolve(args);
        System.err.println(this.format("ERROR", message, messageArgs.getArgs()));
        if (messageArgs.hasException()) messageArgs.getException().printStackTrace(System.err);
    }

    /**
     * Formats a message for output, substituting {@code {}} placeholders with the given arguments.
     *
     * <p>Only as many placeholders are replaced as there are arguments; a remaining {@code {}} stays in the text.
     * Arguments must not be {@code null}, as they are converted with {@link Object#toString()}.
     *
     * @param level   The level prefix to put in brackets
     * @param message The message to format
     * @param args    The arguments for the placeholders
     * @return The formatted line, without a trailing line separator
     */
    protected String format(final String level, final String message, final Object[] args) {
        StringBuffer builder = new StringBuffer();
        builder.append("[").append(level).append("] ");

        Matcher matcher = ARG_PATTERN.matcher(message);
        int i = 0;
        while (matcher.find()) {
            if (i >= args.length) break;
            matcher.appendReplacement(builder, args[i].toString().replace("$", "\\$"));
            i++;
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

}
