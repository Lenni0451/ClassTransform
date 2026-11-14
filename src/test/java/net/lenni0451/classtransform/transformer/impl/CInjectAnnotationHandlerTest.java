package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.transformer.AnnotationHandlerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CInjectAnnotationHandlerTest extends AnnotationHandlerTest {

    private final CInjectAnnotationHandler transformer = new CInjectAnnotationHandler();

    @Test
    @DisplayName("Inject into method selected by descriptor")
    public void injectIntoMethodSelectedByDescriptor() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CInjectAnnotationHandlerTest$SDescTestTransformer");
        this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        assertEquals(100, assertDoesNotThrow(() -> (int) clazz.getDeclaredMethod("add", int.class, int.class).invoke(null, 1, 2)));
    }

    @Test
    @DisplayName("Pass constraint")
    public void passConstraint() {
        this.transformerManager.registerConstraintToken("TEST", 5);
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CInjectAnnotationHandlerTest$SConstraintTestTransformer");
        this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        assertEquals(100, assertDoesNotThrow(() -> (int) clazz.getDeclaredMethod("add", int.class, int.class).invoke(null, 1, 2)));
    }

    @Test
    @DisplayName("Fail constraint")
    public void failConstraint() {
        this.transformerManager.registerConstraintToken("TEST", 5);
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CInjectAnnotationHandlerTest$SFailingConstraintTestTransformer");
        assertThrows(TransformerException.class, () -> this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer));
    }

    @Test
    @DisplayName("Fail descriptor minimum match count")
    public void failDescriptorMinimumMatchCount() throws ClassNotFoundException {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CInjectAnnotationHandlerTest$SDescMinimumTestTransformer");
        assertThrows(TransformerException.class, () -> this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer));
    }


    @CTransformer(SCalculator.class)
    private static class SDescTestTransformer {

        @CInject(desc = @CDesc(value = "add", ret = int.class, args = {int.class, int.class}), target = @CTarget("HEAD"), cancellable = true)
        public static void inject(final int i1, final int i2, final InjectionCallback callback) {
            callback.setReturnValue(100);
        }

    }

    @CTransformer(SCalculator.class)
    private static class SConstraintTestTransformer {

        @CInject(method = "add", target = @CTarget("HEAD"), constraints = "TEST(5)", cancellable = true)
        public static void inject(final int i1, final int i2, final InjectionCallback callback) {
            callback.setReturnValue(100);
        }

    }

    @CTransformer(SCalculator.class)
    private static class SFailingConstraintTestTransformer {

        @CInject(method = "add", target = @CTarget("HEAD"), constraints = "TEST(6)", cancellable = true)
        public static void inject(final int i1, final int i2, final InjectionCallback callback) {
            callback.setReturnValue(100);
        }

    }

    @CTransformer(SCalculator.class)
    private static class SDescMinimumTestTransformer {

        @CInject(desc = @CDesc(value = "add", ret = int.class, args = {int.class, int.class}, min = 2), target = @CTarget("HEAD"), cancellable = true)
        public static void inject(final int i1, final int i2, final InjectionCallback callback) {
            callback.setReturnValue(100);
        }

    }

}
