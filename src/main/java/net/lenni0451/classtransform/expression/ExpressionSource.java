package net.lenni0451.classtransform.expression;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A range of characters in an expression string.
 * Every {@link ExpressionAst} node keeps the range it was parsed from. The range is currently metadata only;
 * no code reads it to produce error messages or debug output.
 */
@ParametersAreNonnullByDefault
public class ExpressionSource {

    /**
     * The full expression string containing the range.
     */
    public final String expression;
    /**
     * The inclusive index of the first character of the range.
     */
    public final int start;
    /**
     * The inclusive index of the last character of the range.
     */
    public final int end;

    /**
     * Creates a new source range.
     *
     * @param expression the full expression string
     * @param start      the inclusive start index
     * @param end        the inclusive end index
     */
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
