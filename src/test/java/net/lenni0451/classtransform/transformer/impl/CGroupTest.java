package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CGroup;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CInject;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CGroupTest {

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void groupMinimumPasses() {
        assertDoesNotThrow(() -> this.transform(MinimumPass.class));
    }

    @Test
    public void groupMinimumFails() {
        assertThrows(TransformerException.class, () -> this.transform(MinimumFail.class));
    }

    @Test
    public void groupMaximumPasses() {
        assertDoesNotThrow(() -> this.transform(MaximumPass.class));
    }

    @Test
    public void groupMaximumFails() {
        assertThrows(TransformerException.class, () -> this.transform(MaximumFail.class));
    }

    @Test
    public void multipleGroupsPass() {
        assertDoesNotThrow(() -> this.transform(MultiplePass.class));
    }

    @Test
    public void multipleGroupsFail() {
        assertThrows(TransformerException.class, () -> this.transform(MultipleFail.class));
    }

    private void transform(final Class<?> transformerClass) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        manager.transform(GroupTarget.class.getName(), this.classProvider.getClass(GroupTarget.class.getName()));
    }


    public static class GroupTarget {

        public static int method() {
            return Math.abs(-1);
        }

    }

    @CTransformer(GroupTarget.class)
    public static class MinimumPass {

        @CGroup(name = "test", min = 2)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void first() {
        }

        @CGroup(name = "test", min = 2)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void second() {
        }

    }

    @CTransformer(GroupTarget.class)
    public static class MinimumFail {

        @CGroup(name = "test", min = 2)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void first() {
        }

    }

    @CTransformer(GroupTarget.class)
    public static class MaximumPass {

        @CGroup(name = "test", max = 2)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void first() {
        }

        @CGroup(name = "test", max = 2)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void second() {
        }

    }

    @CTransformer(GroupTarget.class)
    public static class MaximumFail {

        @CGroup(name = "test", max = 1)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void first() {
        }

        @CGroup(name = "test", max = 1)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void second() {
        }

    }

    @CTransformer(GroupTarget.class)
    public static class MultiplePass {

        @CGroup(name = "first", min = 1)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void first() {
        }

        @CGroup(name = "second", min = 1)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void second() {
        }

    }

    @CTransformer(GroupTarget.class)
    public static class MultipleFail {

        @CGroup(name = "first", min = 1)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void first() {
        }

        @CGroup(name = "second", min = 2)
        @CInject(method = "method", target = @CTarget("HEAD"))
        public static void second() {
        }

    }

}
