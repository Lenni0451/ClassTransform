package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.transformer.ATransformerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class COverrideTransformerTest extends ATransformerTest {

    private final COverrideTransformer transformer = new COverrideTransformer();

    @Test
    @DisplayName("Override static method")
    public void overrideStaticMethod() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.COverrideTransformerTest$StaticOverrideTest");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double sPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals("SPI".hashCode(), sPi);
    }

    @Test
    @DisplayName("Override virtual method")
    public void overrideVirtualMethod() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.COverrideTransformerTest$VirtualOverrideTest");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        double vPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(instance));
        assertEquals("VPI".hashCode(), vPi);
    }


    @CTransformer(SCalculator.class)
    private static class StaticOverrideTest {

        @COverride
        public static double getPi() {
            return "SPI".hashCode();
        }

    }

    @CTransformer(VCalculator.class)
    private static class VirtualOverrideTest {

        @COverride
        public double getPi() {
            return "VPI".hashCode();
        }

    }

}