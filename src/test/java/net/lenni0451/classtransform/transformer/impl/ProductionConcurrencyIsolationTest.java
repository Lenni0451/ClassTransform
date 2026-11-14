package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShared;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CModifyVariable;
import net.lenni0451.classtransform.annotations.injection.CWrapMethod;
import net.lenni0451.classtransform.operation.COperation;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.local.CLocalIntRef;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductionConcurrencyIsolationTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void sharedValueIsIsolatedPerInvocation() throws Exception {
        Class<?> target = this.transform(SharedTarget.class, SharedTransformer.class);
        this.runConcurrently(target, "compute", 2, 0);
        assertEquals(8 * 200, ((AtomicInteger) target.getField("CALLS").get(null)).get());
    }

    @Test
    public void localModificationIsThreadSafe() throws Exception {
        Class<?> target = this.transform(LocalTarget.class, LocalTransformer.class);
        this.runConcurrently(target, "work", 2, 1);
    }

    @Test
    public void sharedValuesAreIsolatedWhenCallsOverlap() throws Exception {
        Class<?> target = this.transform(BarrierTarget.class, BarrierTransformer.class);
        target.getField("barrier").set(null, new CyclicBarrier(8));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                final int value = i;
                futures.add(executor.submit(() -> assertEquals(value * 2, invokeInt(target, "compute", value))));
            }
            for (Future<?> future : futures) future.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void recursiveCallsRemainCorrectUnderLoad() throws Exception {
        Class<?> target = this.transform(RecursiveTarget.class, RecursiveTransformer.class);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) {
                futures.add(executor.submit(() -> {
                    for (int i = 1; i <= 20; i++) {
                        assertEquals(factorial(i), invokeInt(target, "factorial", i));
                    }
                }));
            }
            for (Future<?> future : futures) future.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private void runConcurrently(final Class<?> target, final String method, final int multiplier, final int offset) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) {
                final int start = thread * 1000;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < 200; i++) {
                        int value = start + i;
                        int expected = value * multiplier + offset;
                        assertEquals(expected, invokeInt(target, method, value));
                    }
                }));
            }
            for (Future<?> future : futures) future.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private Class<?> transform(final Class<?> targetClass, final Class<?> transformerClass) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        byte[] bytes = manager.transform(targetClass.getName(), this.classProvider.getClass(targetClass.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }

    private static int invokeInt(final Class<?> target, final String method, final int value) {
        try {
            return (int) target.getMethod(method, int.class).invoke(null, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static int factorial(final int value) {
        int result = 1;
        for (int i = 2; i <= value; i++) result *= i;
        return result;
    }


    public static class SharedTarget {

        public static final AtomicInteger CALLS = new AtomicInteger();

        public static int compute(final int value) {
            CALLS.incrementAndGet();
            return value;
        }

    }

    @CTransformer(SharedTarget.class)
    public static class SharedTransformer {

        @CWrapMethod(method = "compute")
        public static int compute(final int value, final COperation<Integer> original, @CShared("value") final CLocalIntRef ref) {
            ref.set(value);
            int result = original.call(value);
            return result + ref.get();
        }

    }


    public static class LocalTarget {

        public static int work(final int value) {
            int local = value * 2;
            return local;
        }

    }

    @CTransformer(LocalTarget.class)
    public static class LocalTransformer {

        @CModifyVariable(method = "work", target = @CTarget("STORE"), name = "local")
        public static int local(final int value) {
            return value + 1;
        }

    }


    public static class RecursiveTarget {

        public static int factorial(final int value) {
            return value <= 1 ? 1 : value * factorial(value - 1);
        }

    }

    @CTransformer(RecursiveTarget.class)
    public static class RecursiveTransformer {

        @CWrapMethod(method = "factorial")
        public static int factorial(final int value, final COperation<Integer> original) {
            return original.call(value);
        }

    }


    public static class BarrierTarget {

        public static CyclicBarrier barrier;

        public static int compute(final int value) {
            try {
                barrier.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return value;
        }

    }

    @CTransformer(BarrierTarget.class)
    public static class BarrierTransformer {

        @CWrapMethod(method = "compute")
        public static int compute(final int value, final COperation<Integer> original, @CShared("value") final CLocalIntRef ref) {
            ref.set(value);
            int result = original.call(value);
            return result + ref.get();
        }

    }

}
