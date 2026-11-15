package net.lenni0451.classtransform.expression;

/**
 * A node of the parsed expression tree.
 * Expressions are produced by {@link ExpressionParser} and matched against a single
 * {@link ExpressionFlow.FlowValue} of the analyzed target method. Matching happens in two steps:
 * {@link #matches(ExpressionFlow.FlowValue, ExpressionContext)} decides if the value fits, and
 * {@link #capture(ExpressionFlow.FlowValue, ExpressionContext)} then records the matched instructions in the
 * context so that {@link ExpressionEngine} can turn them into an {@link ExpressionMatch}.
 * Implementations are immutable and only inspect the value graph; all match state lives in the context.
 */
public interface Expression {

    /**
     * Tests if the given value satisfies this expression.
     * <p>
     * A successful match does not record anything; {@link #capture(ExpressionFlow.FlowValue, ExpressionContext)}
     * has to be called afterwards. Most implementations reject {@linkplain ExpressionFlow.FlowValue#isComplex()
     * complex} values because a value merged from multiple branches has no single instruction to match.
     *
     * @param node the value to match
     * @param ctx  the context of the current match
     * @return whether the value satisfies this expression
     */
    boolean matches(ExpressionFlow.FlowValue node, ExpressionContext ctx);

    /**
     * Records the instructions matched by this expression in the given context.
     * <p>
     * Called after a successful {@link #matches(ExpressionFlow.FlowValue, ExpressionContext)}. One node may add
     * zero or more {@link ExpressionContext.Capture}s; complex values are ignored by the context.
     *
     * @param node the matched value
     * @param ctx  the context to add the captures to
     */
    void capture(ExpressionFlow.FlowValue node, ExpressionContext ctx);

    /**
     * Returns whether this expression is a constant zero literal.
     * <p>
     * Used when matching implicit comparisons like {@code ? != 0}, where the zero is encoded in the jump
     * instruction instead of a separate literal node. The default is {@code false}.
     *
     * @return whether this expression represents the constant zero
     */
    default boolean matchesZero() {
        return false;
    }

    /**
     * Returns whether this expression is the {@code null} literal.
     * <p>
     * Used when matching implicit comparisons like {@code ? != null}, where the check is encoded in an
     * {@code IFNULL}/{@code IFNONNULL} instruction. The default is {@code false}.
     *
     * @return whether this expression represents {@code null}
     */
    default boolean matchesNull() {
        return false;
    }

}
