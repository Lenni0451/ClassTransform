package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
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

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CModifyExpressionValueAnnotationHandlerTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CModifyExpressionValueAnnotationHandler handler = new CModifyExpressionValueAnnotationHandler();

    @ParameterizedTest
    @CsvSource({
            "intOpcode, 9",
            "intByte, 110",
            "intShort, 1010",
            "intLdc, 100010",
            "longOpcode, 10",
            "longLdc, 100010",
            "floatOpcode, 10.0",
            "floatLdc, 13.25",
            "doubleOpcode, 10.0",
            "doubleLdc, 13.25",
            "floatNaN, 1.0",
            "doubleNaN, 1.0",
            "booleanValue, false",
            "charValue, b",
            "nullValue, replacement",
            "stringValue, value!",
            "classValue, class java.lang.Integer",
            "multiple, 5",
            "overlap, 4",
            "slice, 4",
            "optional, 3",
            "newValue, VALUE",
            "fieldValue, 17"
    })
    public void modifyExpressions(final String name, final String expected) throws Exception {
        Class<?> target = this.transform(name);
        String method = name.equals("overlap") || name.equals("slice") || name.equals("optional") ? "multiple" : name;
        assertEquals(expected, String.valueOf(target.getMethod(method).invoke(null)));
    }

    @Test
    public void modifyNullArray() throws Exception {
        assertArrayEquals(new int[]{1, 2}, (int[]) this.transform("nullArray").getMethod("nullArray").invoke(null));
    }

    @Test
    public void distinguishSignedZeros() throws Exception {
        assertArrayEquals(new double[]{0D, 1D}, (double[]) this.transform("signedZeros").getMethod("signedZeros").invoke(null));
    }

    @Test
    public void captureAllArguments() throws Exception {
        assertEquals(15L, this.transform("capture").getMethod("capture", long.class, int.class, double.class, String.class).invoke(null, 2L, 3, 4D, "text"));
    }

    @Test
    public void captureArgumentPrefixAndLocal() throws Exception {
        assertEquals(7L, this.transform("prefix").getMethod("capture", long.class, int.class, double.class, String.class).invoke(null, 2L, 3, 4D, "text"));
    }

    @Test
    public void captureInvocationArguments() throws Exception {
        assertEquals(8, this.transform("invoke").getMethod("invoke", int.class).invoke(null, 5));
    }

    @Test
    public void captureInstanceArguments() throws Exception {
        ClassNode target = this.read(InstanceTarget.class);
        this.handler.transform(this.transformerManager, target, this.read(InstanceTransformer.class));
        Class<?> clazz = TestClassLoader.load(target);
        Object instance = clazz.getConstructor().newInstance();
        assertEquals(16.5D, clazz.getMethod("value", long.class, int.class, double.class, String.class).invoke(instance, 3L, 4, 5D, "hi"));
    }

    @Test
    public void chainConstantModifiers() throws Exception {
        ClassNode target = this.read(Target.class);
        this.handler.transform(this.transformerManager, target, this.transformer("intByte"));
        this.handler.transform(this.transformerManager, target, this.transformer("intByte"));
        assertEquals(120, TestClassLoader.load(target).getMethod("intByte").invoke(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"noTarget", "wrongValue", "wrongReturn", "wrongCapture", "tooManyArguments", "missing", "nonExpression"})
    public void rejectInvalidInjection(final String kind) throws Exception {
        ClassNode transformer = this.transformer("invoke");
        MethodNode method = transformer.methods.get(0);
        AnnotationNode annotation = AnnotationUtils.findAnnotation(method, CModifyExpressionValue.class).get();
        if (kind.equals("noTarget")) {
            annotation.values = Arrays.asList("method", Arrays.asList("invoke"));
        } else if (kind.equals("wrongValue")) {
            method.desc = "(JI)I";
        } else if (kind.equals("wrongReturn")) {
            method.desc = "(II)J";
        } else if (kind.equals("wrongCapture")) {
            method.desc = "(IJ)I";
        } else if (kind.equals("tooManyArguments")) {
            method.desc = "(III)I";
        } else {
            AnnotationNode target = new AnnotationNode(Type.getDescriptor(CTarget.class));
            target.values = kind.equals("missing") ? Arrays.asList("value", "CONSTANT", "target", "int 999") : Arrays.asList("value", "RETURN");
            annotation.values = Arrays.asList("method", Arrays.asList("invoke"), "target", target);
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
        ClassNode transformer = this.read(ExpressionTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        return transformer;
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class Target {

        public static int number = 7;

        public static int intOpcode() {
            return -1;
        }

        public static int intByte() {
            return 100;
        }

        public static int intShort() {
            return 1000;
        }

        public static int intLdc() {
            return 100000;
        }

        public static long longOpcode() {
            return 0L;
        }

        public static long longLdc() {
            return 100000L;
        }

        public static float floatOpcode() {
            return 0F;
        }

        public static float floatLdc() {
            return 3.25F;
        }

        public static double doubleOpcode() {
            return 0D;
        }

        public static double doubleLdc() {
            return 3.25D;
        }

        public static float floatNaN() {
            return Float.NaN;
        }

        public static double doubleNaN() {
            return Double.NaN;
        }

        public static double[] signedZeros() {
            return new double[]{0D, -0D};
        }

        public static boolean booleanValue() {
            return true;
        }

        public static char charValue() {
            return 'a';
        }

        public static String nullValue() {
            return null;
        }

        public static int[] nullArray() {
            return null;
        }

        public static String stringValue() {
            return "value";
        }

        public static Class<?> classValue() {
            return String.class;
        }

        public static int multiple() {
            return Math.abs(1) + Math.abs(2);
        }

        public static long capture(long first, int second, double third, String fourth) {
            return 2L;
        }

        public static int invoke(int value) {
            return Math.abs(-3);
        }

        public static String newValue() {
            return new String("value");
        }

        public static int fieldValue() {
            return number;
        }

    }

    public static class InstanceTarget {

        public double value(long first, int second, double third, String fourth) {
            return Math.abs(-2.5D);
        }

    }

    public static class ExpressionTransformer {

        @CModifyExpressionValue(method = "intOpcode", target = @CTarget(value = "CONSTANT", target = "int -1"))
        public static int intOpcode(int original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "intByte", target = @CTarget(value = "CONSTANT", target = "int 100"))
        public static int intByte(int original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "intShort", target = @CTarget(value = "CONSTANT", target = "int 1000"))
        public static int intShort(int original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "intLdc", target = @CTarget(value = "CONSTANT", target = "int 100000"))
        public static int intLdc(int original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "longOpcode", target = @CTarget(value = "CONSTANT", target = "long 0"))
        public static long longOpcode(long original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "longLdc", target = @CTarget(value = "CONSTANT", target = "long 100000"))
        public static long longLdc(long original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "floatOpcode", target = @CTarget(value = "CONSTANT", target = "float 0"))
        public static float floatOpcode(float original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "floatLdc", target = @CTarget(value = "CONSTANT", target = "float 3.25"))
        public static float floatLdc(float original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "doubleOpcode", target = @CTarget(value = "CONSTANT", target = "double 0"))
        public static double doubleOpcode(double original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "doubleLdc", target = @CTarget(value = "CONSTANT", target = "double 3.25"))
        public static double doubleLdc(double original) {
            return original + 10;
        }

        @CModifyExpressionValue(method = "floatNaN", target = @CTarget(value = "CONSTANT", target = "float NaN"))
        public static float floatNaN(float original) {
            return Float.isNaN(original) ? 1F : original;
        }

        @CModifyExpressionValue(method = "doubleNaN", target = @CTarget(value = "CONSTANT", target = "double NaN"))
        public static double doubleNaN(double original) {
            return Double.isNaN(original) ? 1D : original;
        }

        @CModifyExpressionValue(method = "signedZeros", target = @CTarget(value = "CONSTANT", target = "double -0.0"))
        public static double signedZeros(double original) {
            return original + 1;
        }

        @CModifyExpressionValue(method = "booleanValue", target = @CTarget(value = "CONSTANT", target = "int 1"))
        public static boolean booleanValue(boolean original) {
            return !original;
        }

        @CModifyExpressionValue(method = "charValue", target = @CTarget(value = "CONSTANT", target = "int 97"))
        public static char charValue(char original) {
            return (char) (original + 1);
        }

        @CModifyExpressionValue(method = "nullValue", target = @CTarget(value = "CONSTANT", target = "null"))
        public static String nullValue(String original) {
            return original == null ? "replacement" : original;
        }

        @CModifyExpressionValue(method = "nullArray", target = @CTarget(value = "CONSTANT", target = "null"))
        public static int[] nullArray(int[] original) {
            return original == null ? new int[]{1, 2} : original;
        }

        @CModifyExpressionValue(method = "stringValue", target = @CTarget(value = "CONSTANT", target = "string value"))
        public static String stringValue(String original) {
            return original + "!";
        }

        @CModifyExpressionValue(method = "classValue", target = @CTarget(value = "CONSTANT", target = "type Ljava/lang/String;"))
        public static Class<?> classValue(Class<?> original) {
            return original == String.class ? Integer.class : original;
        }

        @CModifyExpressionValue(method = "multiple", targets = {@CTarget(value = "CONSTANT", target = "int 1"), @CTarget(value = "CONSTANT", target = "int 2")})
        public static int multiple(int original) {
            return original + 1;
        }

        @CModifyExpressionValue(method = "multiple", target = @CTarget(value = "CONSTANT", target = "int 1"), targets = @CTarget(value = "CONSTANT", target = "int 1"))
        public static int overlap(int original) {
            return original + 1;
        }

        @CModifyExpressionValue(method = "multiple", target = @CTarget(value = "CONSTANT", target = "int 2"), slice = @CSlice(from = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I", ordinal = 0)))
        public static int slice(int original) {
            return original + 1;
        }

        @CModifyExpressionValue(method = "multiple", target = @CTarget(value = "CONSTANT", target = "int 99", optional = true))
        public static int optional(int original) {
            return original + 1;
        }

        @CModifyExpressionValue(method = "capture", target = @CTarget(value = "CONSTANT", target = "long 2"))
        public static long capture(long original, long first, int second, double third, String fourth) {
            return original + first + second + (long) third + fourth.length();
        }

        @CModifyExpressionValue(method = "capture", target = @CTarget(value = "CONSTANT", target = "long 2"))
        public static long prefix(long original, long first, @CLocalVariable(index = 2, loadOpcode = Opcodes.ILOAD) int second) {
            return original + first + second;
        }

        @CModifyExpressionValue(method = "invoke", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(I)I"))
        public static int invoke(int original, int value) {
            return original + value;
        }

        @CModifyExpressionValue(method = "newValue", target = @CTarget(value = "NEW", target = "java/lang/String"))
        public static String newValue(String original) {
            return original.toUpperCase();
        }

        @CModifyExpressionValue(method = "fieldValue", target = @CTarget(value = "GETFIELD", target = "Lnet/lenni0451/classtransform/transformer/impl/CModifyExpressionValueAnnotationHandlerTest$Target;number:I"))
        public static int fieldValue(int original) {
            return original + 10;
        }

    }

    public static class InstanceTransformer {

        @CModifyExpressionValue(method = "value", target = @CTarget(value = "INVOKE", target = "Ljava/lang/Math;abs(D)D"))
        public double value(double original, long first, int second, double third, String fourth) {
            return original + first + second + third + fourth.length();
        }

    }

}
