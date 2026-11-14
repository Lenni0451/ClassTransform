package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CModifyReturnValueAnnotationHandlerTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CModifyReturnValueAnnotationHandler handler = new CModifyReturnValueAnnotationHandler();

    @ParameterizedTest
    @CsvSource({
            "booleanValue, true",
            "byteValue, 3",
            "shortValue, 4",
            "charValue, b",
            "intValue, 5",
            "longValue, 6",
            "floatValue, 7.0",
            "doubleValue, 8.0",
            "stringValue, modified"
    })
    public void modifyReturnTypes(final String method, final String expected) throws Exception {
        Class<?> target = this.transform("types");
        assertEquals(expected, String.valueOf(target.getMethod(method).invoke(null)));
    }

    @Test
    public void modifyArrayReturn() throws Exception {
        assertArrayEquals(new int[]{2, 3}, (int[]) this.transform("types").getMethod("arrayValue").invoke(null));
    }

    @Test
    public void modifyInstanceReturns() throws Exception {
        ClassNode target = this.read(InstanceTarget.class);
        this.handler.transform(this.transformerManager, target, this.read(InstanceTransformer.class));
        Class<?> clazz = TestClassLoader.load(target);
        Object instance = clazz.getConstructor().newInstance();
        assertEquals(12L, clazz.getMethod("longValue").invoke(instance));
        assertEquals(13D, clazz.getMethod("doubleValue").invoke(instance));
        assertEquals("instance!", clazz.getMethod("stringValue").invoke(instance));
    }

    @Test
    public void captureReturnArguments() throws Exception {
        assertEquals(15L, this.transform("capture").getMethod("capture", long.class, int.class, double.class, String.class).invoke(null, 2L, 3, 4D, "text"));
    }

    @Test
    public void captureReturnArgumentPrefix() throws Exception {
        assertEquals(4L, this.transform("prefix").getMethod("capture", long.class, int.class, double.class, String.class).invoke(null, 2L, 3, 4D, "text"));
    }

    @ParameterizedTest
    @CsvSource({
            "allReturns, -1, 11",
            "allReturns, 0, 12",
            "allReturns, 1, 13",
            "ordinal, -1, 1",
            "ordinal, 0, 12",
            "ordinal, 1, 3",
            "tail, -1, 1",
            "tail, 0, 2",
            "tail, 1, 13",
            "slice, -1, 1",
            "slice, 0, 12",
            "slice, 1, 3",
            "tailSlice, -1, 1",
            "tailSlice, 0, 12",
            "tailSlice, 1, 3",
            "overlapping, -1, 11",
            "overlapping, 1, 13",
            "optional, 1, 3",
            "local, 4, 7"
    })
    public void selectReturns(final String transformer, final int argument, final int expected) throws Exception {
        assertEquals(expected, this.transform(transformer).getMethod("branch", int.class).invoke(null, argument));
    }

    @Test
    public void chainTransformers() throws Exception {
        ClassNode target = this.read(Target.class);
        this.handler.transform(this.transformerManager, target, this.transformer("allReturns"));
        this.handler.transform(this.transformerManager, target, this.transformer("multiply"));
        assertEquals(26, TestClassLoader.load(target).getMethod("branch", int.class).invoke(null, 1));
    }

    @Test
    public void useTransformerManager() throws Exception {
        this.transformerManager.setFailStrategy(FailStrategy.THROW);
        this.transformerManager.addTransformer(this.transformer("allReturns"));
        byte[] transformed = this.transformerManager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(transformed);
        assertEquals(13, TestClassLoader.load(ASMUtils.fromBytes(transformed)).getMethod("branch", int.class).invoke(null, 1));
    }

    @Test
    public void propagateHandlerException() throws Exception {
        Class<?> target = this.transform("throwing");
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> target.getMethod("branch", int.class).invoke(null, 1));
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"voidTarget", "missing", "invalidTarget", "wrongArgument", "wrongReturn", "wrongStatic", "extraArgument", "noArgument"})
    public void rejectInvalidInjection(final String kind) throws Exception {
        ClassNode transformer = this.transformer("allReturns");
        MethodNode method = transformer.methods.stream().filter(m -> m.name.equals("allReturns")).findFirst().get();
        AnnotationNode annotation = AnnotationUtils.findAnnotation(method, CModifyReturnValue.class).get();
        if (kind.equals("voidTarget")) {
            annotation.values = Arrays.asList("method", Arrays.asList("voidValue"));
        } else if (kind.equals("missing") || kind.equals("invalidTarget")) {
            AnnotationNode target = new AnnotationNode(Type.getDescriptor(CTarget.class));
            target.values = kind.equals("missing") ? Arrays.asList("value", "RETURN", "ordinal", 10) : Arrays.asList("value", "HEAD");
            annotation.values = Arrays.asList("method", Arrays.asList("branch"), "target", Arrays.asList(target));
        } else if (kind.equals("wrongArgument")) {
            method.desc = "(J)I";
        } else if (kind.equals("wrongReturn")) {
            method.desc = "(I)J";
        } else if (kind.equals("wrongStatic")) {
            method.access &= ~Opcodes.ACC_STATIC;
        } else if (kind.equals("extraArgument")) {
            method.desc = "(III)I";
        } else if (kind.equals("noArgument")) {
            method.desc = "()I";
        }
        ClassNode target = this.read(Target.class);
        assertThrows(TransformerException.class, () -> this.handler.transform(this.transformerManager, target, transformer));
    }

    private Class<?> transform(final String name) throws Exception {
        ClassNode target = this.read(Target.class);
        this.handler.transform(this.transformerManager, target, this.transformer(name));
        return TestClassLoader.load(target);
    }

    private ClassNode transformer(final String name) throws Exception {
        ClassNode transformer = this.read(ReturnTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name) && !(name.equals("types") && method.name.startsWith("type")));
        return transformer;
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class Target {

        public static boolean booleanValue() {
            return false;
        }

        public static byte byteValue() {
            return 1;
        }

        public static short shortValue() {
            return 2;
        }

        public static char charValue() {
            return 'a';
        }

        public static int intValue() {
            return 3;
        }

        public static long longValue() {
            return 4L;
        }

        public static float floatValue() {
            return 5F;
        }

        public static double doubleValue() {
            return 6D;
        }

        public static String stringValue() {
            return null;
        }

        public static int[] arrayValue() {
            return new int[]{1, 2};
        }

        public static int branch(int value) {
            if (value < 0) return 1;
            if (value == 0) return 2;
            return 3;
        }

        public static void voidValue() {
        }

        public static long capture(long first, int second, double third, String fourth) {
            return 2L;
        }

    }

    public static class InstanceTarget {

        public long longValue() {
            return 2L;
        }

        public double doubleValue() {
            return 3D;
        }

        public String stringValue() {
            return "instance";
        }

    }

    @CTransformer(Target.class)
    public static class ReturnTransformer {

        @CModifyReturnValue(method = "booleanValue")
        public static boolean typeBoolean(boolean original) {
            return !original;
        }

        @CModifyReturnValue(method = "byteValue")
        public static byte typeByte(byte original) {
            return (byte) (original + 2);
        }

        @CModifyReturnValue(method = "shortValue")
        public static short typeShort(short original) {
            return (short) (original + 2);
        }

        @CModifyReturnValue(method = "charValue")
        public static char typeChar(char original) {
            return (char) (original + 1);
        }

        @CModifyReturnValue(method = "intValue")
        public static int typeInt(int original) {
            return original + 2;
        }

        @CModifyReturnValue(method = "longValue")
        public static long typeLong(long original) {
            return original + 2;
        }

        @CModifyReturnValue(method = "floatValue")
        public static float typeFloat(float original) {
            return original + 2;
        }

        @CModifyReturnValue(method = "doubleValue")
        public static double typeDouble(double original) {
            return original + 2;
        }

        @CModifyReturnValue(method = "stringValue")
        public static Object typeString(Object original) {
            return original == null ? "modified" : original;
        }

        @CModifyReturnValue(method = "arrayValue")
        public static int[] typeArray(int[] original) {
            return new int[]{original[0] + 1, original[1] + 1};
        }

        @CModifyReturnValue(method = "branch")
        public static int allReturns(int original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "branch", target = @CTarget(value = "RETURN", ordinal = 1))
        public static int ordinal(int original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "branch", target = @CTarget("TAIL"))
        public static int tail(int original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "branch", slice = @CSlice(from = @CTarget(value = "RETURN", ordinal = 1), to = @CTarget(value = "RETURN", ordinal = 1)))
        public static int slice(int original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "branch", target = @CTarget("TAIL"), slice = @CSlice(to = @CTarget(value = "RETURN", ordinal = 1)))
        public static int tailSlice(int original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "branch", target = {@CTarget("RETURN"), @CTarget("TAIL")})
        public static int overlapping(int original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "branch", target = @CTarget(value = "RETURN", ordinal = 10, optional = true))
        public static int optional(int original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "branch")
        public static int multiply(int original) {
            return original * 2;
        }

        @CModifyReturnValue(method = "branch")
        public static int throwing(int original) {
            throw new IllegalStateException("handler");
        }

        @CModifyReturnValue(method = "branch")
        public static int local(int original, @CLocalVariable(index = 0) int value) {
            return original + value;
        }

        @CModifyReturnValue(method = "capture")
        public static long capture(long original, long first, int second, double third, String fourth) {
            return original + first + second + (long) third + fourth.length();
        }

        @CModifyReturnValue(method = "capture")
        public static long prefix(long original, long first) {
            return original + first;
        }

    }

    public static class InstanceTransformer {

        @CModifyReturnValue(method = "longValue")
        public long modifyLong(long original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "doubleValue")
        public double modifyDouble(double original) {
            return original + 10;
        }

        @CModifyReturnValue(method = "stringValue")
        public String modifyString(String original) {
            return original + "!";
        }

    }

}
