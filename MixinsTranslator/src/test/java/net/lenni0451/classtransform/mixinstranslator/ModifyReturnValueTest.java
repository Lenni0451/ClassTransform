package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import static org.junit.jupiter.api.Assertions.*;

class ModifyReturnValueTest {

    @ParameterizedTest
    @CsvSource({
            "all, -1, 11",
            "all, 1, 12",
            "tail, -1, 1",
            "tail, 1, 12",
            "slice, -1, 11",
            "slice, 1, 2",
            "overlap, -1, 11",
            "overlap, 1, 12",
            "chain, 1, 24",
            "capture, 5, 7"
    })
    public void executeTranslatedReturnModifier(final String name, final int argument, final int expected) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(ReturnMixin.class.getName()));
        transformer.methods.removeIf(method -> !method.name.equals(name) && !(name.equals("chain") && method.name.equals("all")));
        manager.addTransformer(transformer);
        byte[] bytes = manager.transform(Target.class.getName(), provider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        Class<?> target = new ClassLoader() {
            Class<?> loadTarget() {
                return this.defineClass(Target.class.getName(), bytes, 0, bytes.length);
            }
        }.loadTarget();
        assertEquals(expected, target.getMethod("branch", int.class).invoke(null, argument));
    }


    public static class Target {

        public static int branch(int value) {
            if (value < 0) return 1;
            return 2;
        }

    }

    @Mixin(Target.class)
    public static class ReturnMixin {

        @ModifyReturnValue(method = "branch", at = @At("RETURN"))
        public static int all(int original) {
            return original + 10;
        }

        @ModifyReturnValue(method = "branch", at = @At("TAIL"))
        public static int tail(int original) {
            return original + 10;
        }

        @ModifyReturnValue(method = "branch", at = @At("RETURN"), slice = @Slice(to = @At(value = "RETURN", ordinal = 0)))
        public static int slice(int original) {
            return original + 10;
        }

        @ModifyReturnValue(method = "branch", at = {@At("RETURN"), @At("TAIL")})
        public static int overlap(int original) {
            return original + 10;
        }

        @ModifyReturnValue(method = "branch", at = @At("RETURN"))
        public static int chain(int original) {
            return original * 2;
        }

        @ModifyReturnValue(method = "branch", at = @At("RETURN"))
        public static int capture(int original, int value) {
            return original + value;
        }

    }

}
