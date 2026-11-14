package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CInjectHandlerVariantsTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CInjectAnnotationHandler handler = new CInjectAnnotationHandler();

    @Test
    public void passBooleanArgument() throws Exception {
        assertEquals(false, this.transform("booleanArg").getMethod("booleanArg", boolean.class).invoke(null, true));
    }

    @Test
    public void passByteArgument() throws Exception {
        assertEquals((byte) 11, this.transform("byteArg").getMethod("byteArg", byte.class).invoke(null, (byte) 1));
    }

    @Test
    public void passShortArgument() throws Exception {
        assertEquals((short) 11, this.transform("shortArg").getMethod("shortArg", short.class).invoke(null, (short) 1));
    }

    @Test
    public void passCharArgument() throws Exception {
        assertEquals('k', this.transform("charArg").getMethod("charArg", char.class).invoke(null, 'a'));
    }

    @Test
    public void passLongArgument() throws Exception {
        assertEquals(11L, this.transform("longArg").getMethod("longArg", long.class).invoke(null, 1L));
    }

    @Test
    public void passFloatArgument() throws Exception {
        assertEquals(11F, this.transform("floatArg").getMethod("floatArg", float.class).invoke(null, 1F));
    }

    @Test
    public void passDoubleArgument() throws Exception {
        assertEquals(11D, this.transform("doubleArg").getMethod("doubleArg", double.class).invoke(null, 1D));
    }

    @Test
    public void passCallbackOnlyForMethodWithArguments() throws Exception {
        assertEquals(99, this.transform("callbackOnly").getMethod("callbackOnly", int.class).invoke(null, 1));
    }

    @Test
    public void cancelVoidMethodAtHead() throws Exception {
        Class<?> target = this.transform("voidHead");
        target.getField("sink").set(null, -1);
        target.getMethod("voidHead", int.class).invoke(null, 7);
        assertEquals(-1, target.getField("sink").get(null));
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void rejectIncompatibleHandler() {
        assertThrows(TransformerException.class, () -> this.transform("invalid"));
    }

    private Class<?> transform(final String name) throws Exception {
        ClassNode target = this.read(Target.class);
        ClassNode transformer = this.read(VariantTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        this.handler.transform(this.transformerManager, target, transformer);
        return TestClassLoader.load(target);
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class Target {

        public static int calls;
        public static int sink;

        public static boolean booleanArg(final boolean value) {
            return !value;
        }

        public static byte byteArg(final byte value) {
            return (byte) (value + 1);
        }

        public static short shortArg(final short value) {
            return (short) (value + 1);
        }

        public static char charArg(final char value) {
            return (char) (value + 1);
        }

        public static int intArg(final int value) {
            return value + 1;
        }

        public static long longArg(final long value) {
            return value + 1;
        }

        public static float floatArg(final float value) {
            return value + 1;
        }

        public static double doubleArg(final double value) {
            return value + 1;
        }

        public static int callbackOnly(final int value) {
            return value + 1;
        }

        public static void voidHead(final int value) {
            sink = value;
        }

    }

    public static class VariantTransformer {

        @CInject(method = "booleanArg", target = @CTarget("HEAD"), cancellable = true)
        public static void booleanArg(final boolean value, final InjectionCallback callback) {
            callback.setReturnValue(!value);
        }

        @CInject(method = "byteArg", target = @CTarget("HEAD"), cancellable = true)
        public static void byteArg(final byte value, final InjectionCallback callback) {
            callback.setReturnValue((byte) (value + 10));
        }

        @CInject(method = "shortArg", target = @CTarget("HEAD"), cancellable = true)
        public static void shortArg(final short value, final InjectionCallback callback) {
            callback.setReturnValue((short) (value + 10));
        }

        @CInject(method = "charArg", target = @CTarget("HEAD"), cancellable = true)
        public static void charArg(final char value, final InjectionCallback callback) {
            callback.setReturnValue((char) (value + 10));
        }

        @CInject(method = "intArg", target = @CTarget("HEAD"), cancellable = true)
        public static void intArg(final int value, final InjectionCallback callback) {
            callback.setReturnValue(value + 10);
        }

        @CInject(method = "longArg", target = @CTarget("HEAD"), cancellable = true)
        public static void longArg(final long value, final InjectionCallback callback) {
            callback.setReturnValue(value + 10L);
        }

        @CInject(method = "floatArg", target = @CTarget("HEAD"), cancellable = true)
        public static void floatArg(final float value, final InjectionCallback callback) {
            callback.setReturnValue(value + 10F);
        }

        @CInject(method = "doubleArg", target = @CTarget("HEAD"), cancellable = true)
        public static void doubleArg(final double value, final InjectionCallback callback) {
            callback.setReturnValue(value + 10D);
        }

        @CInject(method = "callbackOnly", target = @CTarget("HEAD"), cancellable = true)
        public static void callbackOnly(final InjectionCallback callback) {
            callback.setReturnValue(99);
        }

        @CInject(method = "voidHead", target = @CTarget("HEAD"), cancellable = true)
        public static void voidHead(final int value, final InjectionCallback callback) {
            Target.calls++;
            callback.cancel();
        }

        @CInject(method = "intArg", target = @CTarget("HEAD"), cancellable = true)
        public static void invalid(final long value) {
            Target.calls++;
        }

    }

}
