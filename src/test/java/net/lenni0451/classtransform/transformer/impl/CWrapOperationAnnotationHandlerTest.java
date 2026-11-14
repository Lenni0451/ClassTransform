package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CCancellable;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.annotations.injection.CWrapOperation;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.utils.local.CLocalIntRef;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class CWrapOperationAnnotationHandlerTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/impl/CWrapOperationAnnotationHandlerTest$Target;";
    private static final String HOLDER = "Lnet/lenni0451/classtransform/transformer/impl/CWrapOperationAnnotationHandlerTest$Holder;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void wrapMethodInvocation() throws Exception {
        Class<?> target = this.transform(InvokeTransformer.class);
        target.getField("mode").set(null, 0);
        assertEquals(6, target.getMethod("invoke").invoke(null));
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
    public void wrapFieldAccess() throws Exception {
        Class<?> target = this.transform(FieldTransformer.class);
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
    public void wrapObjectCreation() throws Exception {
        Class<?> target = this.transform(NewTransformer.class);
        target.getField("mode").set(null, 0);
        assertEquals(3L, ((Holder) target.getMethod("create", int.class).invoke(null, 2)).value);
        target.getField("mode").set(null, 1);
        assertEquals(100L, ((Holder) target.getMethod("create", int.class).invoke(null, 2)).value);
        target.getField("mode").set(null, 2);
        assertEquals(13L, ((Holder) target.getMethod("create", int.class).invoke(null, 2)).value);
    }

    @Test
    public void wrapSimpleOperations() throws Exception {
        Class<?> target = this.transform(SimpleTransformer.class);
        assertEquals("x!", target.getMethod("cast", Object.class).invoke(null, "x"));
        assertEquals(16, target.getMethod("add", int.class, int.class).invoke(null, 2, 3));
    }

    @Test
    public void wrapConstant() throws Exception {
        Class<?> target = this.transform(ConstantTransformer.class);
        assertEquals(6, target.getMethod("constant").invoke(null));
    }

    @Test
    public void wrapStringConstant() throws Exception {
        Class<?> target = this.transform(StringConstantTransformer.class);
        assertEquals("value!", target.getMethod("stringConstant").invoke(null));
    }

    @Test
    public void wrapClassConstant() throws Exception {
        Class<?> target = this.transform(ClassConstantTransformer.class);
        assertEquals(Integer.class, target.getMethod("classConstant").invoke(null));
    }

    @Test
    public void wrapNullConstant() throws Exception {
        Class<?> target = this.transform(NullConstantTransformer.class);
        assertEquals("wrapped", target.getMethod("nullConstant").invoke(null));
    }

    @Test
    public void wrapOrdinalConstant() throws Exception {
        Class<?> target = this.transform(OrdinalConstantTransformer.class);
        assertEquals(11, target.getMethod("duplicateConstants").invoke(null));
    }

    @Test
    public void wrapInstanceof() throws Exception {
        Class<?> target = this.transform(InstanceofTransformer.class);
        target.getField("mode").set(null, 0);
        assertTrue((boolean) target.getMethod("check", Object.class).invoke(null, "test"));
        assertFalse((boolean) target.getMethod("check", Object.class).invoke(null, 1));
        target.getField("mode").set(null, 1);
        assertFalse((boolean) target.getMethod("check", Object.class).invoke(null, "test"));
        assertFalse((boolean) target.getMethod("check", Object.class).invoke(null, 1));
    }

    @Test
    public void chainNestedWrappers() throws Exception {
        Class<?> target = this.transform(NestedTransformer.class);
        assertEquals(152, target.getMethod("invoke").invoke(null));
    }

    @Test
    public void wrapRedirectedInvocation() throws Exception {
        Class<?> target = this.transform(RedirectTransformer.class);
        assertEquals(101, target.getMethod("redirected", int.class).invoke(null, 1));
    }

    @Test
    public void wrapInstanceOperations() throws Exception {
        Class<?> instanceTarget = this.transform(InstanceTransformer.class);
        Object instance = instanceTarget.getConstructor().newInstance();
        assertEquals(6, instanceTarget.getMethod("instanceInvoke").invoke(instance));

        Class<?> nestedInstanceTarget = this.transform(InstanceNestedTransformer.class);
        Object nestedInstance = nestedInstanceTarget.getConstructor().newInstance();
        assertEquals(152, nestedInstanceTarget.getMethod("instanceInvoke").invoke(nestedInstance));

        Class<?> staticTarget = this.transform(HolderTransformer.class);
        assertEquals(4L, staticTarget.getMethod("callHolder", Holder.class).invoke(null, new Holder(3)));
        assertEquals(4L, staticTarget.getMethod("readHolder", Holder.class).invoke(null, new Holder(3)));
    }

    @Test
    public void shareCallbackAcrossWrapperChain() throws Exception {
        Class<?> target = this.transform(NestedCallbackTransformer.class);
        assertEquals(42, target.getMethod("nestedCancel").invoke(null));
        assertTrue((boolean) target.getField("observed").get(null));
    }

    @Test
    public void propagateSugarAndCallbackContext() throws Exception {
        Class<?> target = this.transform(SugarTransformer.class);
        assertEquals(14, target.getMethod("sugar", int.class).invoke(null, 2));
        assertEquals(4, target.getMethod("cancelWrap", int.class).invoke(null, 2));
        target.getField("mode").set(null, 1);
        assertEquals(42, target.getMethod("cancelWrap", int.class).invoke(null, 2));
    }

    @Test
    public void chainAcrossTransformers() throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(MultiInner.class.getName())));
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(MultiOuter.class.getName())));
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        Class<?> target = TestClassLoader.load(ASMUtils.fromBytes(bytes));
        assertEquals(152, target.getMethod("invoke").invoke(null));
    }

    @Test
    public void propagateOriginalExceptions() throws Exception {
        Class<?> target = this.transform(OriginalExceptionTransformer.class);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> target.getMethod("fail").invoke(null));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("original", exception.getCause().getMessage());
    }

    @Test
    public void propagateExceptions() throws Exception {
        Class<?> target = this.transform(ThrowingTransformer.class);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> target.getMethod("invoke").invoke(null));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    public void rejectIncorrectOperationArgumentCount() throws Exception {
        Class<?> target = this.transform(InvalidOperationCall.class);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> target.getMethod("invoke").invoke(null));
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    private Class<?> transform(final Class<?> transformerClass) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
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
        public static int calls;
        public static int field = 5;

        public static int compute(final int value) {
            calls++;
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

        public int instanceInvoke() {
            return compute(2);
        }

        public static long callHolder(final Holder holder) {
            return holder.value();
        }

        public static long readHolder(final Holder holder) {
            return holder.value;
        }

        public static int sugar(final int value) {
            int local = value;
            return compute(local);
        }

        public static int cancelWrap(final int value) {
            return compute(value);
        }

        public static boolean observed;

        public static int nestedCancel() {
            return compute(2);
        }

        public static String cast(final Object object) {
            return (String) object;
        }

        public static int add(final int left, final int right) {
            return left + right;
        }

        public static int constant() {
            return 5;
        }

        public static String stringConstant() {
            return "value";
        }

        public static Class<?> classConstant() {
            return String.class;
        }

        public static Object nullConstant() {
            return null;
        }

        public static int duplicateConstants() {
            int first = 5;
            int second = 5;
            return first + second;
        }

        public static int fail() {
            return throwing(2);
        }

        public static int throwing(final int value) {
            throw new IllegalStateException("original");
        }

    }

    @CTransformer(Target.class)
    private static class InvokeTransformer {

        @CShadow
        private static int mode;

        @CWrapOperation(method = "invoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int invoke(final int value, final COperation<Integer> original) {
            if (mode == 1) return 42;
            if (mode == 2) return original.call(value) + original.call(value);
            return original.call(value + 1);
        }

    }

    @CTransformer(Target.class)
    private static class FieldTransformer {

        @CShadow
        private static int mode;

        @CWrapOperation(method = "fieldRead", target = @CTarget(value = "GETFIELD", target = TARGET + "field:I"))
        private static int fieldRead(final COperation<Integer> original) {
            return mode == 1 ? 99 : original.call();
        }

        @CWrapOperation(method = "fieldWrite", target = @CTarget(value = "PUTFIELD", target = TARGET + "field:I"))
        private static void fieldWrite(final int value, final COperation<Void> original) {
            original.call(mode == 1 ? value + 1 : value);
        }

    }

    @CTransformer(Target.class)
    private static class NewTransformer {

        @CShadow
        private static int mode;

        @CWrapOperation(method = "create", target = @CTarget(value = "NEW", target = HOLDER))
        private static Holder create(final long value, final COperation<Holder> original) {
            if (mode == 1) return new Holder(100);
            if (mode == 2) return original.call(value + 10);
            return original.call(value);
        }

    }

    @CTransformer(Target.class)
    private static class StringConstantTransformer {

        @CWrapOperation(method = "stringConstant", target = @CTarget(value = "CONSTANT", target = "string value"))
        private static String constant(final COperation<String> original) {
            return original.call() + "!";
        }

    }

    @CTransformer(Target.class)
    private static class ClassConstantTransformer {

        @CWrapOperation(method = "classConstant", target = @CTarget(value = "CONSTANT", target = "type Ljava/lang/String;"))
        private static Class<?> constant(final COperation<Class<?>> original) {
            return Integer.class;
        }

    }

    @CTransformer(Target.class)
    private static class NullConstantTransformer {

        @CWrapOperation(method = "nullConstant", target = @CTarget(value = "CONSTANT", target = "null"))
        private static Object constant(final COperation<Object> original) {
            return "wrapped";
        }

    }

    @CTransformer(Target.class)
    private static class OrdinalConstantTransformer {

        @CWrapOperation(method = "duplicateConstants", target = @CTarget(value = "CONSTANT", target = "int 5", ordinal = 1))
        private static int constant(final COperation<Integer> original) {
            return original.call() + 1;
        }

    }

    @CTransformer(Target.class)
    private static class InstanceofTransformer {

        @CShadow
        private static int mode;

        @CWrapOperation(method = "check", target = @CTarget(value = "INSTANCEOF", target = "java/lang/String"))
        private static boolean check(final Object object, final COperation<Boolean> original) {
            return mode == 0 && original.call(object);
        }

    }

    @CTransformer(Target.class)
    private static class NestedTransformer {

        @CWrapOperation(method = "invoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 100)
        private static int inner(final int value, final COperation<Integer> original) {
            return original.call(value + 1) * 2;
        }

        @CWrapOperation(method = "invoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 200)
        private static int outer(final int value, final COperation<Integer> original) {
            return original.call(value + 10) + 100;
        }

    }

    @CTransformer(Target.class)
    private static class RedirectTransformer {

        @CRedirect(method = "redirected", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int redirect(final int value) {
            return value + 100;
        }

        @CWrapOperation(method = "redirected", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int wrap(final int value, final COperation<Integer> original) {
            return original.call(value);
        }

    }

    @CTransformer(Target.class)
    private static class SimpleTransformer {

        @CWrapOperation(method = "cast", target = @CTarget(value = "OPCODE", target = "CHECKCAST"))
        private static String cast(final Object object, final COperation<String> original) {
            return original.call(object) + "!";
        }

        @CWrapOperation(method = "add", target = @CTarget(value = "OPCODE", target = "IADD"))
        private static int add(final int left, final int right, final COperation<Integer> original) {
            return original.call(left + 1, right) + 10;
        }

    }

    @CTransformer(Target.class)
    private static class ConstantTransformer {

        @CWrapOperation(method = "constant", target = @CTarget(value = "CONSTANT", target = "int 5"))
        private static int constant(final COperation<Integer> original) {
            return original.call() + 1;
        }

    }

    @CTransformer(Target.class)
    private static class InstanceTransformer {

        @CWrapOperation(method = "instanceInvoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private int instanceInvoke(final int value, final COperation<Integer> original) {
            return original.call(value + 1);
        }

    }

    @CTransformer(Target.class)
    private static class InstanceNestedTransformer {

        @CWrapOperation(method = "instanceInvoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 100)
        private int inner(final int value, final COperation<Integer> original) {
            return original.call(value + 1) * 2;
        }

        @CWrapOperation(method = "instanceInvoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 200)
        private int outer(final int value, final COperation<Integer> original) {
            return original.call(value + 10) + 100;
        }

    }

    @CTransformer(Target.class)
    private static class HolderTransformer {

        @CWrapOperation(method = "callHolder", target = @CTarget(value = "INVOKE", target = HOLDER + "value()J"))
        private static long callHolder(final Holder holder, final COperation<Long> original) {
            return original.call(holder) + 1;
        }

        @CWrapOperation(method = "readHolder", target = @CTarget(value = "GETFIELD", target = HOLDER + "value:J"))
        private static long readHolder(final Holder holder, final COperation<Long> original) {
            return original.call(holder) + 1;
        }

    }

    @CTransformer(Target.class)
    private static class SugarTransformer {

        @CShadow
        private static int mode;

        @CWrapOperation(method = "sugar", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int sugar(final int value, final COperation<Integer> original, @CLocalVariable(name = "local") final CLocalIntRef local) {
            local.set(local.get() + 5);
            return original.call(local.get());
        }

        @CWrapOperation(method = "cancelWrap", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int cancelWrap(final int value, final COperation<Integer> original, @CCancellable final InjectionCallback callback) {
            if (mode == 1) callback.setReturnValue(42);
            return original.call(value);
        }

    }

    @CTransformer(Target.class)
    private static class NestedCallbackTransformer {

        @CShadow
        private static boolean observed;

        @CWrapOperation(method = "nestedCancel", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 100)
        private static int inner(final int value, final COperation<Integer> original, @CCancellable final InjectionCallback callback) {
            callback.setReturnValue(42);
            return 42;
        }

        @CWrapOperation(method = "nestedCancel", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 200)
        private static int outer(final int value, final COperation<Integer> original, @CCancellable final InjectionCallback callback) {
            int result = original.call(value);
            observed = callback.isCancelled();
            return result + 100;
        }

    }

    @CTransformer(Target.class)
    private static class MultiInner {

        @CWrapOperation(method = "invoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 100)
        private static int inner(final int value, final COperation<Integer> original) {
            return original.call(value + 1) * 2;
        }

    }

    @CTransformer(Target.class)
    private static class MultiOuter {

        @CWrapOperation(method = "invoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"), order = 200)
        private static int outer(final int value, final COperation<Integer> original) {
            return original.call(value + 10) + 100;
        }

    }

    @CTransformer(Target.class)
    private static class OriginalExceptionTransformer {

        @CWrapOperation(method = "fail", target = @CTarget(value = "INVOKE", target = TARGET + "throwing(I)I"))
        private static int fail(final int value, final COperation<Integer> original) {
            return original.call(value);
        }

    }

    @CTransformer(Target.class)
    private static class ThrowingTransformer {

        @CWrapOperation(method = "invoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int invoke(final int value, final COperation<Integer> original) {
            throw new IllegalStateException("wrapped");
        }

    }

    @CTransformer(Target.class)
    private static class InvalidOperationCall {

        @CWrapOperation(method = "invoke", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        private static int invoke(final int value, final COperation<Integer> original) {
            return original.call();
        }

    }

}
