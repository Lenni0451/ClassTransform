package net.lenni0451.classtransform.transformer.coprocessor.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CLocalVariable;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.local.CLocalIntRef;
import net.lenni0451.classtransform.utils.local.CLocalLongRef;
import net.lenni0451.classtransform.utils.local.CLocalRef;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class CSharedCoprocessorTest {

    private static final String TARGET = "Lnet/lenni0451/classtransform/transformer/coprocessor/impl/CSharedCoprocessorTest$Target;";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void referenceIsSharedAndIdentical() throws Exception {
        Class<?> target = this.transform(RefTransformer.class);
        assertEquals(4, target.getMethod("refShare", int.class).invoke(null, 3));
        assertTrue((boolean) target.getField("same").get(null));
    }

    @Test
    public void defaultValuesAreAvailable() throws Exception {
        Class<?> target = this.transform(DefaultTransformer.class);
        assertEquals(0, target.getMethod("defaultValue").invoke(null));
        assertNull(target.getMethod("defaultObject").invoke(null));
    }

    @Test
    public void valueModeIsPreserved() throws Exception {
        Class<?> target = this.transform(ValueTransformer.class);
        assertEquals(7, target.getMethod("valueShare").invoke(null));
        assertEquals(0, target.getMethod("valueDefault").invoke(null));
    }

    @Test
    public void namespaceSharesAcrossTransformers() throws Exception {
        Class<?> target = this.transform(NamespaceWriter.class, NamespaceReader.class);
        assertEquals(13, target.getMethod("namespaceShare", int.class).invoke(null, 3));
        assertTrue((boolean) target.getField("namespaceSame").get(null));
    }

    @Test
    public void globalSharesAcrossTransformers() throws Exception {
        assertEquals(13, this.transform(GlobalWriter.class, GlobalReader.class).getMethod("globalShare", int.class).invoke(null, 3));
    }

    @Test
    public void defaultNamespaceIsIsolatedPerTransformer() throws Exception {
        assertEquals(0, this.transform(IsolatedWriter.class, IsolatedReader.class).getMethod("isolatedShare", int.class).invoke(null, 3));
    }

    @Test
    public void recursiveCallsAreIsolated() throws Exception {
        Class<?> target = this.transform(RecursiveTransformer.class);
        assertEquals(3, target.getMethod("recursive", int.class).invoke(null, 3));
        assertTrue((boolean) target.getField("ok").get(null));
    }

    @Test
    public void concurrentCallsAreIsolated() throws Exception {
        Class<?> target = this.transform(ConcurrentTransformer.class);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                int value = i + 1;
                tasks.add(() -> value == (int) target.getMethod("concurrent", int.class).invoke(null, value));
                tasks.add(() -> (value + 100) == (int) target.getMethod("concurrent", int.class).invoke(null, value + 100));
            }
            for (Future<Boolean> future : executor.invokeAll(tasks)) assertTrue(future.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void mismatchedReferenceTypesAreRejected() throws Exception {
        assertThrows(TransformerException.class, () -> this.transform(MismatchedTypes.class));
    }

    @Test
    public void mixedValueAndReferenceUsageIsRejected() throws Exception {
        assertThrows(TransformerException.class, () -> this.transform(MixedUsage.class));
    }

    private Class<?> transform(final Class<?>... transformerClasses) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        for (Class<?> transformerClass : transformerClasses) {
            manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        }
        byte[] bytes = manager.transform(Target.class.getName(), this.classProvider.getClass(Target.class.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class Target {

        public static Object identity;
        public static boolean same;
        public static Object namespaceIdentity;
        public static boolean namespaceSame;
        public static boolean ok = true;

        public static void consume() {
        }

        public static int refShare(final int value) {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

        public static int defaultValue() {
            int seen = 5;
            consume();
            return seen;
        }

        public static Object defaultObject() {
            String seen = "initial";
            consume();
            return seen;
        }

        public static int valueShare() {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

        public static int valueDefault() {
            int seen = 5;
            consume();
            return seen;
        }

        public static int namespaceShare(final int value) {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

        public static int isolatedShare(final int value) {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

        public static int globalShare(final int value) {
            int seen = 0;
            consume();
            consume();
            return seen;
        }

        public static int recursive(final int depth) {
            if (depth <= 0) return 0;
            consume();
            int child = recursive(depth - 1);
            consume();
            return child + 1;
        }

        public static int concurrent(final int value) {
            int seen = 0;
            consume();
            Thread.yield();
            consume();
            return seen;
        }

    }

    @CTransformer(Target.class)
    private static class RefTransformer {

        @CShadow
        private static Object identity;
        @CShadow
        private static boolean same;

        @CInject(method = "refShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared("ref") final CLocalIntRef ref, final int value) {
            identity = ref;
            ref.set(value + 1);
        }

        @CInject(method = "refShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared("ref") final CLocalIntRef ref, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            same = identity == ref;
            seen = ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class DefaultTransformer {

        @CInject(method = "defaultValue", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void defaultValue(@CShared("default") final CLocalIntRef ref, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            seen = ref.get();
        }

        @CInject(method = "defaultObject", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void defaultObject(@CShared("defaultObject") final CLocalRef<String> ref, @CLocalVariable(name = "seen", modifiable = true) String seen) {
            seen = ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class ValueTransformer {

        @CInject(method = "valueShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared("value") int value) {
            value = 7;
        }

        @CInject(method = "valueShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared("value") final int value, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            seen = value;
        }

        @CInject(method = "valueDefault", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V"))
        private static void defaultValue(@CShared("valueDefault") final int value, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            seen = value;
        }

    }

    @CTransformer(Target.class)
    private static class NamespaceWriter {

        @CShadow
        private static Object namespaceIdentity;

        @CInject(method = "namespaceShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared(value = "ns", namespace = "shared") final CLocalIntRef ref, final int value) {
            namespaceIdentity = ref;
            ref.set(value + 10);
        }

    }

    @CTransformer(Target.class)
    private static class NamespaceReader {

        @CShadow
        private static Object namespaceIdentity;
        @CShadow
        private static boolean namespaceSame;

        @CInject(method = "namespaceShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared(value = "ns", namespace = "shared") final CLocalIntRef ref, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            namespaceSame = namespaceIdentity == ref;
            seen = ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class GlobalWriter {

        @CInject(method = "globalShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared(value = "global", global = true) final CLocalIntRef ref, final int value) {
            ref.set(value + 10);
        }

    }

    @CTransformer(Target.class)
    private static class GlobalReader {

        @CInject(method = "globalShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared(value = "global", global = true) final CLocalIntRef ref, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            seen = ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class IsolatedWriter {

        @CInject(method = "isolatedShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared("isolated") final CLocalIntRef ref, final int value) {
            ref.set(value + 10);
        }

    }

    @CTransformer(Target.class)
    private static class IsolatedReader {

        @CInject(method = "isolatedShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared("isolated") final CLocalIntRef ref, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            seen = ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class RecursiveTransformer {

        @CShadow
        private static boolean ok;

        @CInject(method = "recursive", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared("recursive") final CLocalIntRef ref, final int depth) {
            ref.set(depth);
        }

        @CInject(method = "recursive", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared("recursive") final CLocalIntRef ref, final int depth) {
            ok = ok && ref.get() == depth;
        }

    }

    @CTransformer(Target.class)
    private static class ConcurrentTransformer {

        @CInject(method = "concurrent", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared("concurrent") final CLocalIntRef ref, final int value) {
            ref.set(value);
        }

        @CInject(method = "concurrent", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared("concurrent") final CLocalIntRef ref, @CLocalVariable(name = "seen", modifiable = true) int seen) {
            seen = ref.get();
        }

    }

    @CTransformer(Target.class)
    private static class MismatchedTypes {

        @CInject(method = "valueShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared("mismatch") final CLocalIntRef ref) {
            ref.set(1);
        }

        @CInject(method = "valueShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared("mismatch") final CLocalLongRef ref) {
            ref.set(2L);
        }

    }

    @CTransformer(Target.class)
    private static class MixedUsage {

        @CInject(method = "valueShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 0))
        private static void writer(@CShared("mixed") int value) {
            value = 1;
        }

        @CInject(method = "valueShare", target = @CTarget(value = "INVOKE", target = TARGET + "consume()V", ordinal = 1))
        private static void reader(@CShared("mixed") final CLocalIntRef ref) {
            ref.set(2);
        }

    }

}
