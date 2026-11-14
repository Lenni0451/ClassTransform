package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import static org.junit.jupiter.api.Assertions.*;

class ShareSugarTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/ShareSugarTest$Target;";

    @Test
    public void shareBetweenHandlers() throws Exception {
        assertEquals(4, this.transform(ShareMixin.class).getMethod("shared", int.class).invoke(null, 3));
    }

    @Test
    public void defaultValueIsAvailable() throws Exception {
        assertEquals(0, this.transform(ShareMixin.class).getMethod("defaultValue").invoke(null));
    }

    @Test
    public void namespaceSharesAcrossMixins() throws Exception {
        assertEquals(13, this.transform(NamespaceWriter.class, NamespaceReader.class).getMethod("namespaceShare", int.class).invoke(null, 3));
    }

    @Test
    public void defaultNamespaceIsIsolated() throws Exception {
        assertEquals(0, this.transform(IsolatedWriter.class, IsolatedReader.class).getMethod("isolatedShare", int.class).invoke(null, 3));
    }

    private Class<?> transform(final Class<?>... mixinClasses) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        for (Class<?> mixinClass : mixinClasses) {
            ClassNode transformer = ASMUtils.fromBytes(provider.getClass(mixinClass.getName()));
            manager.addTransformer(transformer);
        }
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

        public static int shared(final int value) {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

        public static int defaultValue() {
            int seen = 5;
            consume();
            return seen;
        }

        public static int namespaceShare(final int value) {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

        public static int isolatedShare(final int value) {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

    }

    @Mixin(Target.class)
    public static class ShareMixin {

        @Inject(method = "shared", at = @At(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@Share("key") final LocalIntRef ref, final int value) {
            ref.set(value + 1);
        }

        @Inject(method = "shared", at = @At(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@Share("key") final LocalIntRef ref, @Local(name = "seen") final LocalIntRef seen) {
            seen.set(ref.get());
        }

        @Inject(method = "defaultValue", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void defaultValue(@Share("default") final LocalIntRef ref, @Local(name = "seen") final LocalIntRef seen) {
            seen.set(ref.get());
        }

    }

    @Mixin(Target.class)
    public static class NamespaceWriter {

        @Inject(method = "namespaceShare", at = @At(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@Share(value = "ns", namespace = "shared") final LocalIntRef ref, final int value) {
            ref.set(value + 10);
        }

    }

    @Mixin(Target.class)
    public static class NamespaceReader {

        @Inject(method = "namespaceShare", at = @At(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@Share(value = "ns", namespace = "shared") final LocalIntRef ref, @Local(name = "seen") final LocalIntRef seen) {
            seen.set(ref.get());
        }

    }

    @Mixin(Target.class)
    public static class IsolatedWriter {

        @Inject(method = "isolatedShare", at = @At(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@Share("isolated") final LocalIntRef ref, final int value) {
            ref.set(value + 10);
        }

    }

    @Mixin(Target.class)
    public static class IsolatedReader {

        @Inject(method = "isolatedShare", at = @At(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@Share("isolated") final LocalIntRef ref, @Local(name = "seen") final LocalIntRef seen) {
            seen.set(ref.get());
        }

    }

}
