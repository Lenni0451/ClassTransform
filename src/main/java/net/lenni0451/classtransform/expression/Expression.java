package net.lenni0451.classtransform.expression;

/**
 * A node of a parsed expression.
 */
public interface Expression {

    boolean matches(ExpressionFlow.FlowValue node, ExpressionContext ctx);

    void capture(ExpressionFlow.FlowValue node, ExpressionContext ctx);

    default boolean matchesZero() {
        return false;
    }

    default boolean matchesNull() {
        return false;
    }

}
