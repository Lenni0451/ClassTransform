package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.transformer.AnnotationHandlerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.*;

public class CModifyConstantAnnotationHandlerTest extends AnnotationHandlerTest {

    private final CModifyConstantAnnotationHandler transformer = new CModifyConstantAnnotationHandler();

    @Test
    @DisplayName("ModifyConstant invalid target")
    public void modifyConstantInvalidTarget() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CModifyConstantAnnotationHandlerTest$SInvalidTestTransformer");
        assertThrows(TransformerException.class, () -> this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer));
    }

    @Test
    @DisplayName("ModifyConstant static string")
    public void modifyConstantStaticString() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CModifyConstantAnnotationHandlerTest$SRedirectStringTestTransformer");
        this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        assertThrows(ArithmeticException.class, () -> {
            try {
                clazz.getDeclaredMethod("divide", double.class, double.class).invoke(null, 0D, 0D);
            } catch (Throwable t) {
                throw t.getCause();
            }
        }, "/0?");
    }

    @Test
    @DisplayName("ModifyConstant virtual string")
    public void modifyConstantVirtualString() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CModifyConstantAnnotationHandlerTest$VRedirectStringTestTransformer");
        this.transformer.transform(this.transformerManager, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        assertThrows(ArithmeticException.class, () -> {
            try {
                clazz.getDeclaredMethod("divide", double.class, double.class).invoke(instance, 0D, 0D);
            } catch (Throwable t) {
                throw t.getCause();
            }
        }, "/0?");
    }

    @Test
    @DisplayName("ModifyConstant static int")
    public void modifyConstantStaticInt() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CModifyConstantAnnotationHandlerTest$SRedirectDoubleTestTransformer");
        this.transformer.transform(this.transformerManager, this.staticCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.staticCalculatorClass);
        double pow = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("pow2", int.class).invoke(null, 2));
        assertEquals(8, pow);
    }

    @Test
    @DisplayName("ModifyConstant virtual int")
    public void modifyConstantVirtualInt() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CModifyConstantAnnotationHandlerTest$VRedirectDoubleTestTransformer");
        this.transformer.transform(this.transformerManager, this.virtualCalculatorClass, transformer);
        Class<?> clazz = TestClassLoader.load(this.virtualCalculatorClass);
        Object instance = assertDoesNotThrow(() -> clazz.getDeclaredConstructor().newInstance());
        double pow = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("pow2", int.class).invoke(instance, 2));
        assertEquals(8, pow);
    }


    @CTransformer(SCalculator.class)
    private static class SInvalidTestTransformer {

        @CModifyConstant(method = "add", intValue = 132)
        public static int add() {
            return 0;
        }

    }

    @CTransformer(SCalculator.class)
    private static class SRedirectStringTestTransformer {

        @CModifyConstant(method = "divide", stringValue = "Division by zero")
        public static String divide() {
            return "/0?";
        }

    }

    @CTransformer(VCalculator.class)
    private static class VRedirectStringTestTransformer {

        @CModifyConstant(method = "divide", stringValue = "Division by zero")
        public String divide() {
            return "/0?";
        }

    }

    @CTransformer(SCalculator.class)
    private static class SRedirectDoubleTestTransformer {

        @CModifyConstant(method = "pow2", doubleValue = 2)
        public static double pow2() {
            return 3;
        }

    }

    @CTransformer(VCalculator.class)
    private static class VRedirectDoubleTestTransformer {

        @CModifyConstant(method = "pow2", doubleValue = 2)
        public double pow2() {
            return 3;
        }

    }

}
