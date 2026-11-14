package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.CArgs;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.expression.CExpression;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.annotations.injection.CModifyArg;
import net.lenni0451.classtransform.annotations.injection.CModifyArgs;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.annotations.injection.CModifyVariable;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.annotations.injection.CWrapOperation;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductionCoreIntegrationTest {

    private static final String CHECKOUT = "Lnet/lenni0451/classtransform/transformer/impl/ProductionCoreIntegrationTest$CheckoutService;";
    private static final String WRAPPING = "Lnet/lenni0451/classtransform/transformer/impl/ProductionCoreIntegrationTest$WrappingService;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void checkoutPipelineCombinesArgumentsReturnAndInject() throws Exception {
        Class<?> target = this.transform(CheckoutService.class, CheckoutTransformer.class);
        assertEquals(26, target.getMethod("checkout", int.class, int.class).invoke(null, 2, 3));
        assertEquals(1, target.getField("injections").get(null));
        assertEquals(1, target.getField("multiplications").get(null));
    }

    @Test
    public void wrapRedirectAndConditionPipeline() throws Exception {
        Class<?> target = this.transform(WrappingService.class, WrappingTransformer.class);
        assertEquals(42, target.getMethod("calculate", int.class).invoke(null, 100));
        assertEquals(1, target.getField("conditions").get(null));
        assertEquals(-1, target.getField("lastAudit").get(null));
    }

    @Test
    public void modifyLocalAndExpressionTogether() throws Exception {
        Class<?> target = this.transform(LocalExpressionService.class, LocalExpressionTransformer.class);
        assertEquals(24, target.getMethod("process", int.class).invoke(null, 5));
    }

    private Class<?> transform(final Class<?> targetClass, final Class<?>... transformerClasses) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        for (Class<?> transformerClass : transformerClasses) {
            manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        }
        byte[] bytes = manager.transform(targetClass.getName(), this.classProvider.getClass(targetClass.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class CheckoutService {

        public static int injections;
        public static int multiplications;

        public static int checkout(final int base, final int quantity) {
            int subtotal = multiply(base, quantity);
            int total = addFee(subtotal);
            return total;
        }

        public static int multiply(final int first, final int second) {
            multiplications++;
            return first * second;
        }

        public static int addFee(final int value) {
            return value + 5;
        }

    }

    @CTransformer(CheckoutService.class)
    public static class CheckoutTransformer {

        @CInject(method = "checkout", target = @CTarget("HEAD"))
        public static void countInjection() {
            CheckoutService.injections++;
        }

        @CModifyArg(method = "checkout", target = @CTarget(value = "INVOKE", target = CHECKOUT + "multiply(II)I"), index = 0)
        public static int bumpBase(final int base) {
            return base + 1;
        }

        @CModifyArgs(method = "checkout", target = @CTarget(value = "INVOKE", target = CHECKOUT + "addFee(I)I"))
        public static void bumpFee(final CArgs args) {
            args.setInt(0, args.getInt(0) + 2);
        }

        @CModifyReturnValue(method = "checkout")
        public static int addShipping(final int total) {
            return total + 10;
        }

    }


    public static class WrappingService {

        public static int conditions;
        public static int lastAudit = -1;

        public static int calculate(final int value) {
            int fee = add(tax(value), 1);
            audit(fee);
            return fee;
        }

        public static int tax(final int value) {
            return value / 10;
        }

        public static int add(final int first, final int second) {
            return first + second;
        }

        public static void audit(final int value) {
            lastAudit = value;
        }

    }

    @CTransformer(WrappingService.class)
    public static class WrappingTransformer {

        @CRedirect(method = "calculate", target = @CTarget(value = "INVOKE", target = WRAPPING + "tax(I)I"))
        public static int tax(final int value) {
            return value / 5;
        }

        @CWrapOperation(method = "calculate", target = @CTarget(value = "INVOKE", target = WRAPPING + "add(II)I"))
        public static int add(final int first, final int second, final COperation<Integer> original) {
            return original.call(first, second) * 2;
        }

        @CWrapCondition(method = "calculate", target = @CTarget(value = "INVOKE", target = WRAPPING + "audit(I)V"))
        public static boolean condition(final int value) {
            WrappingService.conditions++;
            return false;
        }

    }


    public static class LocalExpressionService {

        public static int process(final int input) {
            int doubled = input * 2;
            int adjusted = doubled + 3;
            return adjusted;
        }

    }

    @CTransformer(LocalExpressionService.class)
    public static class LocalExpressionTransformer {

        @CExpression("? * ?")
        @CModifyExpressionValue(method = "process", target = @CTarget("EXPRESSION"))
        public static int multiply(final int value) {
            return value + 1;
        }

        @CModifyVariable(method = "process", target = @CTarget("STORE"), name = "adjusted")
        public static int adjusted(final int value) {
            return value + 10;
        }

    }


}
