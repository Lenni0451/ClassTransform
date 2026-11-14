package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.expression.CDefinition;
import net.lenni0451.classtransform.annotations.expression.CExpression;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExpressionLanguageTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @ParameterizedTest
    @CsvSource({
            "sub, 8",
            "mul, 19",
            "div, 5",
            "mod, 2",
            "shl, 37",
            "shr, 3",
            "ushr, 3",
            "and, 1",
            "xor, 12",
            "or, 12"
    })
    public void modifyBinaryOperation(final String name, final int expected) throws Exception {
        Class<?> target = this.transform(name);
        assertEquals(expected, target.getMethod(name, int.class, int.class).invoke(null, 9, 2));
    }

    @ParameterizedTest
    @CsvSource({
            "le, false",
            "gt, true",
            "ge, true",
            "eq, true",
            "ne, false"
    })
    public void modifyComparison(final String name, final boolean expected) throws Exception {
        Class<?> target = this.transform(name);
        assertEquals(expected, target.getMethod(name, int.class, int.class).invoke(null, 1, 2));
    }

    @Test
    public void definitionSelectsOverloadedMethod() throws Exception {
        Class<?> target = this.transform(OverloadTransformer.class, "modify");
        assertEquals(19, target.getMethod("useInt").invoke(null));
        assertEquals(16L, target.getMethod("useLong").invoke(null));
    }

    @Test
    public void modifyNegativeIntLiteral() throws Exception {
        assertEquals(5, this.transform("negativeInt").getMethod("negativeInt").invoke(null));
    }

    @Test
    public void modifyNegativeDecimalLiteral() throws Exception {
        assertEquals(-1.5D, this.transform("negativeDecimal").getMethod("negativeDecimal").invoke(null));
    }

    @Test
    public void modifyBooleanLiteral() throws Exception {
        assertEquals(false, this.transform("booleanLiteral").getMethod("booleanLiteral").invoke(null));
    }

    @Test
    public void modifyCapturedExpression() throws Exception {
        assertEquals(12, this.transform("capturedBinary").getMethod("capturedBinary", int.class, int.class).invoke(null, 9, 2));
    }

    @Test
    public void modifyWildcardMember() throws Exception {
        assertEquals(15, this.transform("wildcardMember").getMethod("wildcardMember").invoke(null));
    }

    @Test
    public void modifyArrayCast() throws Exception {
        assertEquals(10, this.transform("arrayCast").getMethod("arrayCast", Object.class).invoke(null, new int[]{1, 2}));
    }

    private Class<?> transform(final String name) throws Exception {
        return this.transform(Transformer.class, name);
    }

    private Class<?> transform(final Class<?> transformerClass, final String name) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        ClassNode transformer = ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName()));
        transformer.methods.removeIf(method -> !method.name.equals(name));
        manager.addTransformer(transformer);
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class Holder {

        public int value;

        public Holder(final int value) {
            this.value = value;
        }

    }

    public static class Target {

        public static int compute(final int value) {
            return value * 2;
        }

        public static long compute(final long value) {
            return value * 3;
        }

        public static int useInt() {
            return compute(4) + 1;
        }

        public static long useLong() {
            return compute(5L) + 1L;
        }

        public static int sub(final int first, final int second) {
            return first - second;
        }

        public static int mul(final int first, final int second) {
            return first * second;
        }

        public static int div(final int first, final int second) {
            return first / second;
        }

        public static int mod(final int first, final int second) {
            return first % second;
        }

        public static int shl(final int first, final int second) {
            return first << second;
        }

        public static int shr(final int first, final int second) {
            return first >> second;
        }

        public static int ushr(final int first, final int second) {
            return first >>> second;
        }

        public static int and(final int first, final int second) {
            return first & second;
        }

        public static int xor(final int first, final int second) {
            return first ^ second;
        }

        public static int or(final int first, final int second) {
            return first | second;
        }

        public static boolean le(final int first, final int second) {
            return first <= second;
        }

        public static boolean gt(final int first, final int second) {
            return first > second;
        }

        public static boolean ge(final int first, final int second) {
            return first >= second;
        }

        public static boolean eq(final int first, final int second) {
            return first == second;
        }

        public static boolean ne(final int first, final int second) {
            return first != second;
        }

        public static int negativeInt() {
            return -5;
        }

        public static double negativeDecimal() {
            return -2.5D;
        }

        public static boolean booleanLiteral() {
            return true;
        }

        public static int capturedBinary(final int first, final int second) {
            return first + second;
        }

        public static int wildcardMember() {
            Holder holder = new Holder(5);
            return holder.value;
        }

        public static int arrayCast(final Object value) {
            int[] array = (int[]) value;
            return array[0];
        }

    }

    @CTransformer(Target.class)
    public static class Transformer {

        @CExpression("? - ?")
        @CModifyExpressionValue(method = "sub", target = @CTarget("EXPRESSION"))
        public static int sub(final int value) {
            return value + 1;
        }

        @CExpression("? * ?")
        @CModifyExpressionValue(method = "mul", target = @CTarget("EXPRESSION"))
        public static int mul(final int value) {
            return value + 1;
        }

        @CExpression("? / ?")
        @CModifyExpressionValue(method = "div", target = @CTarget("EXPRESSION"))
        public static int div(final int value) {
            return value + 1;
        }

        @CExpression("? % ?")
        @CModifyExpressionValue(method = "mod", target = @CTarget("EXPRESSION"))
        public static int mod(final int value) {
            return value + 1;
        }

        @CExpression("? << ?")
        @CModifyExpressionValue(method = "shl", target = @CTarget("EXPRESSION"))
        public static int shl(final int value) {
            return value + 1;
        }

        @CExpression("? >> ?")
        @CModifyExpressionValue(method = "shr", target = @CTarget("EXPRESSION"))
        public static int shr(final int value) {
            return value + 1;
        }

        @CExpression("? >>> ?")
        @CModifyExpressionValue(method = "ushr", target = @CTarget("EXPRESSION"))
        public static int ushr(final int value) {
            return value + 1;
        }

        @CExpression("? & ?")
        @CModifyExpressionValue(method = "and", target = @CTarget("EXPRESSION"))
        public static int and(final int value) {
            return value + 1;
        }

        @CExpression("? ^ ?")
        @CModifyExpressionValue(method = "xor", target = @CTarget("EXPRESSION"))
        public static int xor(final int value) {
            return value + 1;
        }

        @CExpression("? | ?")
        @CModifyExpressionValue(method = "or", target = @CTarget("EXPRESSION"))
        public static int or(final int value) {
            return value + 1;
        }

        @CExpression("? <= ?")
        @CModifyExpressionValue(method = "le", target = @CTarget("EXPRESSION"))
        public static boolean le(final boolean value) {
            return !value;
        }

        @CExpression("? > ?")
        @CModifyExpressionValue(method = "gt", target = @CTarget("EXPRESSION"))
        public static boolean gt(final boolean value) {
            return !value;
        }

        @CExpression("? >= ?")
        @CModifyExpressionValue(method = "ge", target = @CTarget("EXPRESSION"))
        public static boolean ge(final boolean value) {
            return !value;
        }

        @CExpression("? == ?")
        @CModifyExpressionValue(method = "eq", target = @CTarget("EXPRESSION"))
        public static boolean eq(final boolean value) {
            return !value;
        }

        @CExpression("? != ?")
        @CModifyExpressionValue(method = "ne", target = @CTarget("EXPRESSION"))
        public static boolean ne(final boolean value) {
            return !value;
        }

        @CExpression("-5")
        @CModifyExpressionValue(method = "negativeInt", target = @CTarget("EXPRESSION"))
        public static int negativeInt(final int value) {
            return value + 10;
        }

        @CExpression("-2.5")
        @CModifyExpressionValue(method = "negativeDecimal", target = @CTarget("EXPRESSION"))
        public static double negativeDecimal(final double value) {
            return value + 1D;
        }

        @CExpression("true")
        @CModifyExpressionValue(method = "booleanLiteral", target = @CTarget("EXPRESSION"))
        public static boolean booleanLiteral(final boolean value) {
            return !value;
        }

        @CExpression("@(? + ?)")
        @CModifyExpressionValue(method = "capturedBinary", target = @CTarget("EXPRESSION"))
        public static int capturedBinary(final int value) {
            return value + 1;
        }

        @CExpression("?.?")
        @CModifyExpressionValue(method = "wildcardMember", target = @CTarget("EXPRESSION"))
        public static int wildcardMember(final int value) {
            return value + 10;
        }

        @CExpression("(int[]) ?")
        @CModifyExpressionValue(method = "arrayCast", target = @CTarget("EXPRESSION"))
        public static int[] arrayCast(final int[] value) {
            value[0] = 10;
            return value;
        }

    }

    @CTransformer(Target.class)
    public static class OverloadTransformer {

        @CDefinition(id = "intCall", method = "compute(I)I")
        @CExpression("intCall(?)")
        @CModifyExpressionValue(method = "useInt", target = @CTarget("EXPRESSION"))
        public static int modify(final int value) {
            return value + 10;
        }

    }

}
