package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalCapture;
import net.lenni0451.classtransform.annotations.CSurrogate;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CInjectReturnTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CInjectAnnotationHandler handler = new CInjectAnnotationHandler();

    @Test
    public void injectAtReturn() throws Exception {
        Class<?> target = this.transform("returnInject");
        assertEquals(105, target.getMethod("returnValue", int.class).invoke(null, 5));
    }

    @Test
    public void injectAtTail() throws Exception {
        Class<?> target = this.transform("tailInject");
        assertEquals(205, target.getMethod("tailValue", int.class).invoke(null, 5));
    }

    @Test
    public void injectAtThrow() throws Exception {
        Class<?> target = this.transform("throwInject");
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> target.getMethod("throwValue", int.class).invoke(null, 7));
        assertEquals("value 7", exception.getCause().getMessage());
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtReturnWithoutCancellation() throws Exception {
        Class<?> target = this.transform("returnWithoutCancellation");
        assertEquals(6, target.getMethod("returnValue", int.class).invoke(null, 5));
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtVoidReturnWithCancellation() throws Exception {
        Class<?> target = this.transform("voidReturnInject");
        target.getMethod("voidReturn", int.class).invoke(null, 5);
        assertEquals(true, target.getField("voidRan").get(null));
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtVoidReturnWithCallback() throws Exception {
        Class<?> target = this.transform("voidReturnCallback");
        target.getMethod("voidReturn", int.class).invoke(null, 5);
        assertEquals(true, target.getField("voidRan").get(null));
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtLongReturn() throws Exception {
        Class<?> target = this.transform("longReturnInject");
        assertEquals(105L, target.getMethod("longReturn", long.class).invoke(null, 5L));
    }

    @Test
    public void injectAtFloatReturn() throws Exception {
        Class<?> target = this.transform("floatReturnInject");
        assertEquals(105F, target.getMethod("floatReturn", float.class).invoke(null, 5F));
    }

    @Test
    public void injectAtDoubleReturn() throws Exception {
        Class<?> target = this.transform("doubleReturnInject");
        assertEquals(105D, target.getMethod("doubleReturn", double.class).invoke(null, 5D));
    }

    @Test
    public void injectAtObjectReturn() throws Exception {
        Class<?> target = this.transform("objectReturnInject");
        assertEquals("hi?", target.getMethod("objectReturn", String.class).invoke(null, "hi"));
    }

    @Test
    public void injectAtThrowWithCallback() throws Exception {
        Class<?> target = this.transform("throwCallback");
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> target.getMethod("throwWithCallback", int.class).invoke(null, 7));
        assertEquals("callback 7", exception.getCause().getMessage());
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtThrowWithCancellation() throws Exception {
        Class<?> target = this.transform("throwReturn");
        assertEquals(57, target.getMethod("throwWithReturn", int.class).invoke(null, 7));
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtThrowWithoutCancellation() throws Exception {
        Class<?> target = this.transform("throwNoCancel");
        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> target.getMethod("throwWithoutCancel", int.class).invoke(null, 7));
        assertEquals("no cancel 7", exception.getCause().getMessage());
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtBooleanReturn() throws Exception {
        Class<?> target = this.transform("booleanReturnInject");
        assertEquals(false, target.getMethod("booleanReturn", boolean.class).invoke(null, false));
    }

    @Test
    public void injectAtByteReturn() throws Exception {
        Class<?> target = this.transform("byteReturnInject");
        assertEquals((byte) 101, target.getMethod("byteReturn", byte.class).invoke(null, (byte) 1));
    }

    @Test
    public void injectAtShortReturn() throws Exception {
        Class<?> target = this.transform("shortReturnInject");
        assertEquals((short) 1001, target.getMethod("shortReturn", short.class).invoke(null, (short) 1));
    }

    @Test
    public void injectAtCharReturn() throws Exception {
        Class<?> target = this.transform("charReturnInject");
        assertEquals('k', target.getMethod("charReturn", char.class).invoke(null, 'a'));
    }

    @Test
    public void injectAtVoidTail() throws Exception {
        Class<?> target = this.transform("voidTailInject");
        target.getMethod("voidTail", boolean.class).invoke(null, false);
        assertEquals(true, target.getField("tailSink").get(null));
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void tailDoesNotModifyEarlyVoidReturn() throws Exception {
        Class<?> target = this.transform("voidTailInject");
        target.getMethod("voidTail", boolean.class).invoke(null, true);
        assertEquals(false, target.getField("tailSink").get(null));
        assertEquals(0, target.getField("calls").get(null));
    }

    @Test
    public void injectAtLongTail() throws Exception {
        Class<?> target = this.transform("tailLongInject");
        assertEquals(105L, target.getMethod("tailLong", long.class).invoke(null, 5L));
        assertEquals(-1L, target.getMethod("tailLong", long.class).invoke(null, -1L));
    }

    @Test
    public void injectAtDoubleTail() throws Exception {
        Class<?> target = this.transform("tailDoubleInject");
        assertEquals(105D, target.getMethod("tailDouble", double.class).invoke(null, 5D));
        assertEquals(-1D, target.getMethod("tailDouble", double.class).invoke(null, -1D));
    }

    @Test
    public void injectAtObjectTail() throws Exception {
        Class<?> target = this.transform("tailObjectInject");
        assertEquals("hi?", target.getMethod("tailObject", String.class).invoke(null, "hi"));
        assertEquals("null", target.getMethod("tailObject", String.class).invoke(null, (Object) null));
    }

    @Test
    public void injectAtThrowWithLocalCapture() throws Exception {
        Class<?> target = this.transform("throwLocal");
        assertEquals(106, target.getMethod("throwWithLocal", int.class).invoke(null, 5));
    }

    @Test
    public void injectAtThrowWithSurrogate() throws Exception {
        Class<?> target = this.transformWithManager("throwSurrogate");
        assertEquals(206, target.getMethod("throwWithLocal", int.class).invoke(null, 5));
    }

    private Class<?> transform(final String name) throws Exception {
        ClassNode target = this.read(Target.class);
        ClassNode transformer = this.read(ReturnTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        this.handler.transform(this.transformerManager, target, transformer);
        return TestClassLoader.load(target);
    }

    private Class<?> transformWithManager(final String name) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        ClassNode transformer = this.read(ReturnTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        manager.addTransformer(transformer);
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class Target {

        public static int calls;
        public static boolean voidRan;
        public static boolean tailSink;

        public static int returnValue(final int value) {
            return value + 1;
        }

        public static void voidReturn(final int value) {
            voidRan = true;
        }

        public static boolean booleanReturn(final boolean value) {
            return !value;
        }

        public static byte byteReturn(final byte value) {
            return (byte) (value + 1);
        }

        public static short shortReturn(final short value) {
            return (short) (value + 1);
        }

        public static char charReturn(final char value) {
            return (char) (value + 1);
        }

        public static void voidTail(final boolean early) {
            if (early) return;
            tailSink = true;
        }

        public static long tailLong(final long value) {
            if (value < 0) return -1;
            return value + 1;
        }

        public static double tailDouble(final double value) {
            if (value < 0) return -1D;
            return value + 1D;
        }

        public static String tailObject(final String value) {
            if (value == null) return "null";
            return value + "!";
        }

        public static long longReturn(final long value) {
            return value + 1;
        }

        public static float floatReturn(final float value) {
            return value + 1F;
        }

        public static double doubleReturn(final double value) {
            return value + 1D;
        }

        public static String objectReturn(final String value) {
            return value + "!";
        }

        public static int throwWithCallback(final int value) {
            throw new IllegalStateException("callback " + value);
        }

        public static int throwWithReturn(final int value) {
            throw new IllegalStateException("return " + value);
        }

        public static int throwWithoutCancel(final int value) {
            throw new IllegalStateException("no cancel " + value);
        }

        public static int throwWithLocal(final int value) {
            int local = value + 1;
            throw new IllegalStateException("local " + local);
        }

        public static int tailValue(final int value) {
            return value + 1;
        }

        public static int throwValue(final int value) {
            throw new IllegalStateException("value " + value);
        }

    }

    @CTransformer(Target.class)
    public static class ReturnTransformer {

        @CInject(method = "returnValue", target = @CTarget("RETURN"), cancellable = true)
        public static void returnInject(final int value, final InjectionCallback callback) {
            callback.setReturnValue(value + 100);
        }

        @CInject(method = "tailValue", target = @CTarget("TAIL"), cancellable = true)
        public static void tailInject(final int value, final InjectionCallback callback) {
            callback.setReturnValue(value + 200);
        }

        @CInject(method = "throwValue", target = @CTarget("THROW"))
        public static void throwInject(final int value) {
            Target.calls++;
        }

        @CInject(method = "returnValue", target = @CTarget("RETURN"))
        public static void returnWithoutCancellation(final int value, final InjectionCallback callback) {
            Target.calls++;
            if (callback.getReturnValueI() != value + 1) {
                throw new IllegalStateException("Unexpected return value");
            }
        }

        @CInject(method = "voidReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void voidReturnInject(final int value, final InjectionCallback callback) {
            Target.calls++;
            callback.cancel();
        }

        @CInject(method = "voidReturn", target = @CTarget("RETURN"))
        public static void voidReturnCallback(final int value, final InjectionCallback callback) {
            Target.calls++;
        }

        @CInject(method = "booleanReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void booleanReturnInject(final boolean value, final InjectionCallback callback) {
            callback.setReturnValue(value);
        }

        @CInject(method = "byteReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void byteReturnInject(final byte value, final InjectionCallback callback) {
            callback.setReturnValue((byte) (value + 100));
        }

        @CInject(method = "shortReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void shortReturnInject(final short value, final InjectionCallback callback) {
            callback.setReturnValue((short) (value + 1000));
        }

        @CInject(method = "charReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void charReturnInject(final char value, final InjectionCallback callback) {
            callback.setReturnValue((char) (value + 10));
        }

        @CInject(method = "voidTail", target = @CTarget("TAIL"), cancellable = true)
        public static void voidTailInject(final boolean early, final InjectionCallback callback) {
            Target.calls++;
            callback.cancel();
        }

        @CInject(method = "tailLong", target = @CTarget("TAIL"), cancellable = true)
        public static void tailLongInject(final long value, final InjectionCallback callback) {
            callback.setReturnValue(value + 100L);
        }

        @CInject(method = "tailDouble", target = @CTarget("TAIL"), cancellable = true)
        public static void tailDoubleInject(final double value, final InjectionCallback callback) {
            callback.setReturnValue(value + 100D);
        }

        @CInject(method = "tailObject", target = @CTarget("TAIL"), cancellable = true)
        public static void tailObjectInject(final String value, final InjectionCallback callback) {
            callback.setReturnValue(value + "?");
        }

        @CInject(method = "longReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void longReturnInject(final long value, final InjectionCallback callback) {
            callback.setReturnValue(value + 100L);
        }

        @CInject(method = "floatReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void floatReturnInject(final float value, final InjectionCallback callback) {
            callback.setReturnValue(value + 100F);
        }

        @CInject(method = "doubleReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void doubleReturnInject(final double value, final InjectionCallback callback) {
            callback.setReturnValue(value + 100D);
        }

        @CInject(method = "objectReturn", target = @CTarget("RETURN"), cancellable = true)
        public static void objectReturnInject(final String value, final InjectionCallback callback) {
            callback.setReturnValue(value + "?");
        }

        @CInject(method = "throwWithCallback", target = @CTarget("THROW"))
        public static void throwCallback(final int value, final InjectionCallback callback) {
            Target.calls++;
        }

        @CInject(method = "throwWithReturn", target = @CTarget("THROW"), cancellable = true)
        public static void throwReturn(final int value, final InjectionCallback callback) {
            Target.calls++;
            callback.setReturnValue(value + 50);
        }

        @CInject(method = "throwWithoutCancel", target = @CTarget("THROW"), cancellable = true)
        public static void throwNoCancel(final int value, final InjectionCallback callback) {
            Target.calls++;
        }

        @CInject(method = "throwWithLocal", target = @CTarget("THROW"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILHARD)
        public static void throwLocal(final int value, final InjectionCallback callback, final int local) {
            callback.setReturnValue(local + 100);
        }

        @CInject(method = "throwWithLocal", target = @CTarget("THROW"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILHARD)
        public static void throwSurrogate(final int value, final InjectionCallback callback, final long local) {
            callback.setReturnValue(1);
        }

        @CSurrogate
        public static void throwSurrogate(final int value, final InjectionCallback callback, final int local) {
            callback.setReturnValue(local + 200);
        }

    }

}
