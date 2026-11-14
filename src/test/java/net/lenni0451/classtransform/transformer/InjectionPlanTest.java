package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class InjectionPlanTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/InjectionPlanTest$Target;";
    private final BasicClassProvider provider = new BasicClassProvider();

    @Test
    void nativeNamedSlicesAndAggregateCounts() throws Exception {
        Class<?> target = this.transform("sliced");
        assertEquals(8, target.getMethod("values").invoke(null));
    }

    @Test
    void explicitRequireAllowsMissingPoints() throws Exception {
        assertEquals(3, this.transform("optional").getMethod("values").invoke(null));
        assertThrows(Throwable.class, () -> this.transform("required"));
        assertThrows(Throwable.class, () -> this.transform("legacyRequired"));
    }

    @Test
    void orderAfterAnOperationDoesNotReverseCalls() throws Exception {
        Class<?> target = this.transform("afterSecond", "afterFirst");
        target.getMethod("call").invoke(null);
        assertEquals(12, target.getField("trace").get(null));
    }

    @Test
    void virtualConstantModifiersChainBeforeLateExpressionModifiers() throws Exception {
        Class<?> target = this.transform("constantMultiply", "constantAdd", "constantExpression");
        assertEquals(25L, target.getMethod("wide").invoke(target.getConstructor().newInstance()));
    }

    @Test
    void allMethodsAreSelectedBeforeHandlersAreCopied() throws Exception {
        Class<?> target = this.transform("wildcard");
        assertEquals(2, target.getMethod("valueOne").invoke(null));
        assertEquals(3, target.getMethod("valueTwo").invoke(null));
    }

    private Class<?> transform(final String... names) throws Exception {
        TransformerManager manager = new TransformerManager(this.provider);
        manager.setFailStrategy(FailStrategy.THROW);
        ClassNode transformer = ASMUtils.fromBytes(this.provider.getClass(Transformer.class.getName()));
        transformer.methods.removeIf(method -> !Arrays.asList(names).contains(method.name));
        manager.addTransformer(transformer);
        byte[] bytes = manager.transform(Target.class.getName(), this.provider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return new ClassLoader() {
            Class<?> define() {
                return this.defineClass(Target.class.getName(), bytes, 0, bytes.length);
            }
        }.define();
    }

    public static class Target {
        public static int trace;

        public static int values() {
            return Math.abs(1) + Math.abs(2);
        }

        public static void call() {
            operation();
        }

        public static void operation() {
        }

        public long wide() {
            return 2L;
        }

        public static int valueOne() {
            return 1;
        }

        public static int valueTwo() {
            return 2;
        }
    }

    @CTransformer(Target.class)
    public static class Transformer {
        @CModifyExpressionValue(method = "values", targets = {
                @CTarget(value = "CONSTANT", target = "int 1", slice = "first"),
                @CTarget(value = "CONSTANT", target = "int 2", slice = "second")},
                slices = {
                        @CSlice(id = "first", to = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", selector = "FIRST")),
                        @CSlice(id = "second", from = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", selector = "FIRST"))},
                require = 2, allow = 2)
        private static int sliced(int value) {
            return value + value + 1;
        }

        @CModifyReturnValue(method = "missing", require = 0)
        private static int optional(int value) {
            return value;
        }

        @CModifyReturnValue(method = "missing", require = 1)
        private static int required(int value) {
            return value;
        }

        @CModifyReturnValue(method = "missing")
        private static int legacyRequired(int value) {
            return value;
        }

        @CInject(method = "call", target = @CTarget(value = "INVOKE", target = TARGET + "operation()V"), order = 200)
        private static void afterSecond() {
            Target.trace = Target.trace * 10 + 2;
        }

        @CInject(method = "call", target = @CTarget(value = "INVOKE", target = TARGET + "operation()V"), order = 100)
        private static void afterFirst() {
            Target.trace = Target.trace * 10 + 1;
        }

        @CModifyConstant(method = "wide", longValue = 2L, order = 200)
        private long constantMultiply(long value) {
            return value * 3;
        }

        @CModifyConstant(method = "wide", longValue = 2L, order = 100)
        private long constantAdd(long value) {
            return value + 3;
        }

        @CModifyExpressionValue(method = "wide", target = @CTarget(value = "CONSTANT", target = "long 2"), order = -100)
        private long constantExpression(long value) {
            return value + 10;
        }

        @CModifyReturnValue(method = "value*", require = 3, allow = 3)
        private static int wildcard(int value) {
            return value + 1;
        }
    }

}
