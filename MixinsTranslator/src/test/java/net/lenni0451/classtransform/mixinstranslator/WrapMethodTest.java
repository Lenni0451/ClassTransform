package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrapMethodTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/WrapMethodTest$Target;";

    @Test
    public void wrapMethod() throws Exception {
        Class<?> target = this.transform("invoke");
        target.getField("mode").set(null, 0);
        assertEquals(14, target.getMethod("invoke").invoke(null));
        target.getField("mode").set(null, 1);
        assertEquals(42, target.getMethod("invoke").invoke(null));
        target.getField("mode").set(null, 2);
        assertEquals(8, target.getMethod("invoke").invoke(null));
    }

    @Test
    public void changeArguments() throws Exception {
        assertEquals(12, this.transform("change").getMethod("change", int.class).invoke(null, 1));
    }

    @Test
    public void wrapVoidMethod() throws Exception {
        Class<?> target = this.transform("take");
        target.getField("calls").set(null, 0);
        target.getMethod("take", int.class).invoke(null, 2);
        assertEquals(5, target.getField("calls").get(null));
    }

    @Test
    public void wrapInstanceMethod() throws Exception {
        Class<?> target = this.transform("instance");
        Object instance = target.getConstructor().newInstance();
        assertEquals(3, target.getMethod("instance", int.class).invoke(instance, 1));
    }

    @Test
    public void shareBetweenWrapperAndBody() throws Exception {
        Class<?> target = this.transform("inject", "shared");
        assertEquals(45, target.getMethod("shared", int.class).invoke(null, 1));
        assertEquals(42, target.getField("sharedObserved").get(null));
        assertEquals(43, target.getField("sharedAfter").get(null));
    }

    @Test
    public void nestedWrappersWithShare() throws Exception {
        Class<?> target = this.transform("injectNested", "nestedInner", "nestedOuter");
        assertEquals(102, target.getMethod("nested", int.class).invoke(null, 1));
        assertEquals(10, target.getField("innerSeen").get(null));
        assertEquals(12, target.getField("outerAfter").get(null));
        assertEquals(11, target.getField("bodyObserved").get(null));
    }

    @Test
    public void wrapSynchronizedMethod() throws Exception {
        Class<?> target = this.transform("synced");
        assertEquals(3, target.getMethod("synced", int.class).invoke(null, 2));
        assertTrue(Modifier.isSynchronized(target.getMethod("synced", int.class).getModifiers()));
    }

    private Class<?> transform(final String... methods) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(WrapMixin.class.getName()));
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

        public static int mode;
        public static int calls;
        public static int sharedObserved;
        public static int sharedAfter;
        public static int innerSeen;
        public static int outerAfter;
        public static int bodyObserved;

        public static int invoke() {
            calls++;
            return 4;
        }

        public static int change(final int value) {
            return value + 1;
        }

        public static void take(final int value) {
            calls += value;
        }

        public int instance(final int value) {
            return value + 1;
        }

        public static int shared(final int value) {
            return value + 1;
        }

        public static int nested(final int value) {
            return value + 1;
        }

        public static synchronized int synced(final int value) {
            return value + 1;
        }

    }

    @Mixin(Target.class)
    public static class WrapMixin {

        @Shadow
        private static int mode;
        @Shadow
        private static int calls;
        @Shadow
        private static int sharedObserved;
        @Shadow
        private static int sharedAfter;
        @Shadow
        private static int innerSeen;
        @Shadow
        private static int outerAfter;
        @Shadow
        private static int bodyObserved;

        @WrapMethod(method = "invoke")
        private static int invoke(final Operation<Integer> original) {
            if (mode == 1) return 42;
            if (mode == 2) return original.call() + original.call();
            return original.call() + 10;
        }

        @WrapMethod(method = "change")
        private static int change(final int value, final Operation<Integer> original) {
            return original.call(value + 10);
        }

        @WrapMethod(method = "take")
        private static void take(final int value, final Operation<Void> original) {
            original.call(value);
            original.call(value + 1);
        }

        @WrapMethod(method = "instance")
        private int instance(final int value, final Operation<Integer> original) {
            return original.call(value + 1);
        }

        @Inject(method = "shared", at = @At("HEAD"))
        private static void inject(@Share("value") final LocalIntRef ref) {
            sharedObserved = ref.get();
            ref.set(ref.get() + 1);
        }

        @WrapMethod(method = "shared")
        private static int shared(final int value, final Operation<Integer> original, @Share("value") final LocalIntRef ref) {
            ref.set(42);
            int result = original.call(value);
            sharedAfter = ref.get();
            return result + ref.get();
        }

        @Inject(method = "nested", at = @At("HEAD"))
        private static void injectNested(@Share("value") final LocalIntRef ref) {
            bodyObserved = ref.get();
            ref.set(ref.get() + 1);
        }

        @WrapMethod(method = "nested", order = 100)
        private static int nestedInner(final int value, final Operation<Integer> original, @Share("value") final LocalIntRef ref) {
            innerSeen = ref.get();
            ref.set(ref.get() + 1);
            return original.call(value);
        }

        @WrapMethod(method = "nested", order = 200)
        private static int nestedOuter(final int value, final Operation<Integer> original, @Share("value") final LocalIntRef ref) {
            ref.set(10);
            int result = original.call(value);
            outerAfter = ref.get();
            return result + 100;
        }

        @WrapMethod(method = "synced")
        private static int synced(final int value, final Operation<Integer> original) {
            return original.call(value);
        }

    }

}
