package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.expression.CDefinition;
import net.lenni0451.classtransform.annotations.expression.CExpression;
import net.lenni0451.classtransform.annotations.expression.CLocalDefinition;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.annotations.injection.CModifyReceiver;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.annotations.injection.CWrapOperation;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExpressionAnnotationHandlerTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/impl/ExpressionAnnotationHandlerTest$Target;";
    private static final String HOLDER = "Lnet/lenni0451/classtransform/transformer/impl/ExpressionAnnotationHandlerTest$Holder;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void modifyMethodCall() throws Exception {
        Class<?> target = this.transform(ModifyCall.class);
        assertEquals(107, target.getMethod("invoke").invoke(null));
    }

    @Test
    public void modifyArrayAccess() throws Exception {
        Class<?> target = this.transform(ModifyArray.class);
        assertEquals(11, target.getMethod("array", int[].class).invoke(null, new int[]{5, 1, 7}));
    }

    @Test
    public void modifyComparison() throws Exception {
        Class<?> target = this.transform(ModifyComparison.class);
        assertEquals(20, target.getMethod("compare", int.class, int.class).invoke(null, 1, 2));
        assertEquals(10, target.getMethod("compare", int.class, int.class).invoke(null, 3, 2));
    }

    @Test
    public void modifyCast() throws Exception {
        Class<?> target = this.transform(ModifyCast.class);
        assertEquals("TEST!", target.getMethod("cast", Object.class).invoke(null, "test"));
    }

    @Test
    public void redirectExpression() throws Exception {
        Class<?> target = this.transform(RedirectCall.class);
        assertEquals(9, target.getMethod("invoke").invoke(null));
    }

    @Test
    public void injectAtExpression() throws Exception {
        Class<?> target = this.transform(InjectCall.class);
        assertEquals(7, target.getMethod("invoke").invoke(null));
        assertEquals(1, target.getField("seen").get(null));
    }

    @Test
    public void modifyExpressionReturnValue() throws Exception {
        Class<?> target = this.transform(ModifyReturn.class);
        assertEquals("changed!", target.getMethod("cast", Object.class).invoke(null, "test"));
    }

    @Test
    public void wrapComparison() throws Exception {
        Class<?> target = this.transform(WrapComparison.class);
        assertEquals(20, target.getMethod("compare", int.class, int.class).invoke(null, 1, 2));
        assertEquals(10, target.getMethod("compare", int.class, int.class).invoke(null, 3, 2));
    }

    @Test
    public void localDefinition() throws Exception {
        Class<?> target = this.transform(LocalDefinitionTest.class);
        assertEquals(14, target.getMethod("local", int.class).invoke(null, 1));
    }

    @Test
    public void repeatedExpressionsAndIds() throws Exception {
        Class<?> target = this.transform(RepeatedExpressions.class);
        assertEquals(107, target.getMethod("invoke").invoke(null));
        assertEquals(101, target.getMethod("array", int[].class).invoke(null, new int[]{5, 1, 7}));
    }

    @Test
    public void instanceofExpression() throws Exception {
        Class<?> target = this.transform(InstanceofExpression.class);
        assertEquals(false, target.getMethod("check", Object.class).invoke(null, "test"));
        assertEquals(true, target.getMethod("check", Object.class).invoke(null, 1));
    }

    @Test
    public void binaryExpression() throws Exception {
        Class<?> target = this.transform(BinaryExpressionTest.class);
        assertEquals(15, target.getMethod("add", int.class, int.class).invoke(null, 2, 3));
    }

    @Test
    public void intLikeBinaryExpression() throws Exception {
        Class<?> target = this.transform(IntLikeExpressionTest.class);
        assertEquals(0, target.getMethod("literal").invoke(null));
    }

    @Test
    public void modifyUnusedInstantiation() throws Exception {
        Class<?> target = this.transform(UnusedInstantiationTest.class);
        assertDoesNotThrow(() -> target.getMethod("unusedNew").invoke(null));
        assertEquals(1, target.getField("creations").get(null));
    }

    @Test
    public void freeMethodReference() throws Exception {
        Class<?> target = this.transform(FreeMethodReference.class);
        assertEquals(1, target.getMethod("staticReference").invoke(null));
        assertEquals(1, target.getField("seen").get(null));
    }

    @Test
    public void boundMethodReference() throws Exception {
        Class<?> target = this.transform(BoundMethodReference.class);
        Object instance = target.getConstructor().newInstance();
        assertEquals(1, target.getMethod("boundReference").invoke(instance));
        assertEquals(1, target.getField("seen").get(null));
    }

    @Test
    public void constructorReference() throws Exception {
        Class<?> target = this.transform(ConstructorReference.class);
        assertEquals(5, ((Holder) target.getMethod("constructorReference", int.class).invoke(null, 5)).value);
        assertEquals(1, target.getField("seen").get(null));
    }

    @Test
    public void wrapArrayAccess() throws Exception {
        Class<?> target = this.transform(WrapArray.class);
        assertEquals(5, target.getMethod("array", int[].class).invoke(null, new int[]{5, 1, 7}));
    }

    @Test
    public void newArrayExpression() throws Exception {
        Class<?> target = this.transform(NewArrayExpressionTest.class);
        int[] array = (int[]) target.getMethod("createArray", int.class).invoke(null, 2);
        assertEquals(3, array.length);
    }

    @Test
    public void modifyInstantiation() throws Exception {
        Class<?> target = this.transform(InstantiationExpression.class);
        assertEquals(11, ((Holder) target.getMethod("create", int.class).invoke(null, 1)).value);
    }

    @Test
    public void wrapInstantiation() throws Exception {
        Class<?> target = this.transform(WrapInstantiation.class);
        assertEquals(2, ((Holder) target.getMethod("create", int.class).invoke(null, 1)).value);
    }

    @Test
    public void expressionSlice() throws Exception {
        Class<?> target = this.transform(SliceExpression.class);
        assertEquals(16, target.getMethod("slice", int.class).invoke(null, 1));
    }

    @Test
    public void fieldDefinition() throws Exception {
        Class<?> target = this.transform(FieldExpression.class);
        assertEquals(15, target.getMethod("field").invoke(null));
    }

    @Test
    public void modifyReceiverExpression() throws Exception {
        Class<?> target = this.transform(ReceiverExpression.class);
        assertEquals(7, target.getMethod("receiver").invoke(null));
    }

    @Test
    public void wrapConditionExpression() throws Exception {
        Class<?> target = this.transform(ConditionExpression.class);
        target.getMethod("conditional", int.class).invoke(null, 1);
        assertEquals(1, target.getField("conditionValue").get(null));
        target.getField("conditionValue").set(null, 0);
        target.getMethod("conditional", int.class).invoke(null, -1);
        assertEquals(0, target.getField("conditionValue").get(null));
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

        public int value;

        public Holder(final int value) {
            this.value = value;
        }

        public int get() {
            return this.value;
        }

    }

    public static class Target {

        public static int seen;
        public static int conditionValue;
        public static int fieldValue = 5;
        public static int referenceCount;
        public static Holder holder = new Holder(3);

        public static void tick() {
            referenceCount++;
        }

        public void instanceTick() {
            referenceCount++;
        }

        public static int staticReference() {
            Runnable runnable = Target::tick;
            runnable.run();
            return referenceCount;
        }

        public int boundReference() {
            Runnable runnable = this::instanceTick;
            runnable.run();
            return referenceCount;
        }

        public static Holder constructorReference(final int value) {
            IntFunction<Holder> function = Holder::new;
            return function.apply(value);
        }

        public static Holder create(final int value) {
            return new Holder(value);
        }

        public static int compute(final int value) {
            return value * 2;
        }

        public static int invoke() {
            return compute(3) + 1;
        }

        public static int array(final int[] values) {
            return values[1];
        }

        public static int compare(final int left, final int right) {
            return left < right ? 10 : 20;
        }

        public static String cast(final Object value) {
            return ((String) value) + "!";
        }

        public static int local(final int value) {
            int local = value + 1;
            return local + 2;
        }

        public static void consume(final int value) {
            conditionValue = value;
        }

        public static void conditional(final int value) {
            consume(value);
        }

        public static int field() {
            return fieldValue;
        }

        public static int receiver() {
            return holder.get();
        }

        public static boolean check(final Object value) {
            return value instanceof String;
        }

        public static int add(final int left, final int right) {
            return left + right;
        }

        public static int creations;

        public static int literal() {
            return 5;
        }

        public static Holder unusedNew() {
            new Holder(4);
            creations++;
            return null;
        }

        public static int[] createArray(final int size) {
            return new int[size];
        }

        public static int slice(final int value) {
            int first = compute(value);
            int second = compute(value + 1);
            return first + second;
        }

    }

    @CTransformer(Target.class)
    private static class ModifyCall {

        @CDefinition(id = "compute", method = "compute(I)I")
        @CExpression("compute(?)")
        @CModifyExpressionValue(method = "invoke", target = @CTarget(value = "EXPRESSION"))
        private static int invoke(final int original) {
            return original + 100;
        }

    }

    @CTransformer(Target.class)
    private static class ModifyArray {

        @CExpression("?[?]")
        @CModifyExpressionValue(method = "array", target = @CTarget(value = "EXPRESSION"))
        private static int array(final int original) {
            return original + 10;
        }

    }

    @CTransformer(Target.class)
    private static class ModifyComparison {

        @CExpression("? < ?")
        @CModifyExpressionValue(method = "compare", target = @CTarget(value = "EXPRESSION"))
        private static boolean compare(final boolean original) {
            return !original;
        }

    }

    @CTransformer(Target.class)
    private static class ModifyCast {

        @CDefinition(id = "String", type = String.class)
        @CExpression("(String) ?")
        @CModifyExpressionValue(method = "cast", target = @CTarget(value = "EXPRESSION"))
        private static String cast(final String original) {
            return original.toUpperCase();
        }

    }

    @CTransformer(Target.class)
    private static class RedirectCall {

        @CDefinition(id = "compute", method = "compute(I)I")
        @CExpression("compute(?)")
        @CRedirect(method = "invoke", target = @CTarget(value = "EXPRESSION"))
        private static int invoke(final int value) {
            return value + 5;
        }

    }

    @CTransformer(Target.class)
    private static class InjectCall {

        @CShadow
        private static int seen;

        @CDefinition(id = "compute", method = "compute(I)I")
        @CExpression("compute(?)")
        @CInject(method = "invoke", target = @CTarget(value = "EXPRESSION", shift = CTarget.Shift.BEFORE))
        private static void inject() {
            seen = 1;
        }

    }

    @CTransformer(Target.class)
    private static class FreeMethodReference {

        @CShadow
        private static int seen;

        @CDefinition(id = "tick", method = "tick()V")
        @CExpression("::tick")
        @CInject(method = "staticReference", target = @CTarget(value = "EXPRESSION", shift = CTarget.Shift.BEFORE))
        private static void inject() {
            seen = 1;
        }

    }

    @CTransformer(Target.class)
    private static class BoundMethodReference {

        @CShadow
        private static int seen;

        @CDefinition(id = "instanceTick", method = "instanceTick()V")
        @CExpression("this::instanceTick")
        @CInject(method = "boundReference", target = @CTarget(value = "EXPRESSION", shift = CTarget.Shift.BEFORE))
        private void inject() {
            seen = 1;
        }

    }

    @CTransformer(Target.class)
    private static class ConstructorReference {

        @CShadow
        private static int seen;

        @CDefinition(id = "Holder", type = Holder.class)
        @CExpression("Holder::new")
        @CInject(method = "constructorReference", target = @CTarget(value = "EXPRESSION", shift = CTarget.Shift.BEFORE))
        private static void inject() {
            seen = 1;
        }

    }

    @CTransformer(Target.class)
    private static class ModifyReturn {

        @CExpression("return ?")
        @CModifyReturnValue(method = "cast", target = @CTarget(value = "EXPRESSION"))
        private static String cast(final String original) {
            return "changed!";
        }

    }

    @CTransformer(Target.class)
    private static class LocalDefinitionTest {

        @CDefinition(id = "local", local = @CLocalDefinition(type = int.class, name = "local"))
        @CExpression("local + ?")
        @CModifyExpressionValue(method = "local", target = @CTarget(value = "EXPRESSION"))
        private static int local(final int original) {
            return original + 10;
        }

    }

    @CTransformer(Target.class)
    private static class RepeatedExpressions {

        @CDefinition(id = "compute", method = "compute(I)I")
        @CExpression(value = "compute(?)", id = "call")
        @CExpression(value = "?[?]", id = "array")
        @CModifyExpressionValue(
                method = {"invoke", "array"},
                targets = {
                        @CTarget(value = "EXPRESSION", target = "call", optional = true),
                        @CTarget(value = "EXPRESSION", target = "array", optional = true)
                }
        )
        private static int modify(final int original) {
            return original + 100;
        }

    }

    @CTransformer(Target.class)
    private static class InstanceofExpression {

        @CDefinition(id = "String", type = String.class)
        @CExpression("? instanceof String")
        @CModifyExpressionValue(method = "check", target = @CTarget(value = "EXPRESSION"))
        private static boolean check(final boolean original) {
            return !original;
        }

    }

    @CTransformer(Target.class)
    private static class BinaryExpressionTest {

        @CExpression("? + ?")
        @CModifyExpressionValue(method = "add", target = @CTarget(value = "EXPRESSION"))
        private static int add(final int original) {
            return original + 10;
        }

    }

    @CTransformer(Target.class)
    private static class IntLikeExpressionTest {

        @CExpression("5")
        @CModifyExpressionValue(method = "literal", target = @CTarget(value = "EXPRESSION"))
        private static boolean literal(final boolean original) {
            return false;
        }

    }

    @CTransformer(Target.class)
    private static class UnusedInstantiationTest {

        @CDefinition(id = "Holder", type = Holder.class)
        @CExpression("new Holder(?)")
        @CModifyExpressionValue(method = "unusedNew", target = @CTarget(value = "EXPRESSION"))
        private static Holder modify(final Holder original) {
            return original;
        }

    }

    @CTransformer(Target.class)
    private static class NewArrayExpressionTest {

        @CExpression("new int[?]")
        @CModifyExpressionValue(method = "createArray", target = @CTarget(value = "EXPRESSION"))
        private static int[] createArray(final int[] original) {
            return new int[original.length + 1];
        }

    }

    @CTransformer(Target.class)
    private static class WrapArray {

        @CExpression("?[?]")
        @CWrapOperation(method = "array", target = @CTarget(value = "EXPRESSION"))
        private static int array(final int[] values, final int index, final COperation<Integer> original) {
            return original.call(values, index - 1);
        }

    }

    @CTransformer(Target.class)
    private static class InstantiationExpression {

        @CDefinition(id = "Holder", type = Holder.class)
        @CExpression("new Holder(?)")
        @CModifyExpressionValue(method = "create", target = @CTarget(value = "EXPRESSION"))
        private static Holder create(final Holder original) {
            return new Holder(original.value + 10);
        }

    }

    @CTransformer(Target.class)
    private static class WrapInstantiation {

        @CDefinition(id = "Holder", type = Holder.class)
        @CExpression("new Holder(?)")
        @CWrapOperation(method = "create", target = @CTarget(value = "EXPRESSION"))
        private static Holder create(final int value, final COperation<Holder> original) {
            return original.call(value + 1);
        }

    }

    @CTransformer(Target.class)
    private static class SliceExpression {

        @CDefinition(id = "compute", method = "compute(I)I")
        @CExpression(value = "compute(?)", id = "call")
        @CModifyExpressionValue(
                method = "slice",
                slice = @CSlice(
                        id = "first",
                        from = @CTarget("HEAD"),
                        to = @CTarget(value = "EXPRESSION", target = "call", ordinal = 0)
                ),
                target = @CTarget(value = "EXPRESSION", target = "call", slice = "first")
        )
        private static int slice(final int original) {
            return original + 10;
        }

    }

    @CTransformer(Target.class)
    private static class FieldExpression {

        @CDefinition(id = "fieldValue", field = "fieldValue:I")
        @CExpression("fieldValue")
        @CModifyExpressionValue(method = "field", target = @CTarget(value = "EXPRESSION"))
        private static int field(final int original) {
            return original + 10;
        }

    }

    @CTransformer(Target.class)
    private static class ReceiverExpression {

        @CDefinition(id = "get", method = HOLDER + "get()I")
        @CExpression("?.get()")
        @CModifyReceiver(method = "receiver", target = @CTarget(value = "EXPRESSION"))
        private static Holder receiver(final Holder original) {
            return new Holder(7);
        }

    }

    @CTransformer(Target.class)
    private static class ConditionExpression {

        @CDefinition(id = "consume", method = "consume(I)V")
        @CExpression("consume(?)")
        @CWrapCondition(method = "conditional", target = @CTarget(value = "EXPRESSION"))
        private static boolean condition(final int value) {
            return value > 0;
        }

    }

    @CTransformer(Target.class)
    private static class WrapComparison {

        @CExpression("? < ?")
        @CWrapOperation(method = "compare", target = @CTarget(value = "EXPRESSION"))
        private static boolean compare(final int left, final int right, final COperation<Boolean> original) {
            return !original.call(left, right);
        }

    }

}
