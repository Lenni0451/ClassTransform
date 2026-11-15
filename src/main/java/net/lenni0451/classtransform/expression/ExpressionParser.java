package net.lenni0451.classtransform.expression;

import net.lenni0451.classtransform.expression.ExpressionAst.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written parser for the expression language.<br>
 * The supported grammar is a small java-like language:
 * <pre>
 * statement   := return expression | throw expression | expression ('=' expression)?;
 * expression  := bitwiseOr;
 * bitwiseOr   := bitwiseXor ('|' bitwiseXor)*;
 * bitwiseXor  := bitwiseAnd ('^' bitwiseAnd)*;
 * bitwiseAnd  := equality ('&amp;' equality)*;
 * equality    := relational (('==' | '!=') relational)*;
 * relational  := shift (('&lt;' | '&lt;=' | '&gt;' | '&gt;=') shift | 'instanceof' type)*;
 * shift       := additive (('&lt;&lt;' | '&gt;&gt;' | '&gt;&gt;&gt;') additive)*;
 * additive    := multiplicative (('+' | '-') multiplicative)*;
 * multiplicative := unary (('*' | '/' | '%') unary)*;
 * unary       := ('-' | '~') unary | postfix;
 * postfix     := primary ('.' name ('(' arguments ')')? | '[' expression ']' | '::' name | '::' 'new')*;
 * primary     := literal | 'this' | 'super' '.' name '(' arguments ')' | 'new' type (...)?
 *              | '?' | '::' name | '@(' expression ')' | type '.class' | name ( '(' arguments ')' )?
 *              | '(' expression ')' | '(' type ')' expression;
 * name        := identifier | '?';
 * type        := name ('[]')*;
 * </pre>
 * The AST is matched against a value-flow graph built by {@link ExpressionFlow}.
 */
@ParametersAreNonnullByDefault
public class ExpressionParser {

    /**
     * Parses an expression string into an AST.
     * <p>
     * Accepts one statement as described by the class grammar, from the first token to the end of the input.
     * The returned tree is independent of the parser and can be reused for multiple matches.
     *
     * @param expression the expression to parse
     * @return the root node of the expression tree
     * @throws IllegalArgumentException if the expression is malformed or assigns to an invalid target
     */
    public static Expression parse(final String expression) {
        ExpressionParser parser = new ExpressionParser(expression);
        Expression result = parser.parseStatement();
        parser.expect(ExpressionToken.ExpressionTokenType.EOF);
        return result;
    }


    private final String input;
    private final List<ExpressionToken> tokens;
    private int index;

    private ExpressionParser(final String input) {
        this.input = input;
        this.tokens = new ExpressionLexer(input).tokenize();
    }

    private Expression parseStatement() {
        if (this.match(ExpressionToken.ExpressionTokenType.RETURN)) {
            ExpressionToken start = this.previous();
            return new ReturnExpression(this.source(start), this.parseExpression());
        }
        if (this.match(ExpressionToken.ExpressionTokenType.THROW)) {
            ExpressionToken start = this.previous();
            return new ThrowExpression(this.source(start), this.parseExpression());
        }
        Expression expression = this.parseExpression();
        if (this.match(ExpressionToken.ExpressionTokenType.ASSIGN)) {
            Expression value = this.parseExpression();
            if (expression instanceof IdentifierExpression) {
                return new IdentifierAssignmentExpression(this.source(this.previous()), ((IdentifierExpression) expression).getIdentifier(), value);
            }
            if (expression instanceof MemberAccessExpression) {
                MemberAccessExpression member = (MemberAccessExpression) expression;
                return new MemberAssignmentExpression(this.source(this.previous()), member.getReceiver(), member.getMember(), value);
            }
            if (expression instanceof ArrayAccessExpression) {
                ArrayAccessExpression array = (ArrayAccessExpression) expression;
                return new ArrayStoreExpression(this.source(this.previous()), array.getArray(), array.getIndex(), value);
            }
            throw this.error("Invalid assignment target");
        }
        return expression;
    }

