package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyArg;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CModifyArgAnnotationHandlerTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CModifyArgAnnotationHandler handler = new CModifyArgAnnotationHandler();

    @Test
    public void modifyPrimitiveArgument() throws Exception {
        Class<?> target = this.transform("primitive");
        assertEquals(12, target.getMethod("call").invoke(null));
    }

    @Test
    public void modifySecondArgument() throws Exception {
        Class<?> target = this.transform("second");
        assertEquals(21, target.getMethod("call").invoke(null));
    }

    @Test
    public void modifyObjectArgument() throws Exception {
        Class<?> target = this.transform("object");
        assertEquals("VALUE", target.getMethod("callObject").invoke(null));
    }

    @Test
    public void modifyConstructorArgument() throws Exception {
        Class<?> target = this.transform("constructor");
        assertEquals(13, target.getMethod("callNew").invoke(null));
    }

    @Test
    public void modifyWithAllArguments() throws Exception {
        Class<?> target = this.transform("allArguments");
        assertEquals(14, target.getMethod("call").invoke(null));
    }

    @Test
    public void modifyInstanceInvocation() throws Exception {
        Class<?> target = this.transform("instance");
        assertEquals(12, target.getMethod("callInstance").invoke(null));
    }

    @Test
    public void rejectOutOfBoundsIndex() {
        assertThrows(TransformerException.class, () -> this.transform("outOfBounds"));
    }

    @Test
    public void rejectAmbiguousImplicitArgument() {
        assertThrows(TransformerException.class, () -> this.transform("ambiguous"));
    }

    @Test
    public void rejectNonStaticHandlerForStaticTarget() {
        assertThrows(TransformerException.class, () -> this.transform("nonStaticHandler"));
    }

    private Class<?> transform(final String name) throws Exception {
        ClassNode target = this.read(CallTarget.class);
        this.handler.transform(this.transformerManager, target, this.transformer(name));
        return TestClassLoader.load(target);
    }

    private ClassNode transformer(final String name) throws Exception {
        ClassNode transformer = this.read(ArgumentTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        return transformer;
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class Box {

        private final int value;

        public Box(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

    }

    public static class CallTarget {

        public static int call() {
            return sum(1, 2);
        }

        public static String callObject() {
            return echo("value");
        }

        public static int callNew() {
            return new Box(3).getValue();
        }

        public static int callInstance() {
            return new CallTarget().sumInstance(1, 2);
        }

        public static int sum(int first, int second) {
            return first + second;
        }

        public int sumInstance(int first, int second) {
            return first + second;
        }

        public static String echo(String value) {
            return value;
        }

    }

    public static class ArgumentTransformer {

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;sum(II)I"), index = 0)
        public static int primitive(int value) {
            return value + 9;
        }

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;sum(II)I"), index = 1)
        public static int second(int value) {
            return value + 18;
        }

        @CModifyArg(method = "callObject", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;echo(Ljava/lang/String;)Ljava/lang/String;"))
        public static String object(String value) {
            return value.toUpperCase();
        }

        @CModifyArg(method = "callNew", target = @CTarget(value = "NEW", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$Box;"))
        public static int constructor(int value) {
            return value + 10;
        }

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;sum(II)I"), index = 0)
        public static int allArguments(int first, int second) {
            return first + second + 9;
        }

        @CModifyArg(method = "callInstance", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;sumInstance(II)I"), index = 0)
        public static int instance(int value) {
            return value + 9;
        }

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;sum(II)I"), index = 3)
        public static int outOfBounds(int value) {
            return value + 9;
        }

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;sum(II)I"))
        public static int ambiguous(int value) {
            return value + 9;
        }

        @CModifyArg(method = "call", target = @CTarget(value = "INVOKE", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyArgAnnotationHandlerTest$CallTarget;sum(II)I"), index = 0)
        public int nonStaticHandler(int value) {
            return value + 9;
        }

    }

}
