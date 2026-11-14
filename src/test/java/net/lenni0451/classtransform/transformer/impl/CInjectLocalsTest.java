package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalCapture;
import net.lenni0451.classtransform.annotations.CSurrogate;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CInjectLocalsTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CInjectAnnotationHandler handler = new CInjectAnnotationHandler();

    @Test
    public void captureLocals() throws Exception {
        Class<?> target = this.transform("hard");
        assertEquals(15, target.getMethod("method").invoke(null));
    }

    @Test
    public void captureLocalsWithTargetArguments() throws Exception {
        Class<?> target = this.transform("captureArguments");
        assertEquals(21, target.getMethod("methodWithArgument", int.class).invoke(null, 5));
    }

    @Test
    public void softFailSkipsInjection() throws Exception {
        Class<?> target = this.transform("softFail");
        assertEquals(5, target.getMethod("method").invoke(null));
    }

    @Test
    public void hardFailThrows() {
        assertThrows(TransformerException.class, () -> this.transform("hardFail"));
    }

    @Test
    public void failExceptionThrowsAtRuntime() throws Exception {
        Class<?> target = this.transform("failException");
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> target.getMethod("method").invoke(null));
        assertTrue(exception.getCause() instanceof IllegalStateException);
    }

    @Test
    public void printSkipsInjection() throws Exception {
        Class<?> target = this.transform("print");
        assertEquals(5, target.getMethod("method").invoke(null));
    }

    @Test
    public void useSurrogateHandler() throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(SurrogateTransformer.class.getName())));
        byte[] bytes = manager.transform(LocalsTarget.class.getName(), this.classProvider.getClass(LocalsTarget.class.getName()));
        Class<?> target = TestClassLoader.load(ASMUtils.fromBytes(bytes));
        assertEquals(15, target.getMethod("method").invoke(null));
    }

    private Class<?> transform(final String name) throws Exception {
        ClassNode target = this.read(LocalsTarget.class);
        ClassNode transformer = this.read(LocalsTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        this.handler.transform(this.transformerManager, target, transformer);
        return TestClassLoader.load(target);
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class LocalsTarget {

        public static int method() {
            int value = 5;
            return Math.abs(value);
        }

        public static int methodWithArgument(int argument) {
            int value = argument + 1;
            return Math.abs(value);
        }

    }

    public static class LocalsTransformer {

        @CInject(method = "method", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILHARD)
        public static void hard(InjectionCallback callback, int value) {
            callback.setReturnValue(value + 10);
        }

        @CInject(method = "methodWithArgument", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILHARD)
        public static void captureArguments(int argument, InjectionCallback callback, int value) {
            callback.setReturnValue(argument + value + 10);
        }

        @CInject(method = "method", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILSOFT, require = 0)
        public static void softFail(InjectionCallback callback, String value) {
            callback.setReturnValue(15);
        }

        @CInject(method = "method", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILHARD)
        public static void hardFail(InjectionCallback callback, String value) {
            callback.setReturnValue(15);
        }

        @CInject(method = "method", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILEXCEPTION)
        public static void failException(InjectionCallback callback, String value) {
            callback.setReturnValue(15);
        }

        @CInject(method = "method", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), require = 0, locals = CLocalCapture.PRINT)
        public static void print(InjectionCallback callback) {
        }

    }

    @CTransformer(LocalsTarget.class)
    public static class SurrogateTransformer {

        @CInject(method = "method", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"), cancellable = true, locals = CLocalCapture.CAPTURE_FAILHARD)
        public static void inject(InjectionCallback callback, long value) {
            callback.setReturnValue(15);
        }

        @CSurrogate
        public static void inject(InjectionCallback callback, int value) {
            callback.setReturnValue(value + 10);
        }

    }

}