    private Expression parseExpression() {
        return this.parseBitwiseOr();
    }

    private Expression parseBitwiseOr() {
        Expression expression = this.parseBitwiseXor();
        while (this.match(ExpressionToken.ExpressionTokenType.BITWISE_OR)) {
            ExpressionToken operator = this.previous();
            expression = new BinaryExpression(this.source(operator), expression, BinaryExpression.Operator.BITWISE_OR, this.parseBitwiseXor());
        }
        return expression;
    }

    private Expression parseBitwiseXor() {
        Expression expression = this.parseBitwiseAnd();
        while (this.match(ExpressionToken.ExpressionTokenType.BITWISE_XOR)) {
            ExpressionToken operator = this.previous();
            expression = new BinaryExpression(this.source(operator), expression, BinaryExpression.Operator.BITWISE_XOR, this.parseBitwiseAnd());
        }
        return expression;
    }

    private Expression parseBitwiseAnd() {
        Expression expression = this.parseEquality();
        while (this.match(ExpressionToken.ExpressionTokenType.BITWISE_AND)) {
            ExpressionToken operator = this.previous();
            expression = new BinaryExpression(this.source(operator), expression, BinaryExpression.Operator.BITWISE_AND, this.parseEquality());
        }
        return expression;
    }

    private Expression parseEquality() {
        Expression expression = this.parseRelational();
        while (true) {
            if (this.match(ExpressionToken.ExpressionTokenType.EQ)) {
                expression = new ComparisonExpression(this.source(this.previous()), expression, ComparisonExpression.Operator.EQ, this.parseRelational());
            } else if (this.match(ExpressionToken.ExpressionTokenType.NE)) {
                expression = new ComparisonExpression(this.source(this.previous()), expression, ComparisonExpression.Operator.NE, this.parseRelational());
            } else {
                return expression;
            }
        }
    }

    private Expression parseRelational() {
        Expression expression = this.parseShift();
        while (true) {
            if (this.match(ExpressionToken.ExpressionTokenType.LT)) {
                expression = new ComparisonExpression(this.source(this.previous()), expression, ComparisonExpression.Operator.LT, this.parseShift());
            } else if (this.match(ExpressionToken.ExpressionTokenType.LE)) {
                expression = new ComparisonExpression(this.source(this.previous()), expression, ComparisonExpression.Operator.LE, this.parseShift());
            } else if (this.match(ExpressionToken.ExpressionTokenType.GT)) {
                expression = new ComparisonExpression(this.source(this.previous()), expression, ComparisonExpression.Operator.GT, this.parseShift());
            } else if (this.match(ExpressionToken.ExpressionTokenType.GE)) {
                expression = new ComparisonExpression(this.source(this.previous()), expression, ComparisonExpression.Operator.GE, this.parseShift());
            } else if (this.match(ExpressionToken.ExpressionTokenType.INSTANCEOF)) {
                expression = new InstanceofExpression(this.source(this.previous()), expression, this.parseType());
            } else {
                return expression;
            }
        }
    }

