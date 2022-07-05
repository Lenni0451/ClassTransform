package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.annotations.injection.CASM;
import net.lenni0451.classtransform.transformer.ATransformerTest;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.*;

class CASMTransformerTest extends ATransformerTest {

    private final CASMTransformer transformer = new CASMTransformer();
    private ClassNode stringClass;

    @BeforeEach
    public void setUp() {
        super.setUp();

        this.stringClass = ASMUtils.fromBytes(this.classProvider.getClass("java/lang/String"));
    }

    @Test
    @DisplayName("Method isolation")
    public void methodIsolation() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CASMTransformerTest$IsolationTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.stringClass, transformer);
        assertEquals(0, this.stringClass.fields.size());
    }

    @Test
    @DisplayName("Modify method node")
    public void modifyMethodNode() {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CASMTransformerTest$ModifyMethodNodeTestTransformer");
        this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.stringClass, transformer);
        MethodNode method = ASMUtils.getMethod(this.stringClass, "equals", "(Ljava/lang/Object;)Z");
        assertNotNull(method);
        assertEquals(2, method.instructions.size());
        assertEquals(Opcodes.ICONST_0, method.instructions.get(0).getOpcode());
        assertEquals(Opcodes.IRETURN, method.instructions.get(1).getOpcode());
    }

    @ParameterizedTest()
    @CsvSource({
            "LambdaTestTransformer",
            "FieldsTestTransformer"
    })
    @DisplayName("Throw if using invalid operations")
    public void throwIfUsingInvalidOperations(final String transformerName) {
        ClassNode transformer = this.getTransformerClass("net.lenni0451.classtransform.transformer.impl.CASMTransformerTest$" + transformerName);
        assertThrows(IllegalStateException.class, () -> this.transformer.transform(this.transformerManager, this.classProvider, this.injectionTargets, this.stringClass, transformer));
    }


    @CTransformer(String.class)
    private static class IsolationTestTransformer {

        @CASM
        public static void clearFields(ClassNode classNode) {
            classNode.fields.clear();
        }

    }

    @CTransformer(String.class)
    private static class ModifyMethodNodeTestTransformer {

        @CASM("equals(Ljava/lang/Object;)Z")
        public static void clearFields(MethodNode methodNode) {
            methodNode.instructions.clear();
            methodNode.tryCatchBlocks.clear();
            methodNode.instructions.add(new InsnNode(Opcodes.ICONST_0));
            methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
        }

    }

    @CTransformer(String.class)
    private static class LambdaTestTransformer {

        @CASM
        public static void removeAllMethods(ClassNode classNode) {
            classNode.methods.removeIf(m -> m.name.contains("a"));
        }

    }

    @CTransformer(String.class)
    private static class FieldsTestTransformer {

        private static String name;

        @CASM
        public static void removeAllMethods(ClassNode classNode) {
            name = classNode.name;
        }

    }

}