package net.lenni0451.classtransform.expression;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A token of an expression.
 */
@ParametersAreNonnullByDefault
public class ExpressionToken {

    public final ExpressionTokenType type;
    public final String text;
    public final int start;
    public final int end;

    public ExpressionToken(final ExpressionTokenType type, final String text, final int start, final int end) {
        this.type = type;
        this.text = text;
        this.start = start;
        this.end = end;
    }

    public boolean is(final ExpressionTokenType type) {
        return this.type == type;
    }

    @Override
    public String toString() {
        return this.type + "('" + this.text + "')";
    }


    public enum ExpressionTokenType {
        IDENTIFIER, INT, DECIMAL, STRING, BOOLEAN, NULL,
        NEW, INSTANCEOF, RETURN, THROW, THIS, SUPER, CLASS,
        WILDCARD, AT,
        PLUS, MINUS, MULT, DIV, MOD, BITWISE_NOT,
        DOT, COMMA, LEFT_PAREN, RIGHT_PAREN,
        LEFT_BRACKET, RIGHT_BRACKET, LEFT_BRACE, RIGHT_BRACE,
        SHL, SHR, USHR,
        LT, LE, GT, GE, EQ, NE,
        BITWISE_AND, BITWISE_XOR, BITWISE_OR,
        ASSIGN, METHOD_REF, INCREMENT, DECREMENT,
        EOF
    }

}
