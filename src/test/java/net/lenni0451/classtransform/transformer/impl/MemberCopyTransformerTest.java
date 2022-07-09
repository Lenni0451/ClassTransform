package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.transformer.ATransformerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberCopyTransformerTest extends ATransformerTest {

    private final MemberCopyTransformer transformer = new MemberCopyTransformer();

    @Test
    @DisplayName("Copy static field")
    public void copyStaticField() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.MemberCopyTransformerTest$StaticMemberCopyTest");
        this.removeShadows(transformer);
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double sPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals(Math.E, sPi);
    }

    @Test
    @DisplayName("Copy virtual field")
    public void copyVirtualField() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.MemberCopyTransformerTest$VirtualMemberCopyTest");
        this.removeShadows(transformer);
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        double vPi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(instance));
        assertEquals(Math.E, vPi);
    }

    private void removeShadows(final ClassNode transformer) {
        CShadowTransformer shadowTransformer = new CShadowTransformer();
        shadowTransformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.staticCalculatorClass, transformer);
        shadowTransformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.virtualCalculatorClass, transformer);
    }


    @CTransformer(SCalculator.class)
    private static class StaticMemberCopyTest {

        @CShadow
        private static double pi = Math.E;

    }

    @CTransformer(VCalculator.class)
    private static class VirtualMemberCopyTest {

        @CShadow
        private double pi = Math.E;

    }

}