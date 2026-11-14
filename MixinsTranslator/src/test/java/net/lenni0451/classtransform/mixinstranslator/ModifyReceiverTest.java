package net.lenni0451.classtransform.mixinstranslator;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ModifyReceiverTest {

    private static final String HOLDER = "Lnet/lenni0451/classtransform/mixinstranslator/ModifyReceiverTest$Holder;";

    @Test
    public void modifyTranslatedReceiver() throws Exception {
        assertEquals(10L, this.transform("call").getMethod("call").invoke(null));
    }

    @Test
    public void modifyTranslatedReceiverWithArguments() throws Exception {
        Class<?> target = this.transform("argument");
        assertEquals(13L, target.getMethod("argument", long.class).invoke(null, 3L));
        assertEquals(1, target.getField("evaluations").get(null));
    }

    @Test
    public void modifyTranslatedFieldReceivers() throws Exception {
        Class<?> target = this.transform("field", "write");
        assertEquals(42L, target.getMethod("field").invoke(null));
        target.getMethod("write", long.class).invoke(null, 7L);
        assertEquals(2L, ((Holder) target.getField("destination").get(null)).value);
    }

    @Test
    public void chainTranslatedReceivers() throws Exception {
        assertEquals(20L, this.transform("first", "second").getMethod("repeated").invoke(null));
    }

    private Class<?> transform(final String... methods) throws Exception {
        BasicClassProvider provider = new BasicClassProvider();
        TransformerManager manager = new TransformerManager(provider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformerPreprocessor(new MixinsTranslator());
        ClassNode transformer = ASMUtils.fromBytes(provider.getClass(ReceiverMixin.class.getName()));
        transformer.methods.removeIf(method -> !Arrays.asList(methods).contains(method.name));
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

        public long add(final long amount) {
            this.value += amount;
            return this.value;
        }

    }

    public static class Target {

        public static Holder first = new Holder(1);
        public static Holder destination = new Holder(2);
        public static int evaluations;

        public static long call() {
            return first.value();
        }

        public static long argument(final long seed) {
            return first.add(evaluate(seed));
        }

        public static long field() {
            return destination.value;
        }

        public static void write(final long value) {
            destination.value = value;
        }

        public static long repeated() {
            return first.value();
        }

        private static long evaluate(final long seed) {
            evaluations++;
            return seed * 2;
        }

    }

    @Mixin(Target.class)
    public static class ReceiverMixin {

        @ModifyReceiver(method = "call", at = @At(value = "INVOKE", target = HOLDER + "value()J"))
        private static Holder call(final Holder original) {
            return new Holder(10);
        }

        @ModifyReceiver(method = "argument", at = @At(value = "INVOKE", target = HOLDER + "add(J)J"))
        private static Holder argument(final Holder original, final long amount) {
            return new Holder(original.value + amount);
        }

        @ModifyReceiver(method = "field", at = @At(value = "FIELD", target = HOLDER + "value:J", opcode = Opcodes.GETFIELD))
        private static Holder field(final Holder original) {
            return new Holder(42);
        }

        @ModifyReceiver(method = "write", at = @At(value = "FIELD", target = HOLDER + "value:J", opcode = Opcodes.PUTFIELD))
        private static Holder write(final Holder original, final long value) {
            return new Holder(original.value);
        }

        @ModifyReceiver(method = "repeated", at = @At(value = "INVOKE", target = HOLDER + "value()J"), order = 100)
        private static Holder first(final Holder original) {
            return new Holder(original.value + 1);
        }

        @ModifyReceiver(method = "repeated", at = @At(value = "INVOKE", target = HOLDER + "value()J"), order = 200)
        private static Holder second(final Holder original) {
            return new Holder(original.value * 10);
        }

    }

}
