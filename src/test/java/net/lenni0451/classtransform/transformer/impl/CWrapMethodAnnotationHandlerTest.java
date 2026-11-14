package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.annotations.injection.CWrapMethod;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.local.CLocalIntRef;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class CWrapMethodAnnotationHandlerTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/impl/CWrapMethodAnnotationHandlerTest$Target;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void wrapMethod() throws Exception {
        Class<?> target = this.transform(InvokeWrapper.class);
        target.getField("mode").set(null, 0);
        assertEquals(14, target.getMethod("invoke").invoke(null));
        assertEquals(1, target.getField("calls").get(null));

        target.getField("mode").set(null, 1);
        target.getField("calls").set(null, 0);
        assertEquals(42, target.getMethod("invoke").invoke(null));
        assertEquals(0, target.getField("calls").get(null));

        target.getField("mode").set(null, 2);
        target.getField("calls").set(null, 0);
        assertEquals(8, target.getMethod("invoke").invoke(null));
        assertEquals(2, target.getField("calls").get(null));
    }

    @Test
    public void changeArguments() throws Exception {
        Class<?> target = this.transform(ArgumentWrapper.class);
        assertEquals(12, target.getMethod("change", int.class).invoke(null, 1));
    }

    @Test
    public void wrapVoidMethod() throws Exception {
        Class<?> target = this.transform(VoidWrapper.class);
        target.getMethod("take", int.class).invoke(null, 2);
        assertEquals(5, target.getField("calls").get(null));
    }

    @Test
    public void wrapInstanceMethod() throws Exception {
        Class<?> target = this.transform(InstanceWrapper.class);
        Object instance = target.getConstructor().newInstance();
        assertEquals(3, target.getMethod("instance", int.class).invoke(instance, 1));
    }

    @Test
    public void propagateExceptions() throws Exception {
        Class<?> target = this.transform(ThrowWrapper.class);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> target.getMethod("throwing").invoke(null));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("wrapped", exception.getCause().getMessage());

        Class<?> original = this.transform(OriginalThrowWrapper.class);
        exception = assertThrows(InvocationTargetException.class, () -> original.getMethod("throwing").invoke(null));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("original", exception.getCause().getMessage());
    }

    @Test
    public void wrapRecursiveMethod() throws Exception {
        Class<?> target = this.transform(RecursiveWrapper.class);
        assertEquals(120, target.getMethod("factorial", int.class).invoke(null, 5));
        assertEquals(5, target.getField("calls").get(null));
    }

    @Test
    public void wrapSynchronizedMethod() throws Exception {
        Class<?> target = this.transform(SynchronizedWrapper.class);
        assertEquals(3, target.getMethod("synced", int.class).invoke(null, 2));
        assertTrue(Modifier.isSynchronized(target.getMethod("synced", int.class).getModifiers()));
    }

    @Test
    public void interactWithInjections() throws Exception {
        Class<?> target = this.transform(InjectWrapper.class);
        assertEquals(6, target.getMethod("mixed", int.class).invoke(null, 2));
        assertEquals(3, target.getField("seen").get(null));
    }

    @Test
    public void shareBetweenWrapperAndBody() throws Exception {
        Class<?> target = this.transform(ShareWrapper.class);
        assertEquals(45, target.getMethod("shared", int.class).invoke(null, 1));
        assertEquals(42, target.getField("sharedObserved").get(null));
        assertEquals(43, target.getField("sharedAfter").get(null));
    }

    @Test
    public void shareBetweenNestedWrappers() throws Exception {
        Class<?> target = this.transform(NestedShareWrapper.class);
        assertEquals(102, target.getMethod("nestedShared", int.class).invoke(null, 1));
        assertEquals(10, target.getField("innerSeen").get(null));
        assertEquals(12, target.getField("outerAfter").get(null));
        assertEquals(11, target.getField("bodyObserved").get(null));
    }

    @Test
    public void shareValueBetweenBodyInjections() throws Exception {
        Class<?> target = this.transform(BodyShareWrapper.class);
        assertEquals(4, target.getMethod("bodyShared", int.class).invoke(null, 2));
        assertEquals(5, target.getField("bodyValueObserved").get(null));
    }

    @Test
    public void shareWithNamespaceAcrossTransformers() throws Exception {
        Class<?> target = this.transform(NamespaceInjector.class, NamespaceWrapper.class);
        assertEquals(33, target.getMethod("namespaceShared", int.class).invoke(null, 1));
        assertEquals(30, target.getField("namespaceObserved").get(null));
        assertEquals(31, target.getField("namespaceAfter").get(null));
    }

    @Test
    public void chainAcrossTransformers() throws Exception {
        Class<?> target = this.transform(InnerWrapper.class, OuterWrapper.class);
        assertEquals(112, target.getMethod("change", int.class).invoke(null, 0));
    }

    private Class<?> transform(final Class<?>... transformerClasses) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        for (Class<?> transformerClass : transformerClasses) {
            manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        }
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class Target {

        public static int mode;
        public static int calls;
        public static int seen;
        public static int sharedObserved;
        public static int sharedAfter;
        public static int innerSeen;
        public static int outerAfter;
        public static int bodyObserved;
        public static int bodyValueObserved;
        public static int namespaceObserved;
        public static int namespaceAfter;

        public static int compute(final int value) {
            calls++;
            return value * 2;
        }

        public static int invoke() {
            return compute(2);
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

        public static int throwing() {
            throw new IllegalStateException("original");
        }

        public static int factorial(final int value) {
            return value <= 1 ? 1 : value * factorial(value - 1);
        }

        public static synchronized int synced(final int value) {
            return value + 1;
        }

        public static int mixed(final int value) {
            return compute(value);
        }

        public static int shared(final int value) {
            return value + 1;
        }

        public static int nestedShared(final int value) {
            return value + 1;
        }

        public static int bodyShared(final int value) {
            return value;
        }

        public static int namespaceShared(final int value) {
            return value + 1;
        }

    }

    @CTransformer(Target.class)
    private static class InvokeWrapper {

        @CShadow
        private static int mode;

        @CWrapMethod(method = "invoke")
        private static int invoke(final COperation<Integer> original) {
            if (mode == 1) return 42;
            if (mode == 2) return original.call() + original.call();
            return original.call() + 10;
        }

    }

    @CTransformer(Target.class)
    private static class ArgumentWrapper {

        @CWrapMethod(method = "change")
        private static int change(final int value, final COperation<Integer> original) {
            return original.call(value + 10);
        }

    }

    @CTransformer(Target.class)
    private static class VoidWrapper {

        @CWrapMethod(method = "take")
        private static void take(final int value, final COperation<Void> original) {
            original.call(value);
            original.call(value + 1);
        }

    }

    @CTransformer(Target.class)
    private static class InstanceWrapper {

        @CWrapMethod(method = "instance")
        private int instance(final int value, final COperation<Integer> original) {
            return original.call(value + 1);
        }

    }

    @CTransformer(Target.class)
    private static class ThrowWrapper {

        @CWrapMethod(method = "throwing")
        private static int throwing(final COperation<Integer> original) {
            throw new IllegalStateException("wrapped");
        }

    }

    @CTransformer(Target.class)
    private static class OriginalThrowWrapper {

        @CWrapMethod(method = "throwing")
        private static int throwing(final COperation<Integer> original) {
            return original.call();
        }

    }

    @CTransformer(Target.class)
    private static class RecursiveWrapper {

        @CShadow
        private static int calls;

        @CWrapMethod(method = "factorial")
        private static int factorial(final int value, final COperation<Integer> original) {
            calls++;
            return original.call(value);
        }

    }

    @CTransformer(Target.class)
    private static class SynchronizedWrapper {

        @CWrapMethod(method = "synced")
        private static int synced(final int value, final COperation<Integer> original) {
            return original.call(value);
        }

    }

    @CTransformer(Target.class)
    private static class InjectWrapper {

        @CShadow
        private static int seen;

        @CInject(method = "mixed", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static void inject(final int value) {
            seen = value;
        }

        @CWrapMethod(method = "mixed")
        private static int mixed(final int value, final COperation<Integer> original) {
            return original.call(value + 1);
        }

    }

    @CTransformer(Target.class)
    private static class ShareWrapper {

        @CShadow
        private static int sharedObserved;
        @CShadow
        private static int sharedAfter;

        @CInject(method = "shared", target = @CTarget("HEAD"))
        private static void inject(@CShared("value") final CLocalIntRef ref) {
            sharedObserved = ref.get();
            ref.set(ref.get() + 1);
        }

        @CWrapMethod(method = "shared")
        private static int shared(final int value, final COperation<Integer> original, @CShared("value") final CLocalIntRef ref) {
            ref.set(42);
            int result = original.call(value);
            sharedAfter = ref.get();
            return result + ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class NestedShareWrapper {

        @CShadow
        private static int innerSeen;
        @CShadow
        private static int outerAfter;
        @CShadow
        private static int bodyObserved;

        @CInject(method = "nestedShared", target = @CTarget("HEAD"))
        private static void inject(@CShared("value") final CLocalIntRef ref) {
            bodyObserved = ref.get();
            ref.set(ref.get() + 1);
        }

        @CWrapMethod(method = "nestedShared", order = 100)
        private static int inner(final int value, final COperation<Integer> original, @CShared("value") final CLocalIntRef ref) {
            innerSeen = ref.get();
            ref.set(ref.get() + 1);
            return original.call(value);
        }

        @CWrapMethod(method = "nestedShared", order = 200)
        private static int outer(final int value, final COperation<Integer> original, @CShared("value") final CLocalIntRef ref) {
            ref.set(10);
            int result = original.call(value);
            outerAfter = ref.get();
            return result + 100;
        }

    }

    @CTransformer(Target.class)
    private static class BodyShareWrapper {

        @CShadow
        private static int bodyValueObserved;

        @CInject(method = "bodyShared", target = @CTarget("HEAD"))
        private static void setValue(@CShared("bodyValue") int value) {
            value = 5;
        }

        @CInject(method = "bodyShared", target = @CTarget("HEAD"))
        private static void readValue(@CShared("bodyValue") final int value) {
            bodyValueObserved = value;
        }

        @CWrapMethod(method = "bodyShared")
        private static int bodyShared(final int value, final COperation<Integer> original) {
            return original.call(value) + original.call(value);
        }

    }

    @CTransformer(Target.class)
    private static class NamespaceInjector {

        @CShadow
        private static int namespaceObserved;

        @CInject(method = "namespaceShared", target = @CTarget("HEAD"))
        private static void inject(@CShared(value = "namespace", namespace = "test") final CLocalIntRef ref) {
            namespaceObserved = ref.get();
            ref.set(ref.get() + 1);
        }

    }

    @CTransformer(Target.class)
    private static class NamespaceWrapper {

        @CShadow
        private static int namespaceAfter;

        @CWrapMethod(method = "namespaceShared")
        private static int namespaceShared(final int value, final COperation<Integer> original, @CShared(value = "namespace", namespace = "test") final CLocalIntRef ref) {
            ref.set(30);
            int result = original.call(value);
            namespaceAfter = ref.get();
            return result + ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class InnerWrapper {

        @CWrapMethod(method = "change", order = 100)
        private static int change(final int value, final COperation<Integer> original) {
            return original.call(value + 1);
        }

    }

    @CTransformer(Target.class)
    private static class OuterWrapper {

        @CWrapMethod(method = "change", order = 200)
        private static int change(final int value, final COperation<Integer> original) {
            return original.call(value + 10) + 100;
        }

    }

}
