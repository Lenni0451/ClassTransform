package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.annotations.injection.CModifyReceiver;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CModifyReceiverAnnotationHandlerTest {

    private static final String PERSON = "Lnet/lenni0451/classtransform/transformer/impl/CModifyReceiverAnnotationHandlerTest$Person;";
    private static final String SUPER_BASE = "Lnet/lenni0451/classtransform/transformer/impl/CModifyReceiverAnnotationHandlerTest$SuperBase;";

    private final BasicClassProvider classProvider = new BasicClassProvider();
    private final TransformerManager transformerManager = new TransformerManager(this.classProvider);
    private final CModifyReceiverAnnotationHandler handler = new CModifyReceiverAnnotationHandler();

    @Test
    public void modifyVirtualReceiver() throws Exception {
        assertEquals(10L, this.transform(Target.class, Transformer.class, "virtualCall").getMethod("virtualCall").invoke(null));
    }

    @Test
    public void modifyInterfaceReceiver() throws Exception {
        assertEquals(3, this.transform(Target.class, Transformer.class, "interfaceCall").getMethod("interfaceCall").invoke(null));
    }

    @Test
    public void modifyReceiverWithArguments() throws Exception {
        Class<?> target = this.transform(Target.class, Transformer.class, "withArgs");
        assertEquals(13L, target.getMethod("withArgs", long.class).invoke(null, 3L));
        assertEquals(1, target.getField("evaluations").get(null));
        assertEquals(1, target.getField("modifications").get(null));
    }

    @Test
    public void modifyFieldGetReceiver() throws Exception {
        assertEquals(42L, this.transform(Target.class, Transformer.class, "fieldGet").getMethod("fieldGet").invoke(null));
    }

    @Test
    public void modifyFieldSetReceiver() throws Exception {
        Class<?> target = this.transform(Target.class, Transformer.class, "fieldSet");
        assertEquals(7L, target.getMethod("fieldSet", long.class).invoke(null, 7L));
        assertEquals(2L, ((Person) target.getField("destination").get(null)).value);
    }

    @Test
    public void chainReceivers() throws Exception {
        assertEquals(20L, this.transform(Target.class, Transformer.class, "chainAdd", "chainMultiply").getMethod("virtualCall").invoke(null));
    }

    @Test
    public void modifyMultipleTargets() throws Exception {
        Class<?> target = this.transform(Target.class, Transformer.class, "multiple");
        assertEquals(20L, target.getMethod("multiple").invoke(null));
        assertEquals(2, target.getField("modifications").get(null));
    }

    @Test
    public void modifyInstanceTargetReceiver() throws Exception {
        Class<?> target = this.transform(InstanceTarget.class, InstanceTransformer.class, "call");
        Object instance = target.getConstructor().newInstance();
        assertEquals(10L, target.getMethod("call").invoke(instance));
    }

    @Test
    public void modifyInstanceTargetWithStaticHandler() throws Exception {
        Class<?> target = this.transform(InstanceTarget.class, StaticInstanceTransformer.class, "call");
        Object instance = target.getConstructor().newInstance();
        assertEquals(20L, target.getMethod("call").invoke(instance));
    }

    @Test
    public void modifySuperCallReceiver() throws Exception {
        Class<?> target = this.transform(SuperTarget.class, SuperTransformer.class, "callSuper");
        Object instance = target.getConstructor(long.class).newInstance(1L);
        assertEquals(5L, target.getMethod("callSuper").invoke(instance));
    }

    @Test
    public void captureEnclosingArguments() throws Exception {
        assertEquals(7L, this.transform(Target.class, Transformer.class, "captured").getMethod("captured", long.class).invoke(null, 7L));
    }

    @Test
    public void keepOriginalOperationForLateInjectors() throws Exception {
        this.transformerManager.setFailStrategy(FailStrategy.THROW);
        this.transformerManager.addTransformer(this.transformer(Transformer.class, "virtualCall", "expression"));
        byte[] bytes = this.transformerManager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        assertEquals(11L, TestClassLoader.load(ASMUtils.fromBytes(bytes)).getMethod("virtualCall").invoke(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"constructor", "staticCall", "staticField", "wrongReturn", "wrongStatic", "wrongArguments"})
    public void rejectInvalidInjection(final String kind) throws Exception {
        ClassNode target = this.read(Target.class);
        assertThrows(TransformerException.class, () -> this.handler.transform(this.transformerManager, target, this.transformer(InvalidTransformer.class, kind)));
    }

    @Test
    public void missingTargetsRespectRequire() throws Exception {
        assertDoesNotThrow(() -> this.transform(Target.class, OptionalTransformer.class, "optional"));
        ClassNode target = this.read(Target.class);
        assertThrows(TransformerException.class, () -> this.handler.transform(this.transformerManager, target, this.transformer(OptionalTransformer.class, "required")));
    }

    private Class<?> transform(final Class<?> targetClass, final Class<?> transformerClass, final String... methods) throws Exception {
        ClassNode target = this.read(targetClass);
        this.handler.transform(this.transformerManager, target, this.transformer(transformerClass, methods));
        return TestClassLoader.load(target);
    }

    private ClassNode transformer(final Class<?> transformerClass, final String... methods) throws Exception {
        ClassNode transformer = this.read(transformerClass);
        transformer.methods.removeIf(method -> !Arrays.asList(methods).contains(method.name));
        return transformer;
    }

    private ClassNode read(final Class<?> clazz) throws ClassNotFoundException {
        return ASMUtils.fromBytes(this.classProvider.getClass(clazz.getName()));
    }


    public static class Person {

        public long value;

        public Person(final long value) {
            this.value = value;
        }

        public long getValue() {
            return this.value;
        }

        public long add(final long amount) {
            this.value += amount;
            return this.value;
        }

    }

    public static class Target {

        public static Person first = new Person(1);
        public static Person destination = new Person(2);
        public static Person replacement = new Person(2);
        public static CharSequence text = "abcd";
        public static int evaluations;
        public static int modifications;

        public static long virtualCall() {
            return first.getValue();
        }

        public static int interfaceCall() {
            return text.length();
        }

        public static long withArgs(final long seed) {
            return first.add(evaluate(seed));
        }

        public static long fieldGet() {
            return first.value;
        }

        public static long fieldSet(final long value) {
            destination.value = value;
            return replacement.value;
        }

        public static long multiple() {
            return first.getValue() + first.value;
        }

        public static long captured(final long value) {
            return first.getValue();
        }

        public static long constructor() {
            return new Person(1).getValue();
        }

        public static long staticCall() {
            return System.currentTimeMillis();
        }

        public static long staticField() {
            return Integer.MAX_VALUE;
        }

        private static long evaluate(final long seed) {
            evaluations++;
            return seed * 2;
        }

    }

    public static class InstanceTarget {

        public long call() {
            return Target.first.getValue();
        }

    }

    public static class SuperBase {

        public long number;

        public SuperBase(final long number) {
            this.number = number;
        }

        public long value() {
            return this.number;
        }

    }

    public static class SuperTarget extends SuperBase {

        public SuperTarget(final long number) {
            super(number);
        }

        public long callSuper() {
            return super.value();
        }

    }

    @CTransformer(Target.class)
    private static class Transformer {

        @CModifyReceiver(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public static Person virtualCall(final Person original) {
            return new Person(10);
        }

        @CModifyReceiver(method = "interfaceCall", target = @CTarget(value = "INVOKE", target = "Ljava/lang/CharSequence;length()I"))
        public static CharSequence interfaceCall(final CharSequence original) {
            return "xyz";
        }

        @CModifyReceiver(method = "withArgs", target = @CTarget(value = "INVOKE", target = PERSON + "add(J)J"))
        public static Person withArgs(final Person original, final long amount) {
            Target.modifications++;
            return new Person(original.value + amount);
        }

        @CModifyReceiver(method = "fieldGet", target = @CTarget(value = "FIELD", target = PERSON + "value:J", opcode = Opcodes.GETFIELD))
        public static Person fieldGet(final Person original) {
            return new Person(42);
        }

        @CShadow
        private static Person replacement;

        @CModifyReceiver(method = "fieldSet", target = @CTarget(value = "FIELD", target = PERSON + "value:J", opcode = Opcodes.PUTFIELD))
        public static Person fieldSet(final Person original, final long value) {
            return replacement;
        }

        @CModifyReceiver(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"), order = 100)
        public static Person chainAdd(final Person original) {
            return new Person(original.value + 1);
        }

        @CModifyReceiver(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"), order = 200)
        public static Person chainMultiply(final Person original) {
            return new Person(original.value * 10);
        }

        @CModifyReceiver(method = "multiple", target = {
                @CTarget(value = "INVOKE", target = PERSON + "getValue()J"),
                @CTarget(value = "FIELD", target = PERSON + "value:J", opcode = Opcodes.GETFIELD)})
        public static Person multiple(final Person original) {
            Target.modifications++;
            return new Person(10);
        }

        @CModifyReceiver(method = "captured", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public static Person captured(final Person original, final long value) {
            return new Person(value);
        }

        @CModifyExpressionValue(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public static long expression(final long original) {
            return original + 1;
        }

    }

    @CTransformer(InstanceTarget.class)
    private static class InstanceTransformer {

        @CModifyReceiver(method = "call", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public Person call(final Person original) {
            return new Person(10);
        }

    }

    @CTransformer(InstanceTarget.class)
    private static class StaticInstanceTransformer {

        @CModifyReceiver(method = "call", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public static Person call(final Person original) {
            return new Person(20);
        }

    }

    @CTransformer(SuperTarget.class)
    private static class SuperTransformer {

        @CModifyReceiver(method = "callSuper", target = @CTarget(value = "INVOKE", target = SUPER_BASE + "value()J"))
        public SuperTarget callSuper(final SuperTarget original) {
            return new SuperTarget(5);
        }

    }

    @CTransformer(Target.class)
    private static class InvalidTransformer {

        @CModifyReceiver(method = "constructor", target = @CTarget(value = "INVOKE", target = PERSON + "<init>(J)V"))
        public static Person constructor(final Person original) {
            return original;
        }

        @CModifyReceiver(method = "staticCall", target = @CTarget(value = "INVOKE", target = "Ljava/lang/System;currentTimeMillis()J"))
        public static Object staticCall(final Object original) {
            return original;
        }

        @CModifyReceiver(method = "staticField", target = @CTarget(value = "FIELD", target = "Ljava/lang/Integer;MAX_VALUE:I"))
        public static Object staticField(final Object original) {
            return original;
        }

        @CModifyReceiver(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public static String wrongReturn(final Person original) {
            return "invalid";
        }

        @CModifyReceiver(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public Person wrongStatic(final Person original) {
            return original;
        }

        @CModifyReceiver(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"))
        public static Person wrongArguments(final Person original, final int value) {
            return original;
        }

    }

    @CTransformer(Target.class)
    private static class OptionalTransformer {

        @CModifyReceiver(method = "missing", target = @CTarget(value = "INVOKE", target = PERSON + "getValue()J"), require = 0)
        public static Person optional(final Person original) {
            return original;
        }

        @CModifyReceiver(method = "virtualCall", target = @CTarget(value = "INVOKE", target = PERSON + "missing()V"))
        public static Person required(final Person original) {
            return original;
        }

    }

}
