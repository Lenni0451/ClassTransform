package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExpressionTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/ExpressionTest$Target;";

    @Test
    public void modifyMethodCall() throws Exception {
        assertEquals(107, this.transform("call").getMethod("invoke").invoke(null));
    }

    @Test
    public void modifyArrayAccess() throws Exception {
        assertEquals(11, this.transform("array").getMethod("array", int[].class).invoke(null, new int[]{5, 1, 7}));
    }

    @Test
    public void modifyComparison() throws Exception {
        Class<?> target = this.transform("compare");
        assertEquals(20, target.getMethod("compare", int.class, int.class).invoke(null, 1, 2));
        assertEquals(10, target.getMethod("compare", int.class, int.class).invoke(null, 3, 2));
    }

    @Test
    public void modifyCast() throws Exception {
        assertEquals("TEST!", this.transform("cast").getMethod("cast", Object.class).invoke(null, "test"));
    }

    private Class<?> transform(final String... methods) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(ExpressionMixin.class.getName()));
        transformer.methods.removeIf(transformerMethod -> !Arrays.asList(methods).contains(transformerMethod.name));
        manager.addTransformer(transformer);
        byte[] bytes = manager.transform(Target.class.getName(), provider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return new ClassLoader() {
            Class<?> loadTarget() {
                return this.defineClass(Target.class.getName(), bytes, 0, bytes.length);
            }
        }.loadTarget();
    }


    public static class Target {

        public static int compute(final int value) {
            return value * 2;
        }

        public static int invoke() {
            return compute(3) + 1;
        }

        public static int array(final int[] values) {
            return values[1];
        }

        public static int compare(final int left, final int right) {
            return left < right ? 10 : 20;
        }

        public static String cast(final Object value) {
            return ((String) value) + "!";
        }

    }

    @Mixin(Target.class)
    public static class ExpressionMixin {

        @Definition(id = "compute", method = "compute(I)I")
        @Expression("compute(?)")
        @ModifyExpressionValue(method = "invoke", at = @At("MIXINEXTRAS:EXPRESSION"))
        private static int call(final int original) {
            return original + 100;
        }

        @Expression("?[?]")
        @ModifyExpressionValue(method = "array", at = @At("MIXINEXTRAS:EXPRESSION"))
        private static int array(final int original) {
            return original + 10;
        }

        @Expression("? < ?")
        @ModifyExpressionValue(method = "compare", at = @At("MIXINEXTRAS:EXPRESSION"))
        private static boolean compare(final boolean original) {
            return !original;
        }

        @Definition(id = "String", type = String.class)
        @Expression("(String) ?")
        @ModifyExpressionValue(method = "cast", at = @At("MIXINEXTRAS:EXPRESSION"))
        private static String cast(final String original) {
            return original.toUpperCase();
        }

    }

}
