package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.transformer.AnnotationHandlerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CRedirectAnnotationHandlerTest extends AnnotationHandlerTest {

    private final CRedirectAnnotationHandler transformer = new CRedirectAnnotationHandler();

    @Test
    @DisplayName("Redirect invalid target")
    public void redirectInvalidTarget() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$SInvalidTestTransformer");
        assertThrows(InvalidTargetException.class, () -> this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer));
    }

    @Test
    @DisplayName("Redirect static invoke")
    public void redirectStaticInvoke() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$SInvokeTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        int rint = assertDoesNotThrow(() -> (int) clazz.getDeclaredMethod("rint").invoke(null));
        assertEquals(rint, 1234);
    }

    @Test
    @DisplayName("Redirect virtual invoke")
    public void redirectVirtualInvoke() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$VInvokeTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        int rint = assertDoesNotThrow(() -> (int) clazz.getDeclaredMethod("rint").invoke(instance));
        assertEquals(rint, 1234);
    }

    @Test
    @DisplayName("Redirect static GETFIELD")
    public void redirectStaticGETFIELD() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$SGetfieldTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double pi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals(pi, 1.23);
    }

    @Test
    @DisplayName("Redirect virtual GETFIELD")
    public void redirectVirtualGETFIELD() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$VGetfieldTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        double pi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(instance));
        assertEquals(pi, 1.23);
    }

    @Test
    @DisplayName("Redirect static PUTFIELD")
    public void redirectStaticPUTFIELD() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$SPutfieldTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double pi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals(pi, Math.PI);
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("setPi", double.class).invoke(null, 123));
        pi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals(pi, 3.21);
    }

    @Test
    @DisplayName("Redirect virtual PUTFIELD")
    public void redirectVirtualPUTFIELD() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$VPutfieldTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        double pi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(instance));
        assertEquals(pi, Math.PI);
        assertDoesNotThrow(() -> clazz.getDeclaredMethod("setPi", double.class).invoke(instance, 123));
        pi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(instance));
        assertEquals(pi, 3.21);
    }

    @Test
    @DisplayName("Redirect static NEW")
    public void redirectStaticNEW() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$SNewTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        int rint = assertDoesNotThrow(() -> (int) clazz.getDeclaredMethod("rint").invoke(null));
        assertEquals(rint, -1517918040);
    }

    @Test
    @DisplayName("Redirect virtual NEW")
    public void redirectVirtualNEW() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CRedirectTransformerTest$VNewTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        int rint = assertDoesNotThrow(() -> (int) clazz.getDeclaredMethod("rint").invoke(instance));
        assertEquals(rint, -1517918040);
    }


    @CTransformer(SCalculator.class)
    private static class SInvalidTestTransformer {

        @CRedirect(method = "add", target = @CTarget("HEAD"))
        public static int add(final int i1, final int i2) {
            return i1 - i2;
        }

    }

    @CTransformer(SCalculator.class)
    private static class SInvokeTestTransformer {

        @CRedirect(method = "rint", target = @CTarget(value = "INVOKE", target = "Ljava/util/Random;nextInt()I"))
        public static int add(final Random rnd) {
            return 1234;
        }

    }

    @CTransformer(VCalculator.class)
    private static class VInvokeTestTransformer {

        @CRedirect(method = "rint", target = @CTarget(value = "INVOKE", target = "Ljava/util/Random;nextInt()I"))
        public int add(final Random rnd) {
            return 1234;
        }

    }

    @CTransformer(SCalculator.class)
    private static class SGetfieldTestTransformer {

        @CRedirect(method = "getPi", target = @CTarget(value = "GETFIELD", target = "Lnet/lenni0451/classtransform/test/SCalculator;pi:D"))
        public static double getPi() {
            return 1.23;
        }

    }

    @CTransformer(VCalculator.class)
    private static class VGetfieldTestTransformer {

        @CRedirect(method = "getPi", target = @CTarget(value = "GETFIELD", target = "Lnet/lenni0451/classtransform/test/VCalculator;pi:D"))
        public double getPi(final VCalculator vCalculator) {
            return 1.23;
        }

    }

    @CTransformer(SCalculator.class)
    private static class SPutfieldTestTransformer {

        @CShadow
        private static double pi;

        @CRedirect(method = "setPi", target = @CTarget(value = "PUTFIELD", target = "Lnet/lenni0451/classtransform/test/SCalculator;pi:D"))
        public static void setPi(final double pii) {
            pi = 3.21;
        }

    }

    @CTransformer(SCalculator.class)
    private static class VPutfieldTestTransformer {

        @CShadow
        private double pi;

        @CRedirect(method = "setPi", target = @CTarget(value = "PUTFIELD", target = "Lnet/lenni0451/classtransform/test/VCalculator;pi:D"))
        public void setPi(final VCalculator vCalculator, final double pii) {
            pi = 3.21;
        }

    }

    @CTransformer(SCalculator.class)
    private static class SNewTestTransformer {

        @CRedirect(method = "rint", target = @CTarget(value = "NEW", target = "java/util/Random"))
        public static Random add() {
            return new Random(1234);
        }

    }

    @CTransformer(VCalculator.class)
    private static class VNewTestTransformer {

        @CRedirect(method = "rint", target = @CTarget(value = "NEW", target = "java/util/Random"))
        public Random add() {
            return new Random(1234);
        }

    }

}