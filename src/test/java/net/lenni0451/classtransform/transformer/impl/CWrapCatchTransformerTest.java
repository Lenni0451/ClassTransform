package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CWrapCatch;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.transformer.ATransformerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CWrapCatchTransformerTest extends ATransformerTest {

    private final CWrapCatchTransformer transformer = new CWrapCatchTransformer();

    @Test
    @DisplayName("Wrap static method")
    public void wrapStaticMethod() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CWrapCatchTransformerTest$WrapStaticTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double zeroDiv = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("divide", double.class, double.class).invoke(null, 10, 0));
        assertTrue(Double.isInfinite(zeroDiv));
    }

    @Test
    @DisplayName("Wrap virtual method")
    public void wrapVirtualMethod() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CWrapCatchTransformerTest$WrapVirtualTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        double zeroDiv = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("divide", double.class, double.class).invoke(instance, 10, 0));
        assertTrue(Double.isInfinite(zeroDiv));
    }


    @CTransformer(SCalculator.class)
    private static class WrapStaticTransformer {

        @CWrapCatch("divide")
        public static double zeroDivisionIsInfinite(ArithmeticException e) {
            return Double.POSITIVE_INFINITY;
        }

    }

    @CTransformer(VCalculator.class)
    private static class WrapVirtualTransformer {

        @CWrapCatch("divide")
        public double zeroDivisionIsInfinite(ArithmeticException e) {
            return Double.POSITIVE_INFINITY;
        }

    }

}