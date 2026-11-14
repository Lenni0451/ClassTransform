package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.local.CLocalIntRef;
import net.lenni0451.classtransform.utils.local.CLocalLongRef;
import net.lenni0451.classtransform.utils.local.CLocalRef;
import net.lenni0451.classtransform.utils.log.ILogger;
import net.lenni0451.classtransform.utils.log.Logger;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CLocalVariableCoprocessorTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/coprocessor/impl/CLocalVariableCoprocessorTest$Target;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void ordinaryParameterDoesNotWriteBack() throws Exception {
        Class<?> target = this.transform(Transformer.class, "ordinary");
        assertEquals(2, target.getMethod("ordinary", int.class).invoke(null, 1));
        assertEquals(2, target.getField("seen").get(null));
    }

    @Test
    public void modifiableParameterWritesBack() throws Exception {
        Class<?> target = this.transform(Transformer.class, "modifiable");
        assertEquals(99, target.getMethod("modifiable", int.class).invoke(null, 1));
        assertEquals(99, target.getField("seen").get(null));
    }

    @Test
    public void referenceParameterReadsAndWrites() throws Exception {
        Class<?> target = this.transform(Transformer.class, "reference");
        assertEquals(12, target.getMethod("reference", int.class).invoke(null, 1));
        assertEquals(2, target.getField("seen").get(null));
    }

    @Test
    public void multipleReferencesShareTheVariable() throws Exception {
        assertEquals(3, this.transform(Transformer.class, "multiple").getMethod("multiple").invoke(null));
    }

    @Test
    public void implicitSelectionByType() throws Exception {
        assertEquals(8, this.transform(Transformer.class, "implicit").getMethod("implicit").invoke(null));
    }

    @Test
    public void argsOnlySelectsParameters() throws Exception {
        assertEquals(905, this.transform(Transformer.class, "argsOnly").getMethod("argsOnly", int.class).invoke(null, 2));
    }

    @Test
    public void multipleNamesAreTriedInOrder() throws Exception {
        assertEquals(4, this.transform(Transformer.class, "names").getMethod("names").invoke(null));
    }

    @Test
    public void ordinalSelectsByType() throws Exception {
        assertEquals(13, this.transform(Transformer.class, "ordinal").getMethod("ordinal").invoke(null));
    }

    @Test
    public void scopesAndSlotReuseAreRespected() throws Exception {
        Class<?> target = this.transform(Transformer.class, "scopedInt", "scopedString");
        assertEquals(11, target.getMethod("scoped", boolean.class).invoke(null, true));
        assertEquals(2, target.getMethod("scoped", boolean.class).invoke(null, false));
    }

    @Test
    public void wideValuesAreSupported() throws Exception {
        assertEquals(5L, this.transform(Transformer.class, "wide").getMethod("wide").invoke(null));
    }

    @Test
    public void objectReferencesAreSupported() throws Exception {
        assertEquals(3, this.transform(Transformer.class, "object").getMethod("object").invoke(null));
    }

    @Test
    public void referencesCanBePassedToHelperMethods() throws Exception {
        assertEquals(2, this.transform(Transformer.class, "helper").getMethod("helper").invoke(null));
    }

    @Test
    public void explicitIndexWorksWithoutLocalVariableTable() throws Exception {
        assertEquals(6, this.transformWithoutLvt(Transformer.class, "noLvtIndex", "noLvtIndex").getMethod("noLvtIndex").invoke(null));
    }

    @Test
    public void implicitSelectionWorksWithoutLocalVariableTable() throws Exception {
        assertEquals(8, this.transformWithoutLvt(Transformer.class, "noLvtImplicit", "noLvtImplicit").getMethod("noLvtImplicit").invoke(null));
    }

    @Test
    public void printAbortsAndReportsLocals() throws Exception {
        ILogger oldLogger = Logger.LOGGER;
        List<String> messages = new ArrayList<>();
        Logger.LOGGER = new ILogger() {
            @Override
            public void info(final String message, final Object... args) {
                String formatted = message;
                for (Object arg : args) formatted = formatted.replaceFirst("\\{}", java.util.regex.Matcher.quoteReplacement(String.valueOf(arg)));
                messages.add(formatted);
            }

            @Override
            public void warn(final String message, final Object... args) {
            }

            @Override
            public void error(final String message, final Object... args) {
            }
        };
        try {
            TransformerManager manager = this.manager(PrintTransformer.class, "print");
            assertThrows(Throwable.class, () -> manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName())));
            assertTrue(messages.stream().anyMatch(message -> message.contains("Local variables at")), messages.toString());
            assertTrue(messages.stream().anyMatch(message -> message.contains("name=local")), messages.toString());
        } finally {
            Logger.LOGGER = oldLogger;
        }
    }

    private Class<?> transform(final Class<?> transformerClass, final String... methods) throws Exception {
        TransformerManager manager = this.manager(transformerClass, methods);
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }

    private Class<?> transformWithoutLvt(final Class<?> transformerClass, final String targetMethod, final String... methods) throws Exception {
        TransformerManager manager = this.manager(transformerClass, methods);
        ClassNode target = ASMUtils.fromBytes(this.classProvider.getClass(Target.class.getName()));
        MethodNode method = ASMUtils.getMethod(target, targetMethod, "()I");
        assertNotNull(method);
        method.localVariables = null;
        byte[] targetBytes = ASMUtils.toBytes(target, manager);
        byte[] bytes = manager.transform(Target.class.getName(), targetBytes);
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }

    private TransformerManager manager(final Class<?> transformerClass, final String... methods) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        ClassNode transformer = ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName()));
        transformer.methods.removeIf(method -> !Arrays.asList(methods).contains(method.name) && !method.name.startsWith("helper"));
        manager.addTransformer(transformer);
        return manager;
    }


    public static class Target {

        public static int seen;

        public static void consume() {
        }

        public static int ordinary(final int seed) {
            int local = seed + 1;
            consume();
            return local;
        }

        public static int modifiable(final int seed) {
            int local = seed + 1;
            consume();
            return local;
        }

        public static int reference(final int seed) {
            int local = seed + 1;
            consume();
            return local;
        }

        public static int multiple() {
            int local = 1;
            consume();
            return local;
        }

        public static int implicit() {
            int local = 7;
            consume();
            return local;
        }

        public static int argsOnly(final int first) {
            int local = 5;
            consume();
            return first * 100 + local;
        }

        public static int names() {
            int actual = 3;
            consume();
            return actual;
        }

        public static int ordinal() {
            int first = 1;
            int second = 2;
            consume();
            return first * 10 + second;
        }

        public static int scoped(final boolean first) {
            if (first) {
                int value = 10;
                consume();
                return value;
            } else {
                String value = "text";
                consume();
                return value.length();
            }
        }

        public static long wide() {
            long local = 4L;
            consume();
            return local;
        }

        public static int object() {
            String text = "hello";
            consume();
            return text.length();
        }

        public static int helper() {
            int local = 1;
            consume();
            return local;
        }

        public static int noLvtIndex() {
            int local = 5;
            consume();
            return local;
        }

        public static int noLvtImplicit() {
            int local = 7;
            consume();
            return local;
        }

    }

    @CTransformer(Target.class)
    private static class Transformer {

        @CShadow
        private static int seen;

        @CInject(method = "ordinary", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void ordinary(@CLocalVariable(name = "local") final int value) {
            seen = value;
        }

        @CInject(method = "modifiable", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void modifiable(@CLocalVariable(name = "local", modifiable = true) int value) {
            value = 99;
            seen = value;
        }

        @CInject(method = "reference", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void reference(@CLocalVariable(name = "local") final CLocalIntRef ref) {
            seen = ref.get();
            ref.set(ref.get() + 10);
        }

        @CInject(method = "multiple", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void multiple(@CLocalVariable(name = "local") final CLocalIntRef first, @CLocalVariable(name = "local") final CLocalIntRef second) {
            first.set(first.get() + 1);
            second.set(second.get() + 1);
        }

        @CInject(method = "implicit", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void implicit(@CLocalVariable(implicit = true) final CLocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @CInject(method = "argsOnly", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void argsOnly(@CLocalVariable(implicit = true, argsOnly = true) final CLocalIntRef ref) {
            ref.set(9);
        }

        @CInject(method = "names", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void names(@CLocalVariable(names = {"missing", "actual"}) final CLocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @CInject(method = "ordinal", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void ordinal(@CLocalVariable(ordinal = 1) final CLocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @CInject(method = "scoped", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void scopedInt(@CLocalVariable(implicit = true) final CLocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @CInject(method = "scoped", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void scopedString(@CLocalVariable(implicit = true) final CLocalRef<String> ref) {
            ref.set("hi");
        }

        @CInject(method = "wide", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void wide(@CLocalVariable(name = "local") final CLocalLongRef ref) {
            ref.set(ref.get() + 1);
        }

        @CInject(method = "object", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void object(@CLocalVariable(name = "text") final CLocalRef<String> ref) {
            ref.set("hey");
        }

        @CInject(method = "helper", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void helperHandler(@CLocalVariable(name = "local") final CLocalIntRef ref) {
            helper(ref);
        }

        private static void helper(final CLocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @CInject(method = "noLvtIndex", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void noLvtIndex(@CLocalVariable(index = 0) final CLocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

        @CInject(method = "noLvtImplicit", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void noLvtImplicit(@CLocalVariable(implicit = true) final CLocalIntRef ref) {
            ref.set(ref.get() + 1);
        }

    }

    @CTransformer(Target.class)
    private static class PrintTransformer {

        @CInject(method = "ordinary", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void print(@CLocalVariable(name = "local", print = true) final int value) {
        }

    }

}
