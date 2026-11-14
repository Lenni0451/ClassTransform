package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
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
import org.objectweb.asm.tree.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CWrapConditionAnnotationHandlerTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/impl/CWrapConditionAnnotationHandlerTest$Target;";
    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CWrapConditionAnnotationHandler handler = new CWrapConditionAnnotationHandler();

    @ParameterizedTest
    @CsvSource({"true, 14", "false, 0"})
    public void captureMethodArguments(final boolean enabled, final long expected) throws Exception {
        Class<?> target = this.transform("call");
        assertEquals(expected, target.getMethod("call", boolean.class, long.class, int.class, double.class, String.class).invoke(null, enabled, 3L, 4, 5D, "hi"));
        assertEquals(1, target.getField("checks").get(null));
    }

    @ParameterizedTest
    @CsvSource({"true, 14", "false, 0"})
    public void captureInstanceMethodArguments(final boolean enabled, final long expected) throws Exception {
        Class<?> target = this.transform("virtualCall");
        Object instance = target.getConstructor().newInstance();
        assertEquals(expected, target.getMethod("virtualCall", boolean.class, long.class, int.class, double.class, String.class).invoke(instance, enabled, 3L, 4, 5D, "hi"));
    }

    @ParameterizedTest
    @CsvSource({"field, true, 9", "field, false, 0", "virtualField, true, 9", "virtualField, false, 0"})
    public void captureFieldArguments(final String name, final boolean enabled, final long expected) throws Exception {
        Class<?> target = this.transform(name);
        Object instance = name.equals("virtualField") ? target.getConstructor().newInstance() : null;
        assertEquals(expected, target.getMethod(name, boolean.class, long.class).invoke(instance, enabled, 9L));
    }

    @ParameterizedTest
    @CsvSource({"true, true, 1, 1", "true, false, 0, 1", "false, true, 0, 0", "false, false, 0, 0"})
    public void chainConditions(final boolean first, final boolean second, final int expectedCalls, final int expectedSecondChecks) throws Exception {
        ClassNode target = this.read(Target.class);
        this.handler.transform(this.transformerManager, target, this.transformer("first"));
        this.handler.transform(this.transformerManager, target, this.transformer("second"));
        Class<?> clazz = TestClassLoader.load(target);
        assertEquals(expectedCalls, clazz.getMethod("chain", boolean.class, boolean.class).invoke(null, first, second));
        assertEquals(1, clazz.getField("checks").get(null));
        assertEquals(expectedSecondChecks, clazz.getField("secondChecks").get(null));
    }

    @Test
    public void evaluateOperationArgumentsOnce() throws Exception {
        Class<?> target = this.transform("deny");
        assertEquals(0L, target.getMethod("sideEffects").invoke(null));
        assertEquals(1, target.getField("evaluations").get(null));
    }

    @Test
    public void deduplicateOverlappingTargets() throws Exception {
        Class<?> target = this.transform("overlap");
        assertEquals(1, target.getMethod("chain", boolean.class, boolean.class).invoke(null, true, true));
        assertEquals(1, target.getField("checks").get(null));
    }

    @Test
    public void wrapPoppedMethodResult() throws Exception {
        Class<?> target = this.transform("popCall");
        target.getField("calls").set(null, 0);
        target.getMethod("popCall", boolean.class).invoke(null, false);
        assertEquals(0, target.getField("calls").get(null));
        assertEquals(1, target.getField("checks").get(null));

        target.getField("calls").set(null, 0);
        target.getMethod("popCall", boolean.class).invoke(null, true);
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void wrapPoppedStaticFieldGetter() throws Exception {
        ClassNode target = this.read(Target.class);
        this.addFieldPops(target);
        this.handler.transform(this.transformerManager, target, this.transformer("popField"));
        Class<?> clazz = TestClassLoader.load(target);
        assertDoesNotThrow(() -> clazz.getMethod("popField", boolean.class).invoke(null, true));
        assertDoesNotThrow(() -> clazz.getMethod("popField", boolean.class).invoke(null, false));
        assertEquals(2, clazz.getField("checks").get(null));
    }

    @Test
    public void wrapPoppedInstanceFieldGetter() throws Exception {
        ClassNode target = this.read(Target.class);
        this.addFieldPops(target);
        this.handler.transform(this.transformerManager, target, this.transformer("popInstanceField"));
        Class<?> clazz = TestClassLoader.load(target);
        Object instance = clazz.getConstructor().newInstance();
        assertDoesNotThrow(() -> clazz.getMethod("popInstanceField", boolean.class).invoke(instance, true));
        assertDoesNotThrow(() -> clazz.getMethod("popInstanceField", boolean.class).invoke(instance, false));
        assertEquals(2, clazz.getField("checks").get(null));
    }

    @Test
    public void wrapMultipleTargets() throws Exception {
        Class<?> target = this.transform("multiple");
        assertEquals(2, target.getMethod("multiple").invoke(null));
        assertEquals(2, target.getField("checks").get(null));
    }

    @ParameterizedTest
    @CsvSource({"true, 1", "false, 0"})
    public void passAllOperationArgumentTypes(final boolean enabled, final int expected) throws Exception {
        assertEquals(expected, this.transform("allTypes").getMethod("allTypes", boolean.class).invoke(null, enabled));
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrongReturn", "wrongCapture", "missingArgument", "tooManyArguments", "fieldGet", "valueInvocation", "constructor"})
    public void rejectInvalidInjection(final String kind) throws Exception {
        ClassNode transformer = this.transformer("field");
        MethodNode method = transformer.methods.get(0);
        if (kind.equals("wrongReturn")) {
            method.desc = "(JZJ)I";
        } else if (kind.equals("wrongCapture")) {
            method.desc = "(JIZ)Z";
        } else if (kind.equals("missingArgument")) {
            method.desc = "(Z)Z";
        } else if (kind.equals("tooManyArguments")) {
            method.desc = "(JZJI)Z";
        } else {
            AnnotationNode annotation = AnnotationUtils.findAnnotation(method, CWrapCondition.class).get();
            AnnotationNode target = new AnnotationNode(Type.getDescriptor(CTarget.class));
            if (kind.equals("fieldGet")) {
                target.values = Arrays.asList("value", "GETFIELD", "target", TARGET + "value:J");
                annotation.values = Arrays.asList("method", Arrays.asList("field"), "target", Arrays.asList(target));
            } else if (kind.equals("valueInvocation")) {
                target.values = Arrays.asList("value", "INVOKE", "target", TARGET + "argument()J");
                annotation.values = Arrays.asList("method", Arrays.asList("sideEffects"), "target", Arrays.asList(target));
            } else {
                target.values = Arrays.asList("value", "NEW", "target", "java/lang/String");
                annotation.values = Arrays.asList("method", Arrays.asList("construct"), "target", Arrays.asList(target));
            }
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
        ClassNode transformer = this.read(ConditionTransformer.class);
        transformer.methods.removeIf(method -> !method.name.equals(name));
        return transformer;
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }

    private void addFieldPops(final ClassNode target) {
        String targetClass = TARGET.substring(1, TARGET.length() - 1);
        MethodNode staticPop = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "popField", "(Z)V", null, null);
        staticPop.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, targetClass, "value", "J"));
        staticPop.instructions.add(new InsnNode(Opcodes.POP2));
        staticPop.instructions.add(new InsnNode(Opcodes.RETURN));
        target.methods.add(staticPop);

        MethodNode instancePop = new MethodNode(Opcodes.ACC_PUBLIC, "popInstanceField", "(Z)V", null, null);
        instancePop.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instancePop.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, targetClass, "instanceValue", "J"));
        instancePop.instructions.add(new InsnNode(Opcodes.POP2));
        instancePop.instructions.add(new InsnNode(Opcodes.RETURN));
        target.methods.add(instancePop);
    }


    public static class Target {

        public static long value;
        public static int checks;
        public static int secondChecks;
        public static int evaluations;
        public static int calls;
        public long instanceValue;

        public static long call(boolean enabled, long first, int second, double third, String fourth) {
            consume(first, second, third, fourth);
            return value;
        }

        public long virtualCall(boolean enabled, long first, int second, double third, String fourth) {
            this.consumeInstance(first, second, third, fourth);
            return this.instanceValue;
        }

        public static void consume(long first, int second, double third, String fourth) {
            value = first + second + (long) third + fourth.length();
        }

        public void consumeInstance(long first, int second, double third, String fourth) {
            this.instanceValue = first + second + (long) third + fourth.length();
        }

        public static long field(boolean enabled, long input) {
            value = input;
            return value;
        }

        public long virtualField(boolean enabled, long input) {
            this.instanceValue = input;
            return this.instanceValue;
        }

        public static int chain(boolean first, boolean second) {
            hit();
            return calls;
        }

        public static int multiple() {
            hit();
            otherHit();
            return calls;
        }

        public static void popCall(final boolean enabled) {
            compute(enabled ? 1 : 0);
        }

        public static int compute(final int value) {
            calls++;
            return value;
        }

        public static void hit() {
            calls++;
        }

        public static void otherHit() {
            calls++;
        }

        public static long argument() {
            evaluations++;
            return 3L;
        }

        public static long sideEffects() {
            consume(argument(), 4, 5D, "hi");
            return value;
        }

        public static Object construct() {
            return new String("value");
        }

        public static int allTypes(boolean enabled) {
            consumeAll(true, (byte) 2, 'a', (short) 3, 4, 5F, 6L, 7D, new Object(), new int[]{8});
            return calls;
        }

        public static void consumeAll(boolean flag, byte b, char c, short s, int i, float f, long l, double d, Object object, int[] array) {
            calls++;
        }

    }

    public static class ConditionTransformer {

        @CWrapCondition(method = "call", target = @CTarget(value = "INVOKE", target = TARGET + "consume(JIDLjava/lang/String;)V"))
        public static boolean call(long first, int second, double third, String fourth, boolean enabled, long outerFirst, int outerSecond, double outerThird, String outerFourth) {
            Target.checks++;
            return enabled && first == outerFirst && second == outerSecond && third == outerThird && fourth.equals(outerFourth);
        }

        @CWrapCondition(method = "virtualCall", target = @CTarget(value = "INVOKE", target = TARGET + "consumeInstance(JIDLjava/lang/String;)V"))
        public boolean virtualCall(Target receiver, long first, int second, double third, String fourth, boolean enabled) {
            return receiver != null && enabled && first == 3 && second == 4 && third == 5 && fourth.equals("hi");
        }

        @CWrapCondition(method = "field", target = @CTarget(value = "PUTFIELD", target = TARGET + "value:J"))
        public static boolean field(long value, boolean enabled, long input) {
            return enabled && value == input;
        }

        @CWrapCondition(method = "virtualField", target = @CTarget(value = "PUTFIELD", target = TARGET + "instanceValue:J"))
        public boolean virtualField(Target receiver, long value, boolean enabled, long input) {
            return receiver != null && enabled && value == input;
        }

        @CWrapCondition(method = "chain", target = @CTarget(value = "INVOKE", target = TARGET + "hit()V"))
        public static boolean first(boolean first) {
            Target.checks++;
            return first;
        }

        @CWrapCondition(method = "chain", target = @CTarget(value = "INVOKE", target = TARGET + "hit()V"))
        public static boolean second(boolean first, boolean second) {
            Target.secondChecks++;
            return second;
        }

        @CWrapCondition(method = "sideEffects", target = @CTarget(value = "INVOKE", target = TARGET + "consume(JIDLjava/lang/String;)V"))
        public static boolean deny() {
            return false;
        }

        @CWrapCondition(method = "chain", target = {
                @CTarget(value = "INVOKE", target = TARGET + "hit()V"),
                @CTarget(value = "INVOKE", target = TARGET + "hit()V")
        })
        public static boolean overlap() {
            Target.checks++;
            return true;
        }

        @CWrapCondition(method = "multiple", target = {
                @CTarget(value = "INVOKE", target = TARGET + "hit()V"),
                @CTarget(value = "INVOKE", target = TARGET + "otherHit()V")
        })
        public static boolean multiple() {
            Target.checks++;
            return true;
        }

        @CWrapCondition(method = "allTypes", target = @CTarget(value = "INVOKE", target = TARGET + "consumeAll(ZBCSIFJDLjava/lang/Object;[I)V"))
        public static boolean allTypes(boolean flag, byte b, char c, short s, int i, float f, long l, double d, Object object, int[] array, boolean enabled) {
            return enabled && flag && b == 2 && c == 'a' && s == 3 && i == 4 && f == 5F && l == 6L && d == 7D && object != null && array[0] == 8;
        }

        @CWrapCondition(method = "popCall", target = @CTarget(value = "INVOKE", target = TARGET + "compute(I)I"))
        public static boolean popCall(final int value, final boolean enabled) {
            Target.checks++;
            return enabled;
        }

        @CWrapCondition(method = "popField", target = @CTarget(value = "FIELD", target = TARGET + "value:J"))
        public static boolean popField(final boolean enabled) {
            Target.checks++;
            return enabled;
        }

        @CWrapCondition(method = "popInstanceField", target = @CTarget(value = "GETFIELD", target = TARGET + "instanceValue:J"))
        public boolean popInstanceField(final Target receiver, final boolean enabled) {
            Target.checks++;
            return receiver != null && enabled;
        }

    }

}
