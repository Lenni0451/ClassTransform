package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.expression.CDefinition;
import net.lenni0451.classtransform.annotations.expression.CExpression;
import net.lenni0451.classtransform.annotations.injection.CModifyExpressionValue;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.mappings.impl.VoidMapper;
import net.lenni0451.classtransform.test.TestClassLoader;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProductionRemappingTest {

    private static final String METHOD_TARGET = "net/lenni0451/classtransform/transformer/impl/ProductionRemappingTest$MethodTarget";
    private static final String FIELD_TARGET = "net/lenni0451/classtransform/transformer/impl/ProductionRemappingTest$FieldTarget";
    private static final String FIELD_TARGET_DESC = "Lnet/lenni0451/classtransform/transformer/impl/ProductionRemappingTest$FieldTarget;";
    private static final String OBFUSCATED_TARGET = "net/lenni0451/classtransform/transformer/impl/ObfuscatedTarget";
    private static final String CLASS_TARGET = "net/lenni0451/classtransform/transformer/impl/ProductionRemappingTest$ClassTarget";
    private static final String OBFUSCATED_PARENT = "net/lenni0451/classtransform/transformer/impl/ObfuscatedParent";
    private static final String PARENT_TARGET = "net/lenni0451/classtransform/transformer/impl/ProductionRemappingTest$ParentTarget";
    private static final String OBFUSCATED_INTERFACE = "net/lenni0451/classtransform/transformer/impl/ObfuscatedInterface";
    private static final String INTERFACE_TARGET = "net/lenni0451/classtransform/transformer/impl/ProductionRemappingTest$InterfaceTarget";

    private final BasicClassProvider classProvider = new BasicClassProvider();

    @Test
    public void remapsInjectionMethodSelector() throws Exception {
        VoidMapper mapper = new VoidMapper();
        mapper.getRemapper().addMethodMapping(METHOD_TARGET, "mapped", "()I", "actual");
        Class<?> target = this.transform(MethodTarget.class, MethodTransformer.class, mapper);
        assertEquals(15, target.getMethod("actual").invoke(null));
    }

    @Test
    public void remapsInjectionFieldTarget() throws Exception {
        VoidMapper mapper = new VoidMapper();
        mapper.getRemapper().addFieldMapping(FIELD_TARGET, "mappedValue", "actualValue");
        Class<?> target = this.transform(FieldTarget.class, FieldTransformer.class, mapper);
        assertEquals(42, target.getMethod("read").invoke(null));
    }

    @Test
    public void remapsTransformerClassTarget() throws Exception {
        VoidMapper mapper = new VoidMapper();
        mapper.getRemapper().addClassMapping(OBFUSCATED_TARGET, CLASS_TARGET);
        Class<?> target = this.transform(ClassTarget.class, ClassTargetTransformer.class, mapper);
        assertEquals(15, target.getMethod("actual").invoke(null));
    }

    @Test
    public void remapsInheritedExpressionDefinition() throws Exception {
        VoidMapper mapper = new VoidMapper();
        mapper.getRemapper().addClassMapping(OBFUSCATED_PARENT, PARENT_TARGET);
        Class<?> target = this.transform(ChildTarget.class, ChildTargetTransformer.class, mapper);
        Object instance = target.getConstructor().newInstance();
        assertEquals(15, target.getMethod("read").invoke(instance));
    }

    @Test
    public void remapsInterfaceTarget() throws Exception {
        VoidMapper mapper = new VoidMapper();
        mapper.getRemapper().addClassMapping(OBFUSCATED_INTERFACE, INTERFACE_TARGET);
        Class<?> target = this.transform(InterfaceTarget.class, InterfaceTargetTransformer.class, mapper);
        assertEquals(15, target.getMethod("actual").invoke(null));
    }

    private Class<?> transform(final Class<?> targetClass, final Class<?> transformerClass, final VoidMapper mapper) throws Exception {
        TransformerManager manager = new TransformerManager(this.classProvider, mapper);
        manager.setFailStrategy(FailStrategy.THROW);
        manager.addTransformer(ASMUtils.fromBytes(this.classProvider.getClass(transformerClass.getName())));
        byte[] bytes = manager.transform(targetClass.getName(), this.classProvider.getClass(targetClass.getName()));
        assertNotNull(bytes);
        return TestClassLoader.load(ASMUtils.fromBytes(bytes));
    }


    public static class MethodTarget {

        public static int actual() {
            return 5;
        }

    }

    @CTransformer(MethodTarget.class)
    public static class MethodTransformer {

        @CModifyReturnValue(method = "mapped")
        public static int modify(final int original) {
            return original + 10;
        }

    }


    public static class FieldTarget {

        public static int actualValue = 5;

        public static int read() {
            return actualValue;
        }

    }

    @CTransformer(FieldTarget.class)
    public static class FieldTransformer {

        @CRedirect(method = "read", target = @CTarget(value = "FIELD", target = FIELD_TARGET_DESC + "mappedValue:I"))
        public static int redirect() {
            return 42;
        }

    }


    public static class ClassTarget {

        public static int actual() {
            return 5;
        }

    }

    @CTransformer(name = "net.lenni0451.classtransform.transformer.impl.ObfuscatedTarget")
    public static class ClassTargetTransformer {

        @CModifyReturnValue(method = "actual")
        public static int modify(final int original) {
            return original + 10;
        }

    }


    public static class ParentTarget {

        public int parentValue() {
            return 5;
        }

    }

    public static class ChildTarget extends ParentTarget {

        public int read() {
            return this.parentValue();
        }

    }

    @CTransformer(ChildTarget.class)
    public static class ChildTargetTransformer {

        @CDefinition(id = "parentValue", method = "Lnet/lenni0451/classtransform/transformer/impl/ObfuscatedParent;parentValue()I")
        @CExpression("?.parentValue()")
        @CModifyExpressionValue(method = "read", target = @CTarget("EXPRESSION"))
        public int modify(final int original) {
            return original + 10;
        }

    }


    public interface InterfaceTarget {

        static int actual() {
            return 5;
        }

    }

    @CTransformer(name = "net.lenni0451.classtransform.transformer.impl.ObfuscatedInterface")
    public static class InterfaceTargetTransformer {

        @CModifyReturnValue(method = "actual")
        public static int modify(final int original) {
            return original + 10;
        }

    }

}
