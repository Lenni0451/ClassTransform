package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.transformer.AnnotationHandlerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class COverrideTransformerTest extends AnnotationHandlerTest {

    private final COverrideAnnotationHandler transformer = new COverrideAnnotationHandler();

    @Test
    @DisplayName("Override static method")
    public void overrideStaticMethod() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.COverrideTransformerTest$StaticOverrideTest");
        this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double sPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals("SPI".hashCode(), sPi);
    }

    @Test
    @DisplayName("Override virtual method")
    public void overrideVirtualMethod() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.COverrideTransformerTest$VirtualOverrideTest");
        this.transformer.transform(this.transformerManager, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        double vPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(instance));
        assertEquals("VPI".hashCode(), vPi);
    }

    @Test
    @DisplayName("Override method selected by alias")
    public void overrideMethodSelectedByAlias() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.COverrideTransformerTest$AliasOverrideTest");
        this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double sPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals("ALIAS".hashCode(), sPi);
    }

    @Test
    @DisplayName("Pass constraint")
    public void passConstraint() {
        this.transformerManager.registerConstraintToken("TEST", 5);
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.COverrideTransformerTest$ConstraintOverrideTest");
        this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double sPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals("CONSTRAINT".hashCode(), sPi);
    }

    @Test
    @DisplayName("Fail constraint")
    public void failConstraint() {
        this.transformerManager.registerConstraintToken("TEST", 5);
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.COverrideTransformerTest$FailingConstraintOverrideTest");
        assertThrows(TransformerException.class, () -> this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer));
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

    @CTransformer(SCalculator.class)
    private static class AliasOverrideTest {

        @COverride(value = "missing()D", aliases = {"getPi()D"}, remap = false)
        public static double getPi() {
            return "ALIAS".hashCode();
        }

    }

    @CTransformer(SCalculator.class)
    private static class ConstraintOverrideTest {

        @COverride(constraints = "TEST(5)")
        public static double getPi() {
            return "CONSTRAINT".hashCode();
        }

    }

    @CTransformer(SCalculator.class)
    private static class FailingConstraintOverrideTest {

        @COverride(constraints = "TEST(6)")
        public static double getPi() {
            return "CONSTRAINT".hashCode();
        }

    }

}
