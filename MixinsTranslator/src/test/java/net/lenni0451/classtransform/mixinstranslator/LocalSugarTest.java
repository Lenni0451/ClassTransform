package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalLongRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LocalSugarTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/LocalSugarTest$Target;";

    @Test
    public void modifyPrimitiveLocal() throws Exception {
        assertEquals(2, this.transform("reference").getMethod("reference").invoke(null));
    }

    @Test
    public void ordinaryLocalDoesNotWriteBack() throws Exception {
        assertEquals(3, this.transform("ordinary").getMethod("ordinary").invoke(null));
    }

    @Test
    public void localSelectionParameters() throws Exception {
        Class<?> target = this.transform("argsOnly", "names", "ordinal", "indexed");
        assertEquals(905, target.getMethod("argsOnly", int.class).invoke(null, 2));
        assertEquals(4, target.getMethod("names").invoke(null));
        assertEquals(13, target.getMethod("ordinal").invoke(null));
        assertEquals(6, target.getMethod("indexed").invoke(null));
    }

    @Test
    public void primitiveAndObjectReferences() throws Exception {
        Class<?> target = this.transform("primitive", "helperRef");
        assertEquals(5L, target.getMethod("primitive").invoke(null));
        assertEquals("hey", target.getMethod("helperRef").invoke(null));
    }

    private Class<?> transform(final String... methods) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(LocalMixin.class.getName()));
        transformer.methods.removeIf(method -> !Arrays.asList(methods).contains(method.name) && !method.name.equals("helper"));
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

        public static void consume() {
        }

        public static int reference() {
            int local = 1;
            consume();
            return local;
        }

        public static int ordinary() {
            String text = "abc";
            consume();
            return text.length();
        }

        public static int argsOnly(final int first) {
            int local = 5;
            consume();
            return first * 100 + local;
        }

        public static int names() {
            int actual = 3;
            consume();
            return actual;
        }

        public static int ordinal() {
            int first = 1;
            int second = 2;
            consume();
            return first * 10 + second;
        }

        public static int indexed() {
            int local = 5;
            consume();
            return local;
        }

        public static long primitive() {
            long local = 1L;
            consume();
            return local;
        }

        public static String helperRef() {
            String text = "a";
            consume();
            return text;
        }

    }

    @Mixin(Target.class)
    public static class LocalMixin {

        @Inject(method = "reference", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void reference(@Local final LocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @Inject(method = "ordinary", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void ordinary(@Local String text) {
            text = "changed";
        }

        @Inject(method = "argsOnly", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void argsOnly(@Local(argsOnly = true) final LocalIntRef ref) {
            ref.set(9);
        }

        @Inject(method = "names", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void names(@Local(name = {"missing", "actual"}) final LocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @Inject(method = "ordinal", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void ordinal(@Local(ordinal = 1) final LocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @Inject(method = "indexed", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void indexed(@Local(index = 0) final LocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @Inject(method = "primitive", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void primitive(@Local final LocalLongRef ref) {
            ref.set(5L);
        }

        @Inject(method = "helperRef", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void helperRef(@Local final LocalRef<String> ref) {
            helper(ref);
        }

        private static void helper(final LocalRef<String> ref) {
            ref.set("hey");
        }

    }

}
