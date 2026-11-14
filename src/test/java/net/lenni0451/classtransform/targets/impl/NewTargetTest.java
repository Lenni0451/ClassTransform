package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import net.lenni0451.classtransform.utils.CTargetImpl;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NewTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get new targets")
    public void getNewTargets() {
        NewTarget newTarget = new NewTarget();
        List<AbstractInsnNode> insns = newTarget.getTargets(this.injectionTargets, this.method, this.getTarget("java/lang/String", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.INVOKESPECIAL, insns.get(0).getOpcode());
        MethodInsnNode methodInsnNode = (MethodInsnNode) insns.get(0);
        assertEquals("java/lang/String", methodInsnNode.owner);
    }

    @Test
    @DisplayName("Check if owner is target")
    public void checkOwnerIsTarget() {
        Method isTarget = assertDoesNotThrow(() -> NewTarget.class.getDeclaredMethod("isTarget", String.class, String.class, String.class, String.class));
        isTarget.setAccessible(true);

        NewTarget newTarget = new NewTarget();
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "<init>", "", "java/lang/String")));
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "<init>", "", "java.lang.String")));
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "<init>", "", "Ljava.lang.String;")));
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "<init>", "", "Ljava/lang/String;")));
        assertFalse(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "<init>", "", "Ljava.lang.String")));
        assertFalse(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "<init>", "", "Ljava/lang/String")));
    }

    @Test
    @DisplayName("Match constructor descriptor without arguments")
    public void matchConstructorDescriptorWithoutArguments() {
        MethodNode method = this.constructors();
        List<AbstractInsnNode> targets = new NewTarget().getTargets(this.injectionTargets, method, this.descriptorTarget("java/lang/String", new Class<?>[0], void.class), this.emptySlice);
        assertEquals(1, targets.size());
        assertEquals(1, method.instructions.indexOf(targets.get(0)));
    }

    @Test
    @DisplayName("Match constructor descriptor with arguments")
    public void matchConstructorDescriptorWithArguments() {
        MethodNode method = this.constructors();
        List<AbstractInsnNode> targets = new NewTarget().getTargets(this.injectionTargets, method, this.descriptorTarget("java/lang/String", new Class<?>[]{int.class}, void.class), this.emptySlice);
        assertEquals(1, targets.size());
        assertEquals(4, method.instructions.indexOf(targets.get(0)));
    }

    @Test
    @DisplayName("Reject descriptor with non-void return type")
    public void rejectDescriptorWithNonVoidReturnType() {
        MethodNode method = this.constructors();
        List<AbstractInsnNode> targets = new NewTarget().getTargets(this.injectionTargets, method, this.descriptorTarget("java/lang/String", new Class<?>[0], Object.class), this.emptySlice);
        assertTrue(targets.isEmpty());
    }

    @Test
    @DisplayName("Reject descriptor with foreign owner")
    public void rejectDescriptorWithForeignOwner() {
        MethodNode method = this.constructors();
        List<AbstractInsnNode> targets = new NewTarget().getTargets(this.injectionTargets, method, this.descriptorTarget("java/lang/Object", new Class<?>[0], void.class), this.emptySlice);
        assertTrue(targets.isEmpty());
    }

    private MethodNode constructors() {
        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "()V"));
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/String"));
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "(I)V"));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private CTarget descriptorTarget(final String owner, final Class<?>[] args, final Class<?> ret) {
        Map<String, Object> values = new HashMap<>();
        values.put("value", "<init>");
        values.put("owner", Type.getObjectType(owner));
        values.put("ret", Type.getType(ret));
        if (args.length > 0) {
            List<Type> argumentTypes = new ArrayList<>();
            for (Class<?> argument : args) argumentTypes.add(Type.getType(argument));
            values.put("args", argumentTypes);
        }
        CDesc desc = AnnotationParser.parse(CDesc.class, this.classTree, this.classProvider, values);
        return CTargetImpl.of("NEW", "", CTarget.Shift.BEFORE, -1, false, new String[0], desc);
    }

}
