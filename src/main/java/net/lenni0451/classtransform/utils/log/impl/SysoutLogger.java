package net.lenni0451.classtransform.utils.log.impl;

import net.lenni0451.classtransform.utils.log.ILogger;
import net.lenni0451.classtransform.utils.log.Logger.MessageArgs;

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lenni0451.classtransform.utils.log.Logger.resolve;

@ParametersAreNonnullByDefault
public class SysoutLogger implements ILogger {

    private static final Pattern ARG_PATTERN = Pattern.compile("\\{}");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

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

    protected String format(final String level, final String message, final Object[] args) {
        StringBuffer builder = new StringBuffer();
        builder.append("[").append(DATE_FORMAT.format(System.currentTimeMillis())).append("] ");
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
