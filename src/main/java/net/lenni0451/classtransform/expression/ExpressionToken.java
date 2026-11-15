package net.lenni0451.classtransform.expression;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A single token of an expression string produced by {@link ExpressionLexer}.
 * The token keeps its text and position so parser errors and {@link ExpressionSource} ranges can point into the
 * original expression.
 */
@ParametersAreNonnullByDefault
public class ExpressionToken {

    /**
     * The kind of this token.
     */
    public final ExpressionTokenType type;
    /**
     * The text of the token.
     * For {@link ExpressionTokenType#STRING} tokens this is the unescaped value without the enclosing quotes;
     * for all other tokens it is the exact source text.
     */
    public final String text;
    /**
     * The inclusive index of the first character in the source expression.
     */
    public final int start;
    /**
     * The inclusive index of the last character in the source expression.
     * For an {@link ExpressionTokenType#EOF} token this may equal {@link #start} and point one past the last
     * character of the expression, i.e. to {@code expression.length()}.
     */
    public final int end;

    /**
     * Creates a token.
     *
     * @param type  the kind of the token
     * @param text  the token text, already unescaped for string tokens
     * @param start the inclusive start index in the source expression
     * @param end   the inclusive end index in the source expression
     */
    public ExpressionToken(final ExpressionTokenType type, final String text, final int start, final int end) {
        this.type = type;
        this.text = text;
        this.start = start;
        this.end = end;
    }

    /**
     * Tests if this token has the given type.
     *
     * @param type the type to compare against
     * @return whether the token types are identical
     */
    public boolean is(final ExpressionTokenType type) {
        return this.type == type;
    }

    @Override
    public String toString() {
        return this.type + "('" + this.text + "')";
    }


    /**
     * The kinds of tokens produced by {@link ExpressionLexer}.
     */
    public enum ExpressionTokenType {
        /**
         * An identifier naming a member, type or local variable.
         */
        IDENTIFIER,
        /**
         * An integer literal, either decimal or hexadecimal ({@code 0x...}).
         */
        INT,
        /**
         * A floating-point literal containing a decimal point.
         */
        DECIMAL,
        /**
         * A single-quoted string literal.
         */
        STRING,
        /**
         * The {@code true} or {@code false} literal.
         */
        BOOLEAN,
        /**
         * The {@code null} literal.
         */
        NULL,
        /**
         * The {@code new} keyword introducing an instantiation or array creation.
         */
        NEW,
        /**
         * The {@code instanceof} keyword.
         */
        INSTANCEOF,
        /**
         * The {@code return} keyword starting a return statement.
         */
        RETURN,
        /**
         * The {@code throw} keyword starting a throw statement.
         */
        THROW,
        /**
         * The {@code this} keyword.
         */
        THIS,
        /**
         * The {@code super} keyword.
         */
        SUPER,
        /**
         * The {@code class} keyword of a class constant such as {@code String.class}.
         */
        CLASS,
        /**
         * The {@code ?} wildcard matching any value or name.
         */
        WILDCARD,
        /**
         * The {@code @} introducing a capturing expression such as {@code @(?)}.
         */
        AT,
        /**
         * The {@code +} operator, also used for string concatenation.
         */
        PLUS,
        /**
         * The {@code -} operator for subtraction and negation.
         */
        MINUS,
        /**
         * The {@code *} multiplication operator.
         */
        MULT,
        /**
         * The {@code /} division operator.
         */
        DIV,
        /**
         * The {@code %} remainder operator.
         */
        MOD,
        /**
         * The {@code ~} bitwise complement operator.
         */
        BITWISE_NOT,
        /**
         * The {@code .} member access separator.
         */
        DOT,
        /**
         * The {@code ,} argument and array element separator.
         */
        COMMA,
        /**
         * An opening parenthesis.
         */
        LEFT_PAREN,
        /**
         * A closing parenthesis.
         */
        RIGHT_PAREN,
        /**
         * An opening square bracket.
         */
        LEFT_BRACKET,
        /**
         * A closing square bracket.
         */
        RIGHT_BRACKET,
        /**
         * An opening brace starting an array literal.
         */
        LEFT_BRACE,
        /**
         * A closing brace ending an array literal.
         */
        RIGHT_BRACE,
        /**
         * The {@code <<} left shift operator.
         */
        SHL,
        /**
         * The {@code >>} arithmetic right shift operator.
         */
        SHR,
        /**
         * The {@code >>>} unsigned right shift operator.
         */
        USHR,
        /**
         * The {@code <} less-than operator.
         */
        LT,
        /**
         * The {@code <=} less-than-or-equal operator.
         */
        LE,
        /**
         * The {@code >} greater-than operator.
         */
        GT,
        /**
         * The {@code >=} greater-than-or-equal operator.
         */
        GE,
        /**
         * The {@code ==} equality operator.
         */
        EQ,
        /**
         * The {@code !=} inequality operator.
         */
        NE,
        /**
         * The {@code &} bitwise and operator.
         */
        BITWISE_AND,
        /**
         * The {@code ^} bitwise xor operator.
         */
        BITWISE_XOR,
        /**
         * The {@code |} bitwise or operator.
         */
        BITWISE_OR,
        /**
         * The {@code =} assignment operator.
         */
        ASSIGN,
        /**
         * The {@code ::} method reference operator.
         */
        METHOD_REF,
        /**
         * The {@code ++} increment operator.
         */
        INCREMENT,
        /**
         * The {@code --} decrement operator.
         */
        DECREMENT,
        /**
         * The end of the input; always the last token produced by the lexer.
         */
        EOF
    }

}
