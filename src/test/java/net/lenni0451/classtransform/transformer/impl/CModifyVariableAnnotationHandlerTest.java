package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyVariable;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CModifyVariableAnnotationHandlerTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CModifyVariableAnnotationHandler handler = new CModifyVariableAnnotationHandler();

    @Test
    public void modifyByIndex() throws Exception {
        Class<?> target = this.transform("index");
        assertEquals(15, target.getMethod("parameter", int.class).invoke(null, 5));
    }

    @Test
    public void modifyByName() throws Exception {
        Class<?> target = this.transform("name");
        assertEquals(15, target.getMethod("parameter", int.class).invoke(null, 5));
    }

    @Test
    public void modifyByOrdinal() throws Exception {
        Class<?> target = this.transform("ordinal");
        assertEquals(28, target.getMethod("parameter", int.class, int.class).invoke(null, 5, 3));
    }

    @Test
    public void modifyOnlyArguments() throws Exception {
        Class<?> target = this.transform("argsOnly");
        assertEquals(15, target.getMethod("parameterAndLocal", int.class).invoke(null, 5));
    }

    @Test
    public void modifyImplicit() throws Exception {
        Class<?> target = this.transform("implicit");
        assertEquals(6, target.getMethod("object", String.class).invoke(null, "value"));
    }

    @Test
    public void modifyLocalVariable() throws Exception {
        Class<?> target = this.transform("local");
        assertEquals(15, target.getMethod("local").invoke(null));
    }

    @Test
    public void modifyBeforeLoad() throws Exception {
        Class<?> target = this.transform("load");
        assertEquals(13, target.getMethod("load").invoke(null));
    }

    @Test
    public void modifyBeforeLoadWithOrdinal() throws Exception {
        Class<?> target = this.transform("loadOrdinal");
        assertEquals(11, target.getMethod("loadOrdinal").invoke(null));
    }

    @Test
    public void modifyAfterStore() throws Exception {
        Class<?> target = this.transform("store");
        assertEquals(15, target.getMethod("store").invoke(null));
    }

    @Test
    public void captureTargetArguments() throws Exception {
        Class<?> target = this.transform("capture");
        assertEquals(18, target.getMethod("parameterWithExtra", int.class, int.class).invoke(null, 5, 3));
    }

    @Test
    public void modifyInstanceMethod() throws Exception {
        ClassNode targetNode = this.read(InstanceTarget.class);
        this.handler.transform(this.transformerManager, targetNode, this.transformer("instance"));
        Class<?> target = TestClassLoader.load(targetNode);
        Object instance = target.getConstructor().newInstance();
        assertEquals(15, target.getMethod("parameter", int.class).invoke(instance, 5));
    }

    @Test
    public void printDoesNotAbort() throws Exception {
        Class<?> target = this.transform("print");
        assertEquals(15, target.getMethod("parameter", int.class).invoke(null, 5));
    }

    @Test
    public void rejectVoidHandler() {
        assertThrows(TransformerException.class, () -> this.transform("voidHandler"));
    }

    @Test
    public void rejectAmbiguousImplicitVariable() {
        assertThrows(TransformerException.class, () -> this.transform("ambiguous"));
    }

    @Test
    public void rejectUnknownIndex() throws Exception {
        Class<?> target = this.transform("unknownIndex");
        assertEquals(5, target.getMethod("parameter", int.class).invoke(null, 5));
    }

    private Class<?> transform(final String name) throws Exception {
        ClassNode target = this.read(VariableTarget.class);
        this.handler.transform(this.transformerManager, target, this.transformer(name));
        return TestClassLoader.load(target);
    }

    private ClassNode transformer(final String name) throws Exception {
        ClassNode transformer = this.read(VariableTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        return transformer;
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class VariableTarget {

        public static int parameter(int value) {
            return value;
        }

        public static int parameter(int first, int second) {
            return first + second;
        }

        public static int parameterAndLocal(int value) {
            int local = value;
            return value + local - local;
        }

        public static int object(String value) {
            return value.length();
        }

        public static int local() {
            int local = 5;
            return local;
        }

        public static int load() {
            int local = 5;
            return local + local;
        }

        public static int loadOrdinal() {
            int local = 5;
            int first = local;
            int second = local;
            return first + second;
        }

        public static int store() {
            int local = 5;
            return local;
        }

        public static int parameterWithExtra(int value, int extra) {
            return value;
        }

        public static int ambiguous(int first, int second) {
            return first + second;
        }

    }

    public static class InstanceTarget {

        public int parameter(int value) {
            return value;
        }

    }

    public static class VariableTransformer {

        @CModifyVariable(method = "parameter", target = @CTarget("HEAD"), index = 0)
        public static int index(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "parameter", target = @CTarget("HEAD"), name = "value")
        public static int name(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "parameter", target = @CTarget("HEAD"), ordinal = 1)
        public static int ordinal(int value) {
            return value + 20;
        }

        @CModifyVariable(method = "parameterAndLocal", target = @CTarget("HEAD"), index = 0, argsOnly = true)
        public static int argsOnly(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "object", target = @CTarget("HEAD"))
        public static String implicit(String value) {
            return "!" + value;
        }

        @CModifyVariable(method = "local", target = @CTarget("STORE"), index = 0)
        public static int local(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "load", target = @CTarget("LOAD"), name = "local")
        public static int load(int value) {
            return value + 1;
        }

        @CModifyVariable(method = "loadOrdinal", target = @CTarget(value = "LOAD", ordinal = 1), name = "local")
        public static int loadOrdinal(int value) {
            return value + 1;
        }

        @CModifyVariable(method = "store", target = @CTarget("STORE"), name = "local")
        public static int store(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "parameterWithExtra", target = @CTarget("HEAD"), index = 0)
        public static int capture(int value, int targetValue, int targetExtra) {
            return value + 10 + targetExtra;
        }

        @CModifyVariable(method = "parameter", target = @CTarget("HEAD"), index = 1)
        public int instance(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "parameter", target = @CTarget("HEAD"), index = 0, print = true)
        public static int print(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "parameter", target = @CTarget("HEAD"), index = 0)
        public static void voidHandler(int value) {
        }

        @CModifyVariable(method = "ambiguous", target = @CTarget("HEAD"))
        public static int ambiguous(int value) {
            return value + 10;
        }

        @CModifyVariable(method = "parameter", target = @CTarget("HEAD"), index = 5, require = 0)
        public static int unknownIndex(int value) {
            return value + 10;
        }

    }

}
