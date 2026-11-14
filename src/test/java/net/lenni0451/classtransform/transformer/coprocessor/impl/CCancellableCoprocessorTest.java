package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CCancellable;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.*;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CCancellableCoprocessorTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/coprocessor/impl/CCancellableCoprocessorTest$Target;";
    private static final String PERSON = "Lnet/lenni0451/classtransform/transformer/coprocessor/impl/CCancellableCoprocessorTest$Person;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void injectCallbacks() throws Exception {
        Class<?> target = this.transform();
        assertEquals(1, target.getMethod("injectReturn").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("injectReturn").invoke(null));

        target.getField("cancel").set(null, false);
        target.getField("effects").set(null, 0);
        target.getMethod("injectVoid").invoke(null);
        assertEquals(1, target.getField("effects").get(null));
        target.getField("cancel").set(null, true);
        target.getMethod("injectVoid").invoke(null);
        assertEquals(1, target.getField("effects").get(null));

        target.getField("cancel").set(null, false);
        assertEquals(1, target.getMethod("injectPoint").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("injectPoint").invoke(null));
    }

    @Test
    public void valueModifierCallbacks() throws Exception {
        Class<?> target = this.transform();
        assertEquals(2, target.getMethod("returnValue").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("returnValue").invoke(null));

        target.getField("cancel").set(null, false);
        assertEquals(20, target.getMethod("expressionValue").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("expressionValue").invoke(null));

        target.getField("cancel").set(null, false);
        assertEquals(3, target.getMethod("constantValue").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("constantValue").invoke(null));

        target.getField("cancel").set(null, false);
        assertEquals(5, target.getMethod("redirectValue").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("redirectValue").invoke(null));
    }

    @Test
    public void receiverAndConditionCallbacks() throws Exception {
        Class<?> target = this.transform();
        assertEquals(10L, target.getMethod("receiverCall").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42L, target.getMethod("receiverCall").invoke(null));

        target.getField("cancel").set(null, false);
        target.getField("effects").set(null, 0);
        target.getMethod("condition").invoke(null);
        assertEquals(1, target.getField("effects").get(null));
        target.getField("cancel").set(null, true);
        target.getField("effects").set(null, 0);
        target.getMethod("condition").invoke(null);
        assertEquals(0, target.getField("effects").get(null));
    }

    @Test
    public void callbacksWithValuesBelowTheHandlerCall() throws Exception {
        Class<?> target = this.transform();
        target.getField("cancel").set(null, false);
        target.getMethod("fieldExpression").invoke(null);
        assertEquals(20L, ((Person) target.getField("person").get(null)).value);
        target.getField("cancel").set(null, true);
        target.getMethod("fieldExpression").invoke(null);
        assertEquals(20L, ((Person) target.getField("person").get(null)).value);

        target.getField("cancel").set(null, false);
        assertEquals(1, target.getMethod("fieldInjection").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("fieldInjection").invoke(null));
    }

    @Test
    public void nativeInjectCallbackAtNonReturnPoint() throws Exception {
        Class<?> target = this.transform();
        target.getField("effects").set(null, 0);
        assertEquals(1, target.getMethod("nativeCancel").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42, target.getMethod("nativeCancel").invoke(null));
    }

    @Test
    public void wideAndObjectReturns() throws Exception {
        Class<?> target = this.transform();
        assertEquals(4L, target.getMethod("wideReturn").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals(42L, target.getMethod("wideReturn").invoke(null));

        target.getField("cancel").set(null, false);
        assertEquals("original!", target.getMethod("objectReturn").invoke(null));
        target.getField("cancel").set(null, true);
        assertEquals("cancelled", target.getMethod("objectReturn").invoke(null));
    }

    private Class<?> transform() throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(Transformer.class.getName())));
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class Person {

        public long value;

        public Person(final long value) {
            this.value = value;
        }

        public long getValue() {
            return this.value;
        }

    }

    public static class Target {

        public static boolean cancel;
        public static int effects;
        public static Person first = new Person(1);
        public static Person person = new Person(0);

        public static void effect() {
            effects++;
        }

        public static void fieldExpression() {
            person.value = Math.abs(2);
        }

        public static int fieldInjection() {
            person.value = Math.abs(2);
            return 1;
        }

        public static int injectReturn() {
            return 1;
        }

        public static void injectVoid() {
            effect();
        }

        public static int injectPoint() {
            effect();
            return 1;
        }

        public static int nativeCancel() {
            effect();
            return 1;
        }

        public static int returnValue() {
            return 1;
        }

        public static int expressionValue() {
            return Math.abs(2);
        }

        public static long receiverCall() {
            return first.getValue();
        }

        public static void condition() {
            effect();
        }

        public static int redirectValue() {
            return Math.abs(2);
        }

        public static int constantValue() {
            return 2;
        }

        public static long wideReturn() {
            return 3L;
        }

        public static String objectReturn() {
            return "original";
        }

    }

    @CTransformer(Target.class)
    private static class Transformer {

        @CShadow
        private static boolean cancel;

        @CInject(method = "injectReturn", target = @CTarget("HEAD"))
        private static void injectReturn(@CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
        }

        @CInject(method = "injectVoid", target = @CTarget("HEAD"))
        private static void injectVoid(@CCancellable final InjectionCallback callback) {
            if (cancel) callback.cancel();
        }

        @CInject(method = "injectPoint", target = @CTarget(value = "INVOKE", target = TARGET + "effect()V"))
        private static void injectPoint(@CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
        }

        @CInject(method = "nativeCancel", target = @CTarget(value = "INVOKE", target = TARGET + "effect()V"), cancellable = true)
        private static void nativeCancel(final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
        }

        @CModifyExpressionValue(method = "fieldExpression", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        private static int fieldExpression(final int original, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
            return original * 10;
        }

        @CInject(method = "fieldInjection", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        private static void fieldInjection(@CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
        }

        @CModifyReturnValue(method = "returnValue", target = @CTarget("RETURN"))
        private static int returnValue(final int original, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
            return original + 1;
        }

        @CModifyExpressionValue(method = "expressionValue", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        private static int expressionValue(final int original, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
            return original * 10;
        }

        @CModifyReceiver(method = "receiverCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        private static Person receiverCall(final Person original, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42L);
            return new Person(10);
        }

        @CWrapCondition(method = "condition", target = @CTarget(value = "INVOKE", target = TARGET + "effect()V"))
        private static boolean condition(@CCancellable final InjectionCallback callback) {
            if (cancel) callback.cancel();
            return true;
        }

        @CRedirect(method = "redirectValue", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        private static int redirectValue(final int value, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
            return 5;
        }

        @CModifyConstant(method = "constantValue", intValue = 2)
        private static int constantValue(final int value, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42);
            return value + 1;
        }

        @CModifyReturnValue(method = "wideReturn", target = @CTarget("RETURN"))
        private static long wideReturn(final long original, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue(42L);
            return original + 1;
        }

        @CModifyReturnValue(method = "objectReturn", target = @CTarget("RETURN"))
        private static String objectReturn(final String original, @CCancellable final InjectionCallback callback) {
            if (cancel) callback.setReturnValue("cancelled");
            return original + "!";
        }

    }

}
