package net.lenni0451.classtransform.expression;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits an expression string into {@link ExpressionToken}s.
 * The lexer understands the literals, keywords, operators and punctuation of the expression language and
 * rejects everything else. {@link #tokenize()} reads the whole input eagerly, while the internal {@code next()}
 * method advances one token at a time; a single instance is intended for one tokenization pass.
 */
@ParametersAreNonnullByDefault
public class ExpressionLexer {

    private final String expression;
    private int index;

    /**
     * Creates a lexer for the given expression.
     *
     * @param expression the expression string to tokenize
     */
    public ExpressionLexer(final String expression) {
        this.expression = expression;
    }

    /**
     * Tokenizes the whole input.
     * <p>
     * Reads from the current position to the end of the input; the returned list always ends with an
     * {@link ExpressionToken.ExpressionTokenType#EOF} token.
     *
     * @return the tokens of the expression, including the trailing end-of-input token
     * @throws IllegalArgumentException if the input contains an unknown character, an unterminated string or a
     *                                  logical operator
     */
    public List<ExpressionToken> tokenize() {
        List<ExpressionToken> tokens = new ArrayList<>();
        while (true) {
            ExpressionToken token = this.next();
            tokens.add(token);
            if (token.is(ExpressionToken.ExpressionTokenType.EOF)) break;
        }
        return tokens;
    }

    private ExpressionToken next() {
        this.skipWhitespace();
        if (this.index >= this.expression.length()) return new ExpressionToken(ExpressionToken.ExpressionTokenType.EOF, "", this.index, this.index);
        int start = this.index;
        char c = this.expression.charAt(this.index);

        if (c == '\'') return this.readString();
        if (Character.isDigit(c)) return this.readNumber();
        if (c == '.' && this.index + 1 < this.expression.length() && Character.isDigit(this.expression.charAt(this.index + 1))) return this.readNumber();
        if (Character.isJavaIdentifierStart(c)) return this.readIdentifier();

        this.index++;
        switch (c) {
            case '+': return this.operator(ExpressionToken.ExpressionTokenType.PLUS, "++", ExpressionToken.ExpressionTokenType.INCREMENT, start);
            case '-': return this.operator(ExpressionToken.ExpressionTokenType.MINUS, "--", ExpressionToken.ExpressionTokenType.DECREMENT, start);
            case '*': return this.simple(ExpressionToken.ExpressionTokenType.MULT, start);
            case '/': return this.simple(ExpressionToken.ExpressionTokenType.DIV, start);
            case '%': return this.simple(ExpressionToken.ExpressionTokenType.MOD, start);
            case '~': return this.simple(ExpressionToken.ExpressionTokenType.BITWISE_NOT, start);
            case '.': return this.simple(ExpressionToken.ExpressionTokenType.DOT, start);
            case ',': return this.simple(ExpressionToken.ExpressionTokenType.COMMA, start);
            case '(': return this.simple(ExpressionToken.ExpressionTokenType.LEFT_PAREN, start);
            case ')': return this.simple(ExpressionToken.ExpressionTokenType.RIGHT_PAREN, start);
            case '[': return this.simple(ExpressionToken.ExpressionTokenType.LEFT_BRACKET, start);
            case ']': return this.simple(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET, start);
            case '{': return this.simple(ExpressionToken.ExpressionTokenType.LEFT_BRACE, start);
            case '}': return this.simple(ExpressionToken.ExpressionTokenType.RIGHT_BRACE, start);
            case '@': return this.simple(ExpressionToken.ExpressionTokenType.AT, start);
            case '?': return this.simple(ExpressionToken.ExpressionTokenType.WILDCARD, start);
            case '=':
                if (this.match('=')) return this.token(ExpressionToken.ExpressionTokenType.EQ, start);
                return this.token(ExpressionToken.ExpressionTokenType.ASSIGN, start);
            case '!':
                if (this.match('=')) return this.token(ExpressionToken.ExpressionTokenType.NE, start);
                throw this.error("Unexpected character '!'", start);
            case '<':
                if (this.match('<')) return this.token(ExpressionToken.ExpressionTokenType.SHL, start);
                if (this.match('=')) return this.token(ExpressionToken.ExpressionTokenType.LE, start);
                return this.token(ExpressionToken.ExpressionTokenType.LT, start);
            case '>':
                if (this.match('>')) {
                    if (this.match('>')) return this.token(ExpressionToken.ExpressionTokenType.USHR, start);
                    return this.token(ExpressionToken.ExpressionTokenType.SHR, start);
                }
                if (this.match('=')) return this.token(ExpressionToken.ExpressionTokenType.GE, start);
                return this.token(ExpressionToken.ExpressionTokenType.GT, start);
            case '&':
                if (this.match('&')) throw this.error("Logical operators are not supported", start);
                return this.token(ExpressionToken.ExpressionTokenType.BITWISE_AND, start);
            case '^': return this.simple(ExpressionToken.ExpressionTokenType.BITWISE_XOR, start);
            case '|':
                if (this.match('|')) throw this.error("Logical operators are not supported", start);
                return this.simple(ExpressionToken.ExpressionTokenType.BITWISE_OR, start);
            case ':':
                if (this.match(':')) return this.token(ExpressionToken.ExpressionTokenType.METHOD_REF, start);
                throw this.error("Unexpected character ':'", start);
            default:
                throw this.error("Unexpected character '" + c + "'", start);
        }
    }

    private ExpressionToken operator(final ExpressionToken.ExpressionTokenType single, final String doubled, final ExpressionToken.ExpressionTokenType doubledType, final int start) {
        if (this.match(doubled.charAt(1))) return this.token(doubledType, start);
        return this.token(single, start);
    }

    private ExpressionToken simple(final ExpressionToken.ExpressionTokenType type, final int start) {
        return this.token(type, start);
    }

    private ExpressionToken token(final ExpressionToken.ExpressionTokenType type, final int start) {
        return new ExpressionToken(type, this.expression.substring(start, this.index), start, this.index - 1);
    }

    private boolean match(final char expected) {
        if (this.index < this.expression.length() && this.expression.charAt(this.index) == expected) {
            this.index++;
            return true;
        }
        return false;
    }

    private ExpressionToken readString() {
        int start = this.index++;
        StringBuilder value = new StringBuilder();
        while (this.index < this.expression.length()) {
            char c = this.expression.charAt(this.index++);
            if (c == '\\') {
                if (this.index >= this.expression.length()) throw this.error("Unterminated escape sequence", start);
                char escaped = this.expression.charAt(this.index++);
                if (escaped == '\'' || escaped == '\\') value.append(escaped);
                else value.append('\\').append(escaped);
            } else if (c == '\'') {
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.STRING, value.toString(), start, this.index - 1);
            } else {
                value.append(c);
            }
        }
        throw this.error("Unterminated string literal", start);
    }

    private ExpressionToken readNumber() {
        int start = this.index;
        if (this.expression.startsWith("0x", this.index)) {
            this.index += 2;
            while (this.index < this.expression.length() && Character.digit(this.expression.charAt(this.index), 16) >= 0) this.index++;
            return new ExpressionToken(ExpressionToken.ExpressionTokenType.INT, this.expression.substring(start, this.index), start, this.index - 1);
        }
        boolean decimal = false;
        while (this.index < this.expression.length()) {
            char c = this.expression.charAt(this.index);
            if (Character.isDigit(c)) {
                this.index++;
            } else if (c == '.' && !decimal) {
                decimal = true;
                this.index++;
            } else {
                break;
            }
        }
        ExpressionToken.ExpressionTokenType type = decimal ? ExpressionToken.ExpressionTokenType.DECIMAL : ExpressionToken.ExpressionTokenType.INT;
        return new ExpressionToken(type, this.expression.substring(start, this.index), start, this.index - 1);
    }

    private ExpressionToken readIdentifier() {
        int start = this.index++;
        while (this.index < this.expression.length() && Character.isJavaIdentifierPart(this.expression.charAt(this.index))) this.index++;
        String text = this.expression.substring(start, this.index);
        switch (text) {
            case "new":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.NEW, text, start, this.index - 1);
            case "instanceof":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.INSTANCEOF, text, start, this.index - 1);
            case "true":
            case "false":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.BOOLEAN, text, start, this.index - 1);
            case "null":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.NULL, text, start, this.index - 1);
            case "return":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.RETURN, text, start, this.index - 1);
            case "throw":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.THROW, text, start, this.index - 1);
            case "this":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.THIS, text, start, this.index - 1);
            case "super":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.SUPER, text, start, this.index - 1);
            case "class":
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.CLASS, text, start, this.index - 1);
            default:
                return new ExpressionToken(ExpressionToken.ExpressionTokenType.IDENTIFIER, text, start, this.index - 1);
        }
    }

    private void skipWhitespace() {
        while (this.index < this.expression.length() && Character.isWhitespace(this.expression.charAt(this.index))) this.index++;
    }

    private IllegalArgumentException error(final String message, final int position) {
        return new IllegalArgumentException("Failed to parse expression '" + this.expression + "': " + message + " at index " + position);
    }

}
