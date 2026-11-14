package net.lenni0451.classtransform.expression;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A part of an expression string.
 */
@ParametersAreNonnullByDefault
public class ExpressionSource {

    public final String expression;
    public final int start;
    public final int end;

    public ExpressionSource(final String expression, final int start, final int end) {
        this.expression = expression;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return this.expression.substring(this.start, Math.min(this.end + 1, this.expression.length()));
    }

}
