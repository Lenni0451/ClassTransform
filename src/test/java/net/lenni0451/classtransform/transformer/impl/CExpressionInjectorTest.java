package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.CArgs;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.expression.CDefinition;
import net.lenni0451.classtransform.annotations.expression.CExpression;
import net.lenni0451.classtransform.annotations.expression.CLocalDefinition;
import net.lenni0451.classtransform.annotations.injection.CModifyArg;
import net.lenni0451.classtransform.annotations.injection.CModifyArgs;
import net.lenni0451.classtransform.annotations.injection.CModifyVariable;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CExpressionInjectorTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void modifyArgExpressionTarget() throws Exception {
        Class<?> target = this.transform(ModifyArgExpression.class);
        assertEquals(9, target.getMethod("callAbs").invoke(null));
    }

    @Test
    public void modifyArgsExpressionTarget() throws Exception {
        Class<?> target = this.transform(ModifyArgsExpression.class);
        assertEquals(30, target.getMethod("callSum").invoke(null));
    }

    @Test
    public void modifyVariableExpressionTarget() throws Exception {
        Class<?> target = this.transform(ModifyVariableExpression.class);
        assertEquals(15, target.getMethod("callLocal").invoke(null));
    }

    private Class<?> transform(final Class<?> transformerClass) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        byte[] bytes = manager.transform(ExpressionTarget.class.getName(), this.classProvider.getClass(ExpressionTarget.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class ExpressionTarget {

        public static int callAbs() {
            return Math.abs(-1);
        }

        public static int callSum() {
            return sum(1, 2);
        }

        public static int callLocal() {
            int value = 5;
            return Math.abs(value);
        }

        public static int sum(int first, int second) {
            return first + second;
        }

    }

    @CTransformer(ExpressionTarget.class)
    public static class ModifyArgExpression {

        @CDefinition(id = "abs", method = "Ljava/lang/Math;abs(I)I")
        @CExpression("abs(?)")
        @CModifyArg(method = "callAbs", target = @CTarget(value = "EXPRESSION"), index = 0)
        public static int modify(int value) {
            return value + 10;
        }

    }

    @CTransformer(ExpressionTarget.class)
    public static class ModifyArgsExpression {

        @CDefinition(id = "sum", method = "sum(II)I")
        @CExpression("sum(?, ?)")
        @CModifyArgs(method = "callSum", target = @CTarget(value = "EXPRESSION"))
        public static void modify(CArgs args) {
            args.set(0, 10);
            args.set(1, 20);
        }

    }

    @CTransformer(ExpressionTarget.class)
    public static class ModifyVariableExpression {

        @CDefinition(id = "local", local = @CLocalDefinition(type = int.class, name = "value"))
        @CExpression("local")
        @CModifyVariable(method = "callLocal", target = @CTarget(value = "EXPRESSION"), name = "value")
        public static int modify(int value) {
            return value + 10;
        }

    }

}
