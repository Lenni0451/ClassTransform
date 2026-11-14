package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Args;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductionMixinsIntegrationTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/ProductionMixinsIntegrationTest$ShopTarget;";

    @Test
    public void checkoutPipelineCombinesMixinsFeatures() throws Exception {
        Class<?> target = this.transform("priceModifier", "priceWrapper", "quantityModifier", "auditCondition", "checkoutReturn");
        assertEquals(71, target.getMethod("checkout", int.class, int.class).invoke(null, 10, 3));
        assertEquals(4, target.getField("audit").get(null));
        assertEquals(1, target.getField("events").get(null));
        assertEquals(1, target.getField("conditions").get(null));
        assertEquals(0, target.getField("checkoutAudit").get(null));
    }

    @Test
    public void argumentAndExpressionPipeline() throws Exception {
        Class<?> target = this.transform("quantityArg", "purchaseArgs", "priceExpression");
        assertEquals(40, target.getMethod("checkout", int.class, int.class).invoke(null, 10, 2));
        assertEquals(4, target.getField("audit").get(null));
    }

    @Test
    public void coercedReturnValueIsAccepted() throws Exception {
        assertEquals("SHOP", this.transform("coercedName").getMethod("name").invoke(null));
    }

    @Test
    public void modifyArgsUsesMixinArgsApi() throws Exception {
        assertEquals(33, this.transform("modifySumArgs").getMethod("callArgs", int.class, int.class).invoke(null, 1, 2));
    }

    private Class<?> transform(final String... methods) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(ShopMixin.class.getName()));
        transformer.methods.removeIf(method -> !Arrays.asList(methods).contains(method.name));
        manager.addTransformer(transformer);
        byte[] bytes = manager.transform(ShopTarget.class.getName(), provider.getClass(ShopTarget.class.getName()));
        assertNotNull(bytes);
        return new ClassLoader() {
            Class<?> loadTarget() {
                return this.defineClass(ShopTarget.class.getName(), bytes, 0, bytes.length);
            }
        }.loadTarget();
    }


    public static class ShopTarget {

        public static int audit;
        public static int events;
        public static int conditions;
        public static int checkoutAudit;

        public static int price(final int unit, final int quantity) {
            return unit * quantity;
        }

        public static boolean purchase(final String item, final int quantity) {
            if (quantity <= 0) throw new IllegalArgumentException("quantity");
            audit += quantity;
            events++;
            return quantity <= 64;
        }

        public static int checkout(final int unit, final int quantity) {
            int total = price(unit, quantity);
            auditCheckout(total);
            if (!purchase("item", quantity)) return -1;
            return total;
        }

        public static void auditCheckout(final int value) {
            checkoutAudit = value;
        }

        public static String name() {
            return "shop";
        }

        public static int callArgs(final int first, final int second) {
            return sumArgs(first, second);
        }

        public static int sumArgs(final int first, final int second) {
            return first + second;
        }

    }

    @Mixin(ShopTarget.class)
    public static class ShopMixin {

        @ModifyReturnValue(method = "price", at = @At("RETURN"))
        public static int priceModifier(final int original) {
            return original * 2;
        }

        @WrapOperation(method = "checkout", at = @At(value = "INVOKE", target = TARGET + "price(II)I"))
        public static int priceWrapper(final int unit, final int quantity, final Operation<Integer> original) {
            return original.call(unit + 1, quantity);
        }

        @ModifyVariable(method = "purchase", at = @At("HEAD"), index = 1)
        public static int quantityModifier(final int quantity) {
            return quantity + 1;
        }

        @WrapWithCondition(method = "checkout", at = @At(value = "INVOKE", target = TARGET + "auditCheckout(I)V"))
        public static boolean auditCondition(final int value) {
            ShopTarget.conditions++;
            return false;
        }

        @ModifyReturnValue(method = "checkout", at = @At("RETURN"))
        public static int checkoutReturn(final int original) {
            return original + 5;
        }

        @ModifyArg(method = "checkout", at = @At(value = "INVOKE", target = TARGET + "price(II)I"), index = 1)
        public static int quantityArg(final int quantity) {
            return quantity + 1;
        }

        @ModifyArgs(method = "checkout", at = @At(value = "INVOKE", target = TARGET + "purchase(Ljava/lang/String;I)Z"))
        public static void purchaseArgs(final Args args) {
            args.set(1, (Integer) args.get(1) + 2);
        }

        @ModifyArgs(method = "callArgs", at = @At(value = "INVOKE", target = TARGET + "sumArgs(II)I"))
        public static void modifySumArgs(final Args args) {
            Object[] values = args.getAll();
            if (args.size() != 2 || args.getType(0) == null) {
                throw new IllegalStateException("Unexpected argument metadata");
            }
            args.set(0, (Integer) values[0] + 10);
            args.set(1, (Integer) values[1] + 20);
        }

        @Expression("? * ?")
        @ModifyExpressionValue(method = "price", at = @At("MIXINEXTRAS:EXPRESSION"))
        public static int priceExpression(final int original) {
            return original + 10;
        }

        @Coerce
        @ModifyReturnValue(method = "name", at = @At("RETURN"))
        public static Object coercedName(final String original) {
            return original.toUpperCase();
        }

    }

}
