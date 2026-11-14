package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class WrapOperationTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/WrapOperationTest$Target;";
    private static final String HOLDER = "Lnet/lenni0451/classtransform/mixinstranslator/WrapOperationTest$Holder;";

    @Test
    public void wrapMethodInvocation() throws Exception {
        Class<?> target = this.transform("invoke");
        target.getField("mode").set(null, 0);
        assertEquals(6, target.getMethod("invoke").invoke(null));
        target.getField("mode").set(null, 1);
        assertEquals(42, target.getMethod("invoke").invoke(null));
        target.getField("mode").set(null, 2);
        assertEquals(8, target.getMethod("invoke").invoke(null));
    }

    @Test
    public void wrapFields() throws Exception {
        Class<?> target = this.transform("fieldRead", "fieldWrite");
        target.getField("mode").set(null, 0);
        assertEquals(5, target.getMethod("fieldRead").invoke(null));
        target.getField("mode").set(null, 1);
        assertEquals(99, target.getMethod("fieldRead").invoke(null));
        target.getField("mode").set(null, 0);
        assertEquals(2, target.getMethod("fieldWrite", int.class).invoke(null, 2));
        target.getField("mode").set(null, 1);
        assertEquals(3, target.getMethod("fieldWrite", int.class).invoke(null, 2));
    }

    @Test
    public void wrapNewAndInstanceof() throws Exception {
        Class<?> target = this.transform("create", "check");
        assertEquals(3L, ((Holder) target.getMethod("create", int.class).invoke(null, 2)).value);
        target.getField("mode").set(null, 1);
        assertEquals(100L, ((Holder) target.getMethod("create", int.class).invoke(null, 2)).value);
        target.getField("mode").set(null, 0);
        assertTrue((boolean) target.getMethod("check", Object.class).invoke(null, "test"));
        target.getField("mode").set(null, 1);
        assertFalse((boolean) target.getMethod("check", Object.class).invoke(null, "test"));
    }

    @Test
    public void wrapRedirectedInvocation() throws Exception {
        assertEquals(101, this.transform("redirected").getMethod("redirected", int.class).invoke(null, 1));
    }

    @Test
    public void wrapWithCancellableAndLocal() throws Exception {
        Class<?> target = this.transform("cancel", "local");
        assertEquals(42, target.getMethod("cancel").invoke(null));
        assertEquals(14, target.getMethod("local").invoke(null));
    }

    @Test
    public void nestedWrappers() throws Exception {
        assertEquals(152, this.transform("nested", "nestedInner", "nestedOuter").getMethod("nested").invoke(null));
    }

    private Class<?> transform(final String... methods) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(WrapMixin.class.getName()));
        transformer.methods.removeIf(transformerMethod -> !Arrays.asList(methods).contains(transformerMethod.name)
                && !transformerMethod.name.equals("redirectCompute"));
        manager.addTransformer(transformer);
        byte[] bytes = manager.transform(Target.class.getName(), provider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return new ClassLoader() {
            Class<?> loadTarget() {
                return this.defineClass(Target.class.getName(), bytes, 0, bytes.length);
            }
        }.loadTarget();
    }


    public static class Holder {

        public long value;

        public Holder(final long value) {
            this.value = value;
        }

        public long value() {
            return this.value;
        }

    }

    public static class Target {

        public static int mode;
        public static int field = 5;

        public static int compute(final int value) {
            return value * 2;
        }

        public static int invoke() {
            return compute(2);
        }

        public static int fieldRead() {
            return field;
        }

        public static int fieldWrite(final int value) {
            field = value;
            return field;
        }

        public static Holder create(final int value) {
            return new Holder(value + 1);
        }

        public static boolean check(final Object object) {
            return object instanceof String;
        }

        public static int redirected(final int value) {
            return compute(value);
        }

        public static int cancel() {
            return compute(2);
        }

        public static int local() {
            int local = 2;
            return compute(local);
        }

        public static int nested() {
            return compute(2);
        }

    }

    @Mixin(Target.class)
    public static class WrapMixin {

        @Shadow
        private static int mode;

        @WrapOperation(method = "invoke", at = @At(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int invoke(final int value, final Operation<Integer> original) {
            if (mode == 1) return 42;
            if (mode == 2) return original.call(value) + original.call(value);
            return original.call(value + 1);
        }

        @WrapOperation(method = "fieldRead", at = @At(value = "FIELD", target = TARGET + "field:I", opcode = Opcodes.GETSTATIC))
        private static int fieldRead(final Operation<Integer> original) {
            return mode == 1 ? 99 : original.call();
        }

        @WrapOperation(method = "fieldWrite", at = @At(value = "FIELD", target = TARGET + "field:I", opcode = Opcodes.PUTSTATIC))
        private static void fieldWrite(final int value, final Operation<Void> original) {
            original.call(mode == 1 ? value + 1 : value);
        }

        @WrapOperation(method = "create", at = @At(value = "NEW", target = HOLDER))
        private static Holder create(final long value, final Operation<Holder> original) {
            if (mode == 1) return new Holder(100);
            return original.call(value);
        }

        @WrapOperation(method = "check", constant = @Constant(classValue = String.class))
        private static boolean check(final Object object, final Operation<Boolean> original) {
            return mode == 0 && original.call(object);
        }

        @Redirect(method = "redirected", at = @At(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int redirectCompute(final int value) {
            return value + 100;
        }

        @WrapOperation(method = "redirected", at = @At(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int redirected(final int value, final Operation<Integer> original) {
            return original.call(value);
        }

        @WrapOperation(method = "cancel", at = @At(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int cancel(final int value, final Operation<Integer> original, @Cancellable final CallbackInfoReturnable<Integer> callback) {
            callback.setReturnValue(42);
            return original.call(value);
        }

        @WrapOperation(method = "local", at = @At(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int local(final int value, final Operation<Integer> original, @Local(name = "local") final LocalIntRef local) {
            local.set(local.get() + 5);
            return original.call(local.get());
        }

        @WrapOperation(method = "nested", at = @At(value = "INVOKE", target = TARGET + "compute(I)I"), order = 100)
        private static int nestedInner(final int value, final Operation<Integer> original) {
            return original.call(value + 1) * 2;
        }

        @WrapOperation(method = "nested", at = @At(value = "INVOKE", target = TARGET + "compute(I)I"), order = 200)
        private static int nestedOuter(final int value, final Operation<Integer> original) {
            return original.call(value + 10) + 100;
        }

    }

}
