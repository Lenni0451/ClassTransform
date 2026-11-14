package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionInjectionTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/mixinstranslator/ExpressionInjectionTest$Target;";

    @ParameterizedTest
    @CsvSource({"constants, 13", "overlap, 8", "slice, 8", "invocation, 13"})
    public void modifyExpressionsWithCapturedArguments(final String name, final int expected) throws Exception {
        assertEquals(expected, this.transform(name).getMethod("values", long.class).invoke(null, 5L));
    }

    @Test
    public void modifyNullConstant() throws Exception {
        assertEquals("modified", this.transform("nullValue").getMethod("nullValue").invoke(null));
    }

    @Test
    public void modifyClassConstant() throws Exception {
        assertEquals(Integer.class, this.transform("classValue").getMethod("classValue").invoke(null));
    }

    @Test
    public void modifyEmptyStringConstant() throws Exception {
        assertEquals("modified", this.transform("emptyString").getMethod("emptyString").invoke(null));
    }

    @ParameterizedTest
    @CsvSource({"true, 12", "false, 0"})
    public void wrapInvocationWithCapturedArguments(final boolean enabled, final long expected) throws Exception {
        Class<?> target = this.transform("condition");
        assertEquals(expected, target.getMethod("call", boolean.class, long.class, int.class, double.class).invoke(null, enabled, 3L, 4, 5D));
    }

    @ParameterizedTest
    @CsvSource({"true, 9", "false, 0"})
    public void wrapFieldWithCapturedArguments(final boolean enabled, final long expected) throws Exception {
        Class<?> target = this.transform("field");
        assertEquals(expected, target.getMethod("field", boolean.class, long.class).invoke(null, enabled, 9L));
    }

    @Test
    public void chainTranslatedConditions() throws Exception {
        Class<?> target = this.transform("chain");
        assertEquals(0L, target.getMethod("call", boolean.class, long.class, int.class, double.class).invoke(null, true, 3L, 4, 5D));
        assertEquals(2, target.getField("checks").get(null));
    }

    private Class<?> transform(final String name) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(ExpressionMixin.class.getName()));
        transformer.methods.removeIf(method -> !method.name.equals(name) && !(name.equals("chain") && method.name.equals("condition")));
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

        public static long result;
        public static int checks;

        public static int values(long bonus) {
            return Math.abs(1) + Math.abs(2);
        }

        public static String nullValue() {
            return null;
        }

        public static Class<?> classValue() {
            return String.class;
        }

        public static String emptyString() {
            return "";
        }

        public static long call(boolean enabled, long first, int second, double third) {
            consume(first, second, third);
            return result;
        }

        public static void consume(long first, int second, double third) {
            result = first + second + (long) third;
        }

        public static long field(boolean enabled, long value) {
            result = value;
            return result;
        }

    }

    @Mixin(Target.class)
    public static class ExpressionMixin {

        @ModifyExpressionValue(method = "values", at = {@At(value = "CONSTANT", args = "intValue=1"), @At(value = "CONSTANT", args = "intValue=2")})
        public static int constants(int original, long bonus) {
            return original + (int) bonus;
        }

        @ModifyExpressionValue(method = "values", at = {@At(value = "CONSTANT", args = "intValue=1"), @At(value = "CONSTANT", args = "intValue=1")})
        public static int overlap(int original, long bonus) {
            return original + (int) bonus;
        }

        @ModifyExpressionValue(method = "values", at = @At(value = "CONSTANT", args = "intValue=2"), slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", ordinal = 0)))
        public static int slice(int original, long bonus) {
            return original + (int) bonus;
        }

        @ModifyExpressionValue(method = "values", at = @At(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        public static int invocation(int original, long bonus) {
            return original + (int) bonus;
        }

        @ModifyExpressionValue(method = "nullValue", at = @At(value = "CONSTANT", args = "nullValue=true"))
        public static String nullValue(String original) {
            return original == null ? "modified" : original;
        }

        @ModifyExpressionValue(method = "classValue", at = @At(value = "CONSTANT", args = "classValue=java.lang.String"))
        public static Class<?> classValue(Class<?> original) {
            return original == String.class ? Integer.class : original;
        }

        @ModifyExpressionValue(method = "emptyString", at = @At(value = "CONSTANT", args = "stringValue="))
        public static String emptyString(String original) {
            return original.isEmpty() ? "modified" : original;
        }

        @WrapWithCondition(method = "call", at = @At(value = "INVOKE", target = TARGET + "consume(JID)V"))
        public static boolean condition(long first, int second, double third, boolean enabled, long outerFirst, int outerSecond, double outerThird) {
            Target.checks++;
            return enabled && first == outerFirst && second == outerSecond && third == outerThird;
        }

        @WrapWithCondition(method = "field", at = @At(value = "FIELD", target = TARGET + "result:J", ordinal = 0))
        public static boolean field(long value, boolean enabled) {
            return enabled;
        }

        @WrapWithCondition(method = "call", at = @At(value = "INVOKE", target = TARGET + "consume(JID)V"))
        public static boolean chain(long first, int second, double third) {
            Target.checks++;
            return false;
        }

    }

}
