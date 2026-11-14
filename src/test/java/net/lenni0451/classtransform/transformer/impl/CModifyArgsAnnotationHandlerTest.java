package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.CArgs;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyArgs;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CModifyArgsAnnotationHandlerTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CModifyArgsAnnotationHandler handler = new CModifyArgsAnnotationHandler();

    @Test
    public void modifyAllArguments() throws Exception {
        Class<?> target = this.transform("all");
        assertEquals(30, target.getMethod("call").invoke(null));
    }

    @Test
    public void modifyObjectArguments() throws Exception {
        Class<?> target = this.transform("objects");
        assertEquals("AB", target.getMethod("callObject").invoke(null));
    }

    @Test
    public void modifySingleTypeArguments() throws Exception {
        Class<?> target = this.transform("types");
        assertEquals(33, target.getMethod("callTyped").invoke(null));
    }

    @Test
    public void modifyConstructorArguments() throws Exception {
        Class<?> target = this.transform("constructor");
        assertEquals(30, target.getMethod("callNew").invoke(null));
    }

    @Test
    public void modifySuperConstructorArguments() throws Exception {
        Class<?> target = this.transform("superConstructor", Child.class);
        Object instance = target.getConstructor(int.class).newInstance(5);
        assertEquals(42, target.getField("value").get(instance));
        assertEquals(5, target.getField("own").get(instance));
    }

    @Test
    public void captureTargetArguments() throws Exception {
        Class<?> target = this.transform("capture");
        assertEquals(30, target.getMethod("callWithArguments", int.class, int.class).invoke(null, 10, 20));
    }

    @Test
    public void modifyInstanceInvocation() throws Exception {
        Class<?> target = this.transform("instance");
        assertEquals(30, target.getMethod("callInstance").invoke(null));
    }

    @Test
    public void rejectNonVoidHandler() {
        assertThrows(TransformerException.class, () -> this.transform("nonVoid"));
    }

    @Test
    public void rejectNoArguments() {
        assertThrows(TransformerException.class, () -> this.transform("noArguments"));
    }

    @Test
    public void rejectNonStaticHandlerBeforeSuper() {
        assertThrows(TransformerException.class, () -> this.transform("nonStaticSuper", Child.class));
    }

    private Class<?> transform(final String name) throws Exception {
        return this.transform(name, CallTarget.class);
    }

    private Class<?> transform(final String name, final Class<?> targetClass) throws Exception {
        ClassNode target = this.read(targetClass);
        this.handler.transform(this.transformerManager, target, this.transformer(name));
        return TestClassLoader.load(target);
    }

    private ClassNode transformer(final String name) throws Exception {
        ClassNode transformer = this.read(ArgumentsTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        return transformer;
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class Box {

        private final int first;
        private final int second;

        public Box(int first, int second) {
            this.first = first;
            this.second = second;
        }

        public int sum() {
            return this.first + this.second;
        }

    }

    public static class CallTarget {

        public static int call() {
            return sum(1, 2);
        }

        public static String callObject() {
            return concat("a", "b");
        }

        public static int callTyped() {
            return typed(1L, 2D);
        }

        public static int callNew() {
            return new Box(1, 2).sum();
        }

        public static int callWithArguments(int first, int second) {
            return sum(first, second);
        }

        public static int callInstance() {
            return new CallTarget().sum(1, 2);
        }

        public static void callNoArguments() {
            noArguments();
        }

        public static int sum(int first, int second) {
            return first + second;
        }

        public static String concat(String first, String second) {
            return first + second;
        }

        public static int typed(long first, double second) {
            return (int) (first + second);
        }

        public static int noArguments() {
            return 1;
        }

    }

    public static class Parent {

        public final int value;

        public Parent(int value) {
            this.value = value;
        }

    }

    public static class Child extends Parent {

        public final int own;

        public Child(int value) {
            super(value);
            this.own = value;
        }

    }

    public static class ArgumentsTransformer {

        @CModifyArgs(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$CallTarget;sum(II)I"))
        public static void all(CArgs args) {
            args.set(0, 10);
            args.set(1, 20);
        }

        @CModifyArgs(method = "callObject", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$CallTarget;concat(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
        public static void objects(CArgs args) {
            args.set(0, "A");
            args.set(1, "B");
        }

        @CModifyArgs(method = "callTyped", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$CallTarget;typed(JD)I"))
        public static void types(CArgs args) {
            args.setLong(0, 30L);
            args.setDouble(1, 3D);
        }

        @CModifyArgs(method = "callNew", target = @CTarget(value = "NEW", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$Box;"))
        public static void constructor(CArgs args) {
            args.set(0, 10);
            args.set(1, 20);
        }

        @CModifyArgs(method = "<init>", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$Parent;<init>(I)V"))
        public static void superConstructor(CArgs args) {
            args.set(0, 42);
        }

        @CModifyArgs(method = "callWithArguments", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$CallTarget;sum(II)I"))
        public static void capture(CArgs args, int first, int second) {
            args.set(0, first);
            args.set(1, second);
        }

        @CModifyArgs(method = "callInstance", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$CallTarget;sum(II)I"))
        public static void instance(CArgs args) {
            args.set(0, 10);
            args.set(1, 20);
        }

        @CModifyArgs(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$CallTarget;sum(II)I"))
        public static int nonVoid(CArgs args) {
            return 0;
        }

        @CModifyArgs(method = "callNoArguments", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$CallTarget;noArguments()I"))
        public static void noArguments(CArgs args) {
        }

        @CModifyArgs(method = "<init>", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgsAnnotationHandlerTest$Parent;<init>(I)V"))
        public void nonStaticSuper(CArgs args) {
            args.set(0, 42);
        }

    }

}
