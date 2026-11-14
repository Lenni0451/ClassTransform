package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CCoerce;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.annotations.injection.CModifyReceiver;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.annotations.injection.CWrapOperation;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CCoerceTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);

    @Test
    public void coerceExpressionParameterAndReturn() throws Exception {
        Class<?> target = this.transform(new CModifyExpressionValueAnnotationHandler(), ExpressionTarget.class, ExpressionTransformer.class, "string");
        assertEquals("VALUE!", target.getMethod("stringValue").invoke(null));
    }

    @Test
    public void coerceIntLikeParameter() throws Exception {
        Class<?> target = this.transform(new CModifyExpressionValueAnnotationHandler(), ExpressionTarget.class, ExpressionTransformer.class, "intLike");
        assertEquals(101, target.getMethod("intByte").invoke(null));
    }

    @Test
    public void coerceRedirectParameter() throws Exception {
        Class<?> target = this.transform(new CRedirectAnnotationHandler(), RedirectTarget.class, RedirectTransformer.class, "receiver");
        assertEquals(1234, target.getMethod("rint").invoke(null));
    }

    @Test
    public void coerceRedirectReturn() throws Exception {
        Class<?> target = this.transform(new CRedirectAnnotationHandler(), RedirectTarget.class, RedirectTransformer.class, "returnValue");
        assertEquals("redirected", target.getMethod("staticValue").invoke(null));
    }

    @Test
    public void coerceFieldReturn() throws Exception {
        Class<?> target = this.transform(new CRedirectAnnotationHandler(), RedirectTarget.class, RedirectTransformer.class, "field");
        assertEquals("redirected", target.getMethod("fieldValue").invoke(null));
    }

    @Test
    public void coerceReturnValueParameter() throws Exception {
        Class<?> target = this.transform(new CModifyReturnValueAnnotationHandler(), ReturnTarget.class, ReturnTransformer.class, "modify");
        assertEquals("VALUE", target.getMethod("echo", String.class).invoke(null, "value"));
    }

    @Test
    public void coerceReceiverParameter() throws Exception {
        Class<?> target = this.transform(new CModifyReceiverAnnotationHandler(), ReceiverTarget.class, ReceiverTransformer.class, "modify");
        assertEquals("VALUE!", target.getMethod("call").invoke(null));
    }

    @Test
    public void coerceWrapOperationReturn() throws Exception {
        Class<?> target = this.transform(new CWrapOperationAnnotationHandler(), WrapTarget.class, WrapTransformer.class, "wrap");
        assertEquals("VALUE!", target.getMethod("value").invoke(null));
    }

    @Test
    public void coerceInjectParameter() throws Exception {
        Class<?> target = this.transform(new CInjectAnnotationHandler(), InjectTarget.class, InjectTransformer.class, "inject");
        assertEquals("value!", target.getMethod("echo", String.class).invoke(null, "value"));
    }

    private Class<?> transform(final AnnotationHandler handler, final Class<?> targetClass, final Class<?> transformerClass, final String method) throws Exception {
        ClassNode target = this.read(targetClass);
        ClassNode transformer = this.read(transformerClass);
        transformer.methods.removeIf(transformerMethod -> !transformerMethod.name.equals(method));
        handler.transform(this.transformerManager, target, transformer);
        return TestClassLoader.load(target);
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class ExpressionTarget {

        public static String stringValue() {
            return "value".toUpperCase();
        }

        public static int intByte() {
            return 100;
        }

    }

    public static class ExpressionTransformer {

        @CCoerce
        @CModifyExpressionValue(method = "stringValue", target = @CTarget(value = "INVOKE", target = "Ljava/lang/String;toUpperCase()Ljava/lang/String;"))
        public static Object string(@CCoerce Object original) {
            return original + "!";
        }

        @CModifyExpressionValue(method = "intByte", target = @CTarget(value = "CONSTANT", target = "int 100"))
        public static int intLike(@CCoerce boolean original) {
            return original ? 101 : 0;
        }

    }

    public static class RedirectTarget {

        public static String value = "value";

        public static int rint() {
            Random random = new Random();
            return random.nextInt();
        }

        public static String staticValue() {
            return String.valueOf(5);
        }

        public static String fieldValue() {
            return value;
        }

    }

    public static class RedirectTransformer {

        @CRedirect(method = "rint", target = @CTarget(value = "INVOKE", target = "Ljava/util/Random;nextInt()I"))
        public static int receiver(@CCoerce Object random) {
            return 1234;
        }

        @CCoerce
        @CRedirect(method = "staticValue", target = @CTarget(value = "INVOKE", target = "Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
        public static Object returnValue(int value) {
            return "redirected";
        }

        @CCoerce
        @CRedirect(method = "fieldValue", target = @CTarget(value = "FIELD", target = "Lnet/lenni0451/classtransform/transformer/impl/CCoerceTest$RedirectTarget;value:Ljava/lang/String;"))
        public static Object field() {
            return "redirected";
        }

    }

    public static class ReturnTarget {

        public static String echo(String value) {
            return value;
        }

    }

    public static class ReturnTransformer {

        @CModifyReturnValue(method = "echo", target = @CTarget("RETURN"))
        public static String modify(@CCoerce Object original) {
            return ((String) original).toUpperCase();
        }

    }

    public static class ReceiverTarget {

        public static String call() {
            return "value".toUpperCase();
        }

    }

    public static class ReceiverTransformer {

        @CModifyReceiver(method = "call", target = @CTarget(value = "INVOKE", target = "Ljava/lang/String;toUpperCase()Ljava/lang/String;"))
        public static String modify(@CCoerce Object receiver) {
            return receiver + "!";
        }

    }

    public static class WrapTarget {

        public static String value() {
            return "value".toUpperCase();
        }

    }

    public static class WrapTransformer {

        @CCoerce
        @CWrapOperation(method = "value", target = @CTarget(value = "INVOKE", target = "Ljava/lang/String;toUpperCase()Ljava/lang/String;"))
        public static Object wrap(@CCoerce Object receiver, COperation<String> operation) {
            return operation.call(receiver) + "!";
        }

    }

    public static class InjectTarget {

        public static String echo(String value) {
            return value;
        }

    }

    public static class InjectTransformer {

        @CInject(method = "echo", target = @CTarget("HEAD"), cancellable = true)
        public static void inject(@CCoerce Object value, InjectionCallback callback) {
            callback.setReturnValue(value + "!");
        }

    }

}
