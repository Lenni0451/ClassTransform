package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.junit.jupiter.api.Assertions.*;

class CancellableSugarTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/CancellableSugarTest$Target;";

    @Test
    public void cancellableReturnModifier() throws Exception {
        assertEquals(42, this.transform("returnValue").getMethod("returnValue").invoke(null));
    }

    @Test
    public void cancellableExpressionModifier() throws Exception {
        assertEquals(0, this.transform("expressionValue").getMethod("expressionValue").invoke(null));
    }

    @Test
    public void cancellableRedirect() throws Exception {
        assertEquals(42, this.transform("redirectValue").getMethod("redirectValue").invoke(null));
    }

    @Test
    public void cancellableCondition() throws Exception {
        Class<?> target = this.transform("condition");
        target.getMethod("condition").invoke(null);
        assertEquals(0, target.getField("effects").get(null));
    }

    @Test
    public void callbackInfoReturnableMethods() throws Exception {
        Class<?> target = this.transform("primitive");
        assertEquals(7, target.getMethod("primitive").invoke(null));
    }

    @Test
    public void callbacksCombineWithLocalSugar() throws Exception {
        assertEquals(4, this.transform("combined").getMethod("combined", int.class).invoke(null, 3));
    }

    @Test
    public void callbackInfoReturnableObjectMethods() throws Exception {
        assertEquals("cancelled", this.transform("object").getMethod("object").invoke(null));
    }

    private Class<?> transform(final String method) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(CancellableMixin.class.getName()));
        transformer.methods.removeIf(transformerMethod -> !transformerMethod.name.equals(method) && !transformerMethod.name.equals("setReturnValue"));
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

        public static int effects;

        public static void effect() {
            effects++;
        }

        public static int returnValue() {
            return 1;
        }

        public static int expressionValue() {
            return Math.abs(2);
        }

        public static int redirectValue() {
            return Math.abs(2);
        }

        public static void condition() {
            effect();
        }

        public static void consume() {
        }

        public static int combined(final int value) {
            consume();
            return 0;
        }

        public static int primitive() {
            return 1;
        }

        public static String object() {
            return "original";
        }

    }

    @Mixin(Target.class)
    public static class CancellableMixin {

        @ModifyReturnValue(method = "returnValue", at = @At("RETURN"))
        private static int returnValue(final int original, @Cancellable final CallbackInfoReturnable<Integer> callback) {
            callback.setReturnValue(42);
            return original + 1;
        }

        @ModifyExpressionValue(method = "expressionValue", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        private static int expressionValue(final int original, @Cancellable final CallbackInfoReturnable<Integer> callback) {
            callback.cancel();
            return original * 10;
        }

        @Redirect(method = "redirectValue", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        private static int redirectValue(final int value, @Cancellable final CallbackInfoReturnable<Integer> callback) {
            if (callback.isCancelled()) return 0;
            callback.setReturnValue(42);
            return 5;
        }

        @WrapWithCondition(method = "condition", at = @At(value = "INVOKE", target = TARGET + "effect()V"))
        private static boolean condition(@Cancellable final CallbackInfo callback) {
            callback.cancel();
            return true;
        }

        @Inject(method = "combined", at = @At(value = "INVOKE", target = TARGET + "consume()V"))
        private static void combined(@Cancellable final CallbackInfoReturnable<Integer> callback, @Local(name = "value") final LocalIntRef value) {
            value.set(value.get() + 1);
            callback.setReturnValue(value.get());
        }

        @Inject(method = "primitive", at = @At("HEAD"))
        private static void primitive(@Cancellable final CallbackInfoReturnable<Integer> callback) {
            if (!callback.isCancellable()) throw new IllegalStateException("callback is not cancellable");
            int value = callback.getReturnValueI();
            setReturnValue(callback, value + 7);
        }

        private static void setReturnValue(final CallbackInfoReturnable<Integer> callback, final int value) {
            callback.setReturnValue(value);
        }

        @Inject(method = "object", at = @At("HEAD"))
        private static void object(@Cancellable final CallbackInfoReturnable<String> callback) {
            String value = callback.getReturnValue();
            callback.setReturnValue(value == null ? "cancelled" : value);
        }

    }

}
