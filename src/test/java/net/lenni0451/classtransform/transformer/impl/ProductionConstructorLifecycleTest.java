package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.CArgs;
import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.annotations.injection.CModifyArg;
import net.lenni0451.classtransform.annotations.injection.CModifyArgs;
import net.lenni0451.classtransform.annotations.injection.CModifyVariable;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionConstructorLifecycleTest {

    private static final String CHILD = "Lnet/lenni0451/classtransform/transformer/impl/ProductionConstructorLifecycleTest$Child;";
    private static final String PARENT = "Lnet/lenni0451/classtransform/transformer/impl/ProductionConstructorLifecycleTest$Parent;";
    private static final String OVERLOADED = "Lnet/lenni0451/classtransform/transformer/impl/ProductionConstructorLifecycleTest$Overloaded;";
    private static final String AFTER_INIT = "Lnet/lenni0451/classtransform/transformer/impl/ProductionConstructorLifecycleTest$AfterInit;";
    private static final String AFTER_INIT_ARGS = "Lnet/lenni0451/classtransform/transformer/impl/ProductionConstructorLifecycleTest$AfterInitArgs;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void constructorChainKeepsStateAndInjectsAfterSuper() throws Exception {
        Class<?> target = this.transform(Child.class, ConstructorTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(3);
        assertEquals(11, target.getMethod("total").invoke(instance));
        assertEquals(1, target.getField("injections").get(null));
    }

    @Test
    public void thisCallArgumentsCanBeModified() throws Exception {
        Class<?> target = this.transform(Overloaded.class, ThisCallTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(2);
        assertEquals(13, target.getField("value").get(instance));
    }

    @Test
    public void thisCallArgumentCanBeModifiedWithStaticHandler() throws Exception {
        Class<?> target = this.transform(Overloaded.class, ThisCallArgTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(2);
        assertEquals(13, target.getField("value").get(instance));
    }

    @Test
    public void nonStaticHandlerBeforeThisCallIsRejected() {
        assertThrows(TransformerException.class, () -> this.transform(Overloaded.class, NonStaticThisCallArgTransformer.class));
    }

    @Test
    public void staticHandlerCanModifySuperCallArguments() throws Exception {
        Class<?> target = this.transform(SuperArgChild.class, SuperArgTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(2);
        assertEquals(15, target.getMethod("total").invoke(instance));
    }

    @Test
    public void nonStaticHandlerCanModifyCallAfterSuper() throws Exception {
        Class<?> target = this.transform(AfterInit.class, AfterInitTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(1);
        assertEquals(12, target.getField("value").get(instance));
    }

    @Test
    public void constructorLocalVariableCanBeModified() throws Exception {
        Class<?> target = this.transform(LocalCtor.class, LocalCtorTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(1);
        assertEquals(12, target.getField("value").get(instance));
    }

    @Test
    public void nestedNewBeforeSuperCallIsHandled() throws Exception {
        Class<?> target = this.transform(NestedNewChild.class, NestedNewTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(2);
        assertEquals(8, target.getMethod("total").invoke(instance));
    }

    @Test
    public void nonStaticModifyArgsHandlerCanRunAfterSuper() throws Exception {
        Class<?> target = this.transform(AfterInitArgs.class, AfterInitArgsTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(1);
        assertEquals(12, target.getField("value").get(instance));
    }

    @Test
    public void injectAtConstructorReturn() throws Exception {
        Class<?> target = this.transform(ConstructorReturnTarget.class, ConstructorReturnTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(5);
        assertEquals(5, target.getField("value").get(instance));
        assertEquals(1, target.getField("calls").get(null));
    }

    @Test
    public void injectAtConstructorTail() throws Exception {
        Class<?> target = this.transform(ConstructorReturnTarget.class, ConstructorTailTransformer.class);
        Object instance = target.getConstructor(int.class).newInstance(5);
        assertEquals(5, target.getField("value").get(instance));
        assertEquals(1, target.getField("calls").get(null));
    }

    private Class<?> transform(final Class<?> targetClass, final Class<?> transformerClass) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        byte[] bytes = manager.transform(targetClass.getName(), this.classProvider.getClass(targetClass.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class Parent {

        public int value;

        public Parent(final int value) {
            this.value = value;
        }

    }

    public static class Child extends Parent {

        public static int injections;

        public int own;

        public Child(final int value) {
            super(value + 1);
            this.own = value;
        }

        public int total() {
            return this.value + this.own;
        }

    }

    @CTransformer(Child.class)
    public static class ConstructorTransformer {

        @CInject(method = "<init>", target = @CTarget("CTOR_HEAD"))
        public void countConstructor() {
            Child.injections++;
        }

        @CModifyArgs(method = "<init>", target = @CTarget(value = "INVOKE", target = PARENT + "<init>(I)V"))
        public static void changeSuperArgument(final CArgs args) {
            args.setInt(0, args.getInt(0) * 2);
        }

    }


    public static class Overloaded {

        public int value;

        public Overloaded(final int value) {
            this(value, 1);
        }

        public Overloaded(final int value, final int extra) {
            this.value = value + extra;
        }

    }

    @CTransformer(Overloaded.class)
    public static class ThisCallTransformer {

        @CModifyArgs(method = "<init>(I)V", target = @CTarget(value = "INVOKE", target = OVERLOADED + "<init>(II)V"))
        public static void changeThisArgument(final CArgs args) {
            args.setInt(0, args.getInt(0) + 10);
        }

    }

    @CTransformer(Overloaded.class)
    public static class ThisCallArgTransformer {

        @CModifyArg(method = "<init>(I)V", target = @CTarget(value = "INVOKE", target = OVERLOADED + "<init>(II)V"), index = 0)
        public static int changeThisArgument(final int value) {
            return value + 10;
        }

    }

    @CTransformer(Overloaded.class)
    public static class NonStaticThisCallArgTransformer {

        @CModifyArg(method = "<init>(I)V", target = @CTarget(value = "INVOKE", target = OVERLOADED + "<init>(II)V"), index = 0)
        public int changeThisArgument(final int value) {
            return value + 10;
        }

    }


    public static class SuperArgChild extends Parent {

        public int own;

        public SuperArgChild(final int value) {
            super(value + 1);
            this.own = value;
        }

        public int total() {
            return this.value + this.own;
        }

    }

    @CTransformer(SuperArgChild.class)
    public static class SuperArgTransformer {

        @CModifyArg(method = "<init>", target = @CTarget(value = "INVOKE", target = PARENT + "<init>(I)V"), index = 0)
        public static int changeSuperArgument(final int value) {
            return value + 10;
        }

    }


    public static class AfterInit {

        public int value;

        public AfterInit(final int value) {
            this.value = this.adjust(value);
        }

        public int adjust(final int value) {
            return value + 1;
        }

    }

    @CTransformer(AfterInit.class)
    public static class AfterInitTransformer {

        @CModifyArg(method = "<init>", target = @CTarget(value = "INVOKE", target = AFTER_INIT + "adjust(I)I"), index = 0)
        public int adjust(final int value) {
            return value + 10;
        }

    }


    public static class LocalCtor {

        public int value;

        public LocalCtor(final int input) {
            int local = input + 1;
            this.value = local;
        }

    }

    @CTransformer(LocalCtor.class)
    public static class LocalCtorTransformer {

        @CModifyVariable(method = "<init>", target = @CTarget("STORE"), name = "local")
        public int local(final int value) {
            return value + 10;
        }

    }


    public static class NestedNewChild extends Parent {

        public int own;

        public NestedNewChild(final int value) {
            super(new Helper(value).value);
            this.own = value;
        }

        public int total() {
            return this.value + this.own;
        }

    }

    public static class Helper {

        public final int value;

        public Helper(final int value) {
            this.value = value + 1;
        }

    }

    @CTransformer(NestedNewChild.class)
    public static class NestedNewTransformer {

        @CModifyArgs(method = "<init>", target = @CTarget(value = "INVOKE", target = PARENT + "<init>(I)V"))
        public static void multiplySuperArgument(final CArgs args) {
            args.setInt(0, args.getInt(0) * 2);
        }

    }


    public static class AfterInitArgs {

        public int value;

        public AfterInitArgs(final int value) {
            this.value = this.adjust(value, 1);
        }

        public int adjust(final int value, final int extra) {
            return value + extra;
        }

    }

    @CTransformer(AfterInitArgs.class)
    public static class AfterInitArgsTransformer {

        @CModifyArgs(method = "<init>", target = @CTarget(value = "INVOKE", target = AFTER_INIT_ARGS + "adjust(II)I"))
        public void adjust(final CArgs args) {
            args.setInt(0, args.getInt(0) + 10);
        }

    }


    public static class ConstructorReturnTarget {

        public static int calls;
        public int value;

        public ConstructorReturnTarget(final int value) {
            this.value = value;
        }

    }

    @CTransformer(ConstructorReturnTarget.class)
    public static class ConstructorReturnTransformer {

        @CInject(method = "<init>", target = @CTarget("RETURN"), cancellable = true)
        public void returnInject(final int value, final InjectionCallback callback) {
            ConstructorReturnTarget.calls++;
            callback.cancel();
        }

    }

    @CTransformer(ConstructorReturnTarget.class)
    public static class ConstructorTailTransformer {

        @CInject(method = "<init>", target = @CTarget("TAIL"))
        public void tailInject(final int value, final InjectionCallback callback) {
            ConstructorReturnTarget.calls++;
        }

    }

}