    private Expression parseShift() {
        Expression expression = this.parseAdditive();
        while (true) {
            if (this.match(ExpressionToken.ExpressionTokenType.SHL)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.SHL, this.parseAdditive());
            } else if (this.match(ExpressionToken.ExpressionTokenType.SHR)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.SHR, this.parseAdditive());
            } else if (this.match(ExpressionToken.ExpressionTokenType.USHR)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.USHR, this.parseAdditive());
            } else {
                return expression;
            }
        }
    }

    private Expression parseAdditive() {
        Expression expression = this.parseMultiplicative();
        while (true) {
            if (this.match(ExpressionToken.ExpressionTokenType.PLUS)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.PLUS, this.parseMultiplicative());
            } else if (this.match(ExpressionToken.ExpressionTokenType.MINUS)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.MINUS, this.parseMultiplicative());
            } else {
                return expression;
            }
        }
    }

    private Expression parseMultiplicative() {
        Expression expression = this.parseUnary();
        while (true) {
            if (this.match(ExpressionToken.ExpressionTokenType.MULT)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.MULT, this.parseUnary());
            } else if (this.match(ExpressionToken.ExpressionTokenType.DIV)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.DIV, this.parseUnary());
            } else if (this.match(ExpressionToken.ExpressionTokenType.MOD)) {
                expression = new BinaryExpression(this.source(this.previous()), expression, BinaryExpression.Operator.MOD, this.parseUnary());
            } else {
                return expression;
            }
        }
    }

    private Expression parseUnary() {
        if (this.match(ExpressionToken.ExpressionTokenType.MINUS)) {
            ExpressionToken operator = this.previous();
            if (this.check(ExpressionToken.ExpressionTokenType.INT)) {
                ExpressionToken literal = this.advance();
                return new IntLiteralExpression(this.source(literal), -Long.parseLong(normalizeInt(literal.text)));
            }
            if (this.check(ExpressionToken.ExpressionTokenType.DECIMAL)) {
                ExpressionToken literal = this.advance();
                return new DecimalLiteralExpression(this.source(literal), -Double.parseDouble(literal.text));
            }
            return new UnaryExpression(this.source(operator), UnaryExpression.Operator.MINUS, this.parseUnary());
        }
        if (this.match(ExpressionToken.ExpressionTokenType.BITWISE_NOT)) {
            ExpressionToken operator = this.previous();
            return new UnaryExpression(this.source(operator), UnaryExpression.Operator.BITWISE_NOT, this.parseUnary());
        }
        return this.parsePostfix();
    }

    private Expression parsePostfix() {
        Expression expression = this.parsePrimary();
        while (true) {
            if (this.match(ExpressionToken.ExpressionTokenType.DOT)) {
                ExpressionToken dot = this.previous();
                if (this.match(ExpressionToken.ExpressionTokenType.CLASS)) {
                    expression = new ClassConstantExpression(this.source(dot), this.asType(expression));
                } else {
                    Name name = this.parseName();
                    if (this.match(ExpressionToken.ExpressionTokenType.LEFT_PAREN)) {
                        expression = new MethodCallExpression(this.source(dot), expression, this.asMember(name), this.parseArguments());
                    } else {
                        expression = new MemberAccessExpression(this.source(dot), expression, this.asMember(name));
                    }
                }
            } else if (this.match(ExpressionToken.ExpressionTokenType.LEFT_BRACKET)) {
                ExpressionToken bracket = this.previous();
                Expression index = this.parseExpression();
                this.expect(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET);
                expression = new ArrayAccessExpression(this.source(bracket), expression, index);
            } else if (this.match(ExpressionToken.ExpressionTokenType.METHOD_REF)) {
                ExpressionToken methodReference = this.previous();
                if (this.match(ExpressionToken.ExpressionTokenType.NEW)) {
                    return new ConstructorReferenceExpression(this.source(methodReference), this.asType(expression));
                }
                Name name = this.parseName();
                return new BoundMethodReferenceExpression(this.source(methodReference), expression, this.asMember(name));
            } else {
                return expression;
            }
        }
    }

    private Expression parsePrimary() {
        if (this.match(ExpressionToken.ExpressionTokenType.AT)) {
            ExpressionToken start = this.previous();
            this.expect(ExpressionToken.ExpressionTokenType.LEFT_PAREN);
            Expression inner = this.parseExpression();
            this.expect(ExpressionToken.ExpressionTokenType.RIGHT_PAREN);
            return new CapturingExpression(this.source(start), inner);
        }

        if (this.check(ExpressionToken.ExpressionTokenType.LEFT_PAREN)) {
            int startIndex = this.index;
            this.advance();
            TypeIdentifier castType = this.tryParseType();
            if (castType != null && this.match(ExpressionToken.ExpressionTokenType.RIGHT_PAREN) && this.startsExpression(this.peek())) {
                return new CastExpression(this.source(this.previous()), castType, this.parseUnary());
            }
            this.index = startIndex;
            this.expect(ExpressionToken.ExpressionTokenType.LEFT_PAREN);
            Expression expression = this.parseExpression();
            this.expect(ExpressionToken.ExpressionTokenType.RIGHT_PAREN);
            return expression;
        }

        if (this.check(ExpressionToken.ExpressionTokenType.IDENTIFIER) || this.check(ExpressionToken.ExpressionTokenType.WILDCARD)) {
            int startIndex = this.index;
            TypeIdentifier type = this.tryParseType();
            if (type != null && this.match(ExpressionToken.ExpressionTokenType.DOT) && this.match(ExpressionToken.ExpressionTokenType.CLASS)) {
                return new ClassConstantExpression(this.source(this.tokens.get(startIndex)), type);
            }
            this.index = startIndex;
        }

        if (this.match(ExpressionToken.ExpressionTokenType.METHOD_REF)) {
            ExpressionToken methodReference = this.previous();
            return new FreeMethodReferenceExpression(this.source(methodReference), this.asMember(this.parseName()));
        }
        if (this.match(ExpressionToken.ExpressionTokenType.WILDCARD)) {
            return new WildcardExpression(this.source(this.previous()));
        }
        if (this.match(ExpressionToken.ExpressionTokenType.THIS)) {
            return new ThisExpression(this.source(this.previous()));
        }
        if (this.match(ExpressionToken.ExpressionTokenType.SUPER)) {
            ExpressionToken start = this.previous();
            this.expect(ExpressionToken.ExpressionTokenType.DOT);
            Name name = this.parseName();
            this.expect(ExpressionToken.ExpressionTokenType.LEFT_PAREN);
            return new SuperCallExpression(this.source(start), this.asMember(name), this.parseArguments());
        }
        if (this.match(ExpressionToken.ExpressionTokenType.NEW)) {
            return this.parseNew(this.previous());
        }
        if (this.match(ExpressionToken.ExpressionTokenType.INT)) {
            ExpressionToken token = this.previous();
            return new IntLiteralExpression(this.source(token), Long.parseLong(normalizeInt(token.text)));
        }
        if (this.match(ExpressionToken.ExpressionTokenType.DECIMAL)) {
            ExpressionToken token = this.previous();
            return new DecimalLiteralExpression(this.source(token), Double.parseDouble(token.text));
        }
        if (this.match(ExpressionToken.ExpressionTokenType.STRING)) {
            ExpressionToken token = this.previous();
            return new StringLiteralExpression(this.source(token), token.text);
        }
        if (this.match(ExpressionToken.ExpressionTokenType.BOOLEAN)) {
            ExpressionToken token = this.previous();
            return new BooleanLiteralExpression(this.source(token), Boolean.parseBoolean(token.text));
        }
        if (this.match(ExpressionToken.ExpressionTokenType.NULL)) {
            return new NullLiteralExpression(this.source(this.previous()));
        }
        if (this.check(ExpressionToken.ExpressionTokenType.IDENTIFIER)) {
            ExpressionToken token = this.advance();
            if (this.match(ExpressionToken.ExpressionTokenType.LEFT_PAREN)) {
                return new StaticMethodCallExpression(this.source(token), new NamedMember(token.text), this.parseArguments());
            }
            return new IdentifierExpression(this.source(token), token.text);
        }
        throw this.error("Expected an expression");
    }

    private Expression parseNew(final ExpressionToken start) {
        Name name = this.parseName();
        if (this.match(ExpressionToken.ExpressionTokenType.LEFT_PAREN)) {
            return new InstantiationExpression(this.source(start), new NamedType(name), this.parseArguments());
        }
        int arrayLiteralDimensions = this.countEmptyBrackets();
        if (arrayLiteralDimensions > 0 && this.checkAt(this.index + arrayLiteralDimensions * 2, ExpressionToken.ExpressionTokenType.LEFT_BRACE)) {
            TypeIdentifier elementType = name.wildcard ? new WildcardType() : new NamedType(name);
            for (int i = 1; i < arrayLiteralDimensions; i++) elementType = new ArrayTypeIdentifier(1, elementType);
            for (int i = 0; i < arrayLiteralDimensions; i++) {
                this.expect(ExpressionToken.ExpressionTokenType.LEFT_BRACKET);
                this.expect(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET);
            }
            this.expect(ExpressionToken.ExpressionTokenType.LEFT_BRACE);
            List<Expression> values = new ArrayList<>();
            do {
                values.add(this.parseExpression());
            } while (this.match(ExpressionToken.ExpressionTokenType.COMMA));
            this.expect(ExpressionToken.ExpressionTokenType.RIGHT_BRACE);
            return new ArrayLiteralExpression(this.source(start), elementType, values);
        }

        int dims = 0;
        List<Expression> dimensions = new ArrayList<>();
        while (this.match(ExpressionToken.ExpressionTokenType.LEFT_BRACKET)) {
            if (this.match(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET)) {
                dims++;
                break;
            }
            dimensions.add(this.parseExpression());
            this.expect(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET);
        }
        if (dimensions.isEmpty() && dims == 0) throw this.error("Expected constructor arguments or array dimensions");
        while (this.match(ExpressionToken.ExpressionTokenType.LEFT_BRACKET)) {
            this.expect(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET);
            dims++;
        }
        return new NewArrayExpression(this.source(start), new NamedType(name), dimensions, dims);
    }

    private int countEmptyBrackets() {
        int count = 0;
        int index = this.index;
        while (index + 1 < this.tokens.size()
                && this.tokens.get(index).is(ExpressionToken.ExpressionTokenType.LEFT_BRACKET)
                && this.tokens.get(index + 1).is(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET)) {
            count++;
            index += 2;
        }
        return count;
    }

    private boolean checkAt(final int index, final ExpressionToken.ExpressionTokenType type) {
        return this.tokens.get(Math.min(index, this.tokens.size() - 1)).is(type);
    }

    private List<Expression> parseArguments() {
        List<Expression> arguments = new ArrayList<>();
        if (this.match(ExpressionToken.ExpressionTokenType.RIGHT_PAREN)) return arguments;
        do {
            arguments.add(this.parseExpression());
        } while (this.match(ExpressionToken.ExpressionTokenType.COMMA));
        this.expect(ExpressionToken.ExpressionTokenType.RIGHT_PAREN);
        return arguments;
    }

    @Nullable
    private TypeIdentifier tryParseType() {
        int start = this.index;
        List<String> dims = new ArrayList<>();
        Name name;
        if (this.check(ExpressionToken.ExpressionTokenType.IDENTIFIER) || this.check(ExpressionToken.ExpressionTokenType.WILDCARD)) {
            name = this.parseName();
        } else {
            return null;
        }
        while (this.match(ExpressionToken.ExpressionTokenType.LEFT_BRACKET)) {
            if (!this.match(ExpressionToken.ExpressionTokenType.RIGHT_BRACKET)) {
                this.index = start;
                return null;
            }
            dims.add("");
        }
        TypeIdentifier type = name.wildcard ? new WildcardType() : new NamedType(name);
        for (int i = 0; i < dims.size(); i++) type = new ArrayTypeIdentifier(1, type);
        return type;
    }

    private TypeIdentifier parseType() {
        TypeIdentifier type = this.tryParseType();
        if (type == null) throw this.error("Expected a type");
        return type;
    }

    private TypeIdentifier asType(final Expression expression) {
        if (expression instanceof IdentifierExpression) {
            return new NamedType(new Name(((IdentifierExpression) expression).getIdentifier(), false));
        }
        throw this.error("Expected a type");
    }

    private MemberName asMember(final Name name) {
        return name.wildcard ? new WildcardMember() : new NamedMember(name.identifier);
    }

    private Name parseName() {
        if (this.match(ExpressionToken.ExpressionTokenType.IDENTIFIER)) return new Name(this.previous().text, false);
        if (this.match(ExpressionToken.ExpressionTokenType.WILDCARD)) return new Name("?", true);
        throw this.error("Expected an identifier");
    }

    private boolean startsExpression(final ExpressionToken token) {
        switch (token.type) {
            case IDENTIFIER:
            case INT:
            case DECIMAL:
            case STRING:
            case BOOLEAN:
            case NULL:
            case NEW:
            case THIS:
            case SUPER:
            case WILDCARD:
            case AT:
            case LEFT_PAREN:
                return true;
            default:
                return false;
        }
    }

    private void expect(final ExpressionToken.ExpressionTokenType type) {
        if (!this.match(type)) throw this.error("Expected " + type + " but got " + this.peek());
    }

    private boolean match(final ExpressionToken.ExpressionTokenType type) {
        if (this.check(type)) {
            this.index++;
            return true;
        }
        return false;
    }

    private boolean check(final ExpressionToken.ExpressionTokenType type) {
        return this.peek().is(type);
    }

    private ExpressionToken advance() {
        return this.tokens.get(this.index++);
    }

    private ExpressionToken peek() {
        return this.tokens.get(Math.min(this.index, this.tokens.size() - 1));
    }

    private ExpressionToken previous() {
        return this.tokens.get(this.index - 1);
    }

    private ExpressionSource source(final ExpressionToken token) {
        return new ExpressionSource(this.input, token.start, token.end);
    }

    private IllegalArgumentException error(final String message) {
        ExpressionToken token = this.peek();
        return new IllegalArgumentException("Failed to parse expression '" + this.input + "': " + message + " at index " + token.start);
    }

    private static String normalizeInt(final String text) {
        if (text.startsWith("0x") || text.startsWith("0X")) return String.valueOf(Long.parseLong(text.substring(2), 16));
        return text;
    }


    private static class Name {
        private final String identifier;
        private final boolean wildcard;

        private Name(final String identifier, final boolean wildcard) {
            this.identifier = identifier;
            this.wildcard = wildcard;
        }
    }

    private static class NamedMember implements MemberName {
        private final String identifier;

        private NamedMember(final String identifier) {
            this.identifier = identifier;
        }

        @Override
        public boolean matches(final ExpressionContext ctx, final ExpressionFlow.FlowValue node) {
            return ctx.getIdentifiers().matchesMember(this.identifier, node);
        }

        @Override
        public boolean matches(final ExpressionContext ctx, final Handle handle) {
            return ctx.getIdentifiers().matchesMember(this.identifier, handle);
        }
    }

    private static class WildcardMember implements MemberName {
        @Override
        public boolean matches(final ExpressionContext ctx, final ExpressionFlow.FlowValue node) {
            return true;
        }

        @Override
        public boolean matches(final ExpressionContext ctx, final Handle handle) {
            return true;
        }
    }

    private static class NamedType implements TypeIdentifier {
        private final Name name;

        private NamedType(final Name name) {
            this.name = name;
        }

        @Override
        public boolean matches(final ExpressionIdentifiers identifiers, final Type type) {
            if (this.name.wildcard) return true;
            return identifiers.matchesType(this.name.identifier, type);
        }
    }

    private static class WildcardType implements TypeIdentifier {
        @Override
        public boolean matches(final ExpressionIdentifiers identifiers, final Type type) {
            return true;
        }
    }

    private static class ArrayTypeIdentifier implements TypeIdentifier {
        private final int dimensions;
        private final TypeIdentifier element;

        private ArrayTypeIdentifier(final int dimensions, final TypeIdentifier element) {
            this.dimensions = dimensions;
            this.element = element;
        }

        @Override
        public boolean matches(final ExpressionIdentifiers identifiers, final Type type) {
            Type elementType = type;
            for (int i = 0; i < this.dimensions; i++) {
                if (elementType.getSort() != Type.ARRAY) return false;
                elementType = elementType.getElementType();
            }
            return this.element.matches(identifiers, elementType);
        }
    }

}
