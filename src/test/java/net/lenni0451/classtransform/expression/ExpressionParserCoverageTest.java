package net.lenni0451.classtransform.expression;

import net.lenni0451.classtransform.expression.ExpressionAst.ArrayLiteralExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.ArrayStoreExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.BinaryExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.BooleanLiteralExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.BoundMethodReferenceExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.CapturingExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.CastExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.ComparisonExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.DecimalLiteralExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.FreeMethodReferenceExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.IdentifierAssignmentExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.InstanceofExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.IntLiteralExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.MemberAccessExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.MemberAssignmentExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.NewArrayExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.ReturnExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.SuperCallExpression;
import net.lenni0451.classtransform.expression.ExpressionAst.ThrowExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionParserCoverageTest {

    @Test
    public void parseStatements() {
        assertInstanceOf(ReturnExpression.class, ExpressionParser.parse("return ?"));
        assertInstanceOf(ThrowExpression.class, ExpressionParser.parse("throw ?"));
        assertInstanceOf(IdentifierAssignmentExpression.class, ExpressionParser.parse("value = ?"));
        assertInstanceOf(MemberAssignmentExpression.class, ExpressionParser.parse("?.field = ?"));
        assertInstanceOf(ArrayStoreExpression.class, ExpressionParser.parse("?[?] = ?"));
    }

    @Test
    public void parseBitwiseOperators() {
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? | ?"));
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? ^ ?"));
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? & ?"));
    }

    @Test
    public void parseComparisons() {
        assertInstanceOf(ComparisonExpression.class, ExpressionParser.parse("? == ?"));
        assertInstanceOf(ComparisonExpression.class, ExpressionParser.parse("? != ?"));
        assertInstanceOf(ComparisonExpression.class, ExpressionParser.parse("? < ?"));
        assertInstanceOf(ComparisonExpression.class, ExpressionParser.parse("? <= ?"));
        assertInstanceOf(ComparisonExpression.class, ExpressionParser.parse("? > ?"));
        assertInstanceOf(ComparisonExpression.class, ExpressionParser.parse("? >= ?"));
    }

    @Test
    public void parseShiftsAndArithmetic() {
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? << ?"));
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? >> ?"));
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? >>> ?"));
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? - ?"));
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? / ?"));
        assertInstanceOf(BinaryExpression.class, ExpressionParser.parse("? % ?"));
    }

    @Test
    public void parseLiterals() {
        assertInstanceOf(IntLiteralExpression.class, ExpressionParser.parse("-5"));
        assertInstanceOf(DecimalLiteralExpression.class, ExpressionParser.parse("-2.5"));
        assertInstanceOf(DecimalLiteralExpression.class, ExpressionParser.parse("2.5"));
        assertInstanceOf(BooleanLiteralExpression.class, ExpressionParser.parse("true"));
        assertInstanceOf(BooleanLiteralExpression.class, ExpressionParser.parse("false"));
    }

    @Test
    public void parseSpecialExpressions() {
        assertInstanceOf(SuperCallExpression.class, ExpressionParser.parse("super.toString()"));
        assertInstanceOf(CapturingExpression.class, ExpressionParser.parse("@(?)"));
        assertInstanceOf(MemberAccessExpression.class, ExpressionParser.parse("?.?"));
        assertInstanceOf(FreeMethodReferenceExpression.class, ExpressionParser.parse("::?"));
        assertInstanceOf(BoundMethodReferenceExpression.class, ExpressionParser.parse("?::?"));
    }

    @Test
    public void parseTypes() {
        assertInstanceOf(CastExpression.class, ExpressionParser.parse("(int[]) ?"));
        assertInstanceOf(CastExpression.class, ExpressionParser.parse("(?[]) ?"));
        assertInstanceOf(InstanceofExpression.class, ExpressionParser.parse("? instanceof int[]"));
        assertInstanceOf(NewArrayExpression.class, ExpressionParser.parse("new int[]"));
        assertInstanceOf(NewArrayExpression.class, ExpressionParser.parse("new int[?][]"));
        assertInstanceOf(ArrayLiteralExpression.class, ExpressionParser.parse("new int[]{1, ?, 3}"));
    }

    @Test
    public void rejectInvalidExpressions() {
        assertThrows(IllegalArgumentException.class, () -> ExpressionParser.parse("("));
        assertThrows(IllegalArgumentException.class, () -> ExpressionParser.parse("? +"));
        assertThrows(IllegalArgumentException.class, () -> ExpressionParser.parse("? ?"));
        assertThrows(IllegalArgumentException.class, () -> ExpressionParser.parse("value ="));
    }

}
