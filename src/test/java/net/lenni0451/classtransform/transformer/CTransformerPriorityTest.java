package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.COverride;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CTransformerPriorityTest extends AnnotationHandlerTest {

    @Test
    @DisplayName("Higher priority is applied first")
    public void higherPriorityIsAppliedFirst() throws Exception {
        ClassNode low = ASMUtils.fromBytes(this.classProvider.getClass(LowPriorityTransformer.class.getName()));
        ClassNode high = ASMUtils.fromBytes(this.classProvider.getClass(HighPriorityTransformer.class.getName()));
        this.transformerManager.addTransformer(low);
        this.transformerManager.addTransformer(high);

        byte[] transformed = this.transformerManager.transform(SCalculator.class.getName(), this.classProvider.getClass(SCalculator.class.getName()), false);
        assertNotNull(transformed);
        Class<?> clazz = TestClassLoader.load(ASMUtils.fromBytes(transformed));
        double pi = assertDoesNotThrow(() -> (double) clazz.getDeclaredMethod("getPi").invoke(null));
        assertEquals("LOW".hashCode(), pi);
    }


    @CTransformer(value = SCalculator.class, priority = 2000)
    private static class HighPriorityTransformer {

        @COverride
        public static double getPi() {
            return "HIGH".hashCode();
        }

    }

    @CTransformer(value = SCalculator.class, priority = 1000)
    private static class LowPriorityTransformer {

        @COverride
        public static double getPi() {
            return "LOW".hashCode();
        }

    }

}
