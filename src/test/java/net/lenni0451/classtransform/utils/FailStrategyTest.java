package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FailStrategyTest {

    private BasicClassProvider classProvider;
    private TransformerManager transformerManager;

    @BeforeEach
    public void setUp() {
        this.classProvider = new BasicClassProvider();
        this.transformerManager = new TransformerManager(this.classProvider);
    }

    @Test
    public void testThrowStrategy() {
        this.transformerManager.setFailStrategy(FailStrategy.THROW);
        this.transformerManager.addBytecodeTransformer((className, bytecode, calculateStackMapFrames) -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(RuntimeException.class, () -> this.transformerManager.transform(SCalculator.class.getName(), new byte[0]));
    }

    @Test
    public void testCancelStrategy() {
        this.transformerManager.setFailStrategy(FailStrategy.CANCEL);
        this.transformerManager.addBytecodeTransformer((className, bytecode, calculateStackMapFrames) -> {
            throw new RuntimeException("Test exception");
        });

        assertNull(this.transformerManager.transform(SCalculator.class.getName(), new byte[0]));
    }

}
