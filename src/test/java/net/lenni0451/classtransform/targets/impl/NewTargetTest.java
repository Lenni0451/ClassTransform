package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NewTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get new targets")
    public void getNewTargets() {
        NewTarget newTarget = new NewTarget();
        List<AbstractInsnNode> insns = newTarget.getTargets(this.injectionTargets, this.method, this.getTarget("Test", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.INVOKESPECIAL, insns.get(0).getOpcode());
        MethodInsnNode methodInsnNode = (MethodInsnNode) insns.get(0);
        assertEquals("Test", methodInsnNode.owner);
    }

    @Test
    @DisplayName("Check if owner is target")
    public void checkOwnerIsTarget() {
        Method isTarget = assertDoesNotThrow(() -> NewTarget.class.getDeclaredMethod("isTarget", String.class, String.class));
        isTarget.setAccessible(true);

        NewTarget newTarget = new NewTarget();
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "java/lang/String")));
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "java.lang.String")));
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "Ljava.lang.String;")));
        assertTrue(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "Ljava/lang/String;")));
        assertFalse(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "Ljava.lang.String")));
        assertFalse(assertDoesNotThrow(() -> (boolean) isTarget.invoke(newTarget, "java/lang/String", "Ljava/lang/String")));
    }

}
