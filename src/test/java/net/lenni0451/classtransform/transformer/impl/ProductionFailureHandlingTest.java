package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CModifyArg;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionFailureHandlingTest {

    private static final String CALLS = "Lnet/lenni0451/classtransform/transformer/impl/ProductionFailureHandlingTest$Calls;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void rejectInvalidModifyArgHandlers() {
        assertThrows(TransformerException.class, () -> this.transform(Calls.class, InvalidModifyArgVoid.class));
        assertThrows(TransformerException.class, () -> this.transform(Calls.class, InvalidModifyArgNoArguments.class));
    }

    @Test
    public void failStrategiesDecideTransformationOutcome() throws Exception {
        assertNull(this.transformWithStrategy(Calls.class, InvalidModifyArgVoid.class, FailStrategy.CANCEL));
        assertThrows(TransformerException.class,
                () -> this.transformWithStrategy(Calls.class, InvalidModifyArgVoid.class, FailStrategy.THROW));
    }

    private Class<?> transform(final Class<?> targetClass, final Class<?> transformerClass) throws Exception {
        byte[] bytes = this.transformWithStrategy(targetClass, transformerClass, FailStrategy.THROW);
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }

    private byte[] transformWithStrategy(final Class<?> targetClass, final Class<?> transformerClass, final FailStrategy strategy) throws ClassNotFoundException {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(strategy);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        return manager.transform(targetClass.getName(), this.classProvider.getClass(targetClass.getName()));
    }


    public static class Calls {

        public static int call() {
            return sum(1, 2);
        }

        public static int sum(final int first, final int second) {
            return first + second;
        }

    }

    @CTransformer(Calls.class)
    public static class InvalidModifyArgVoid {

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = CALLS + "sum(II)I"), index = 0)
        public static void handler(final int value) {
        }

    }

    @CTransformer(Calls.class)
    public static class InvalidModifyArgNoArguments {

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = CALLS + "sum(II)I"))
        public static int handler() {
            return 0;
        }

    }

}
