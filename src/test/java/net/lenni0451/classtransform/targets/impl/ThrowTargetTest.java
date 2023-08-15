package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThrowTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get throw target")
    public void getThrowTarget() {
        ThrowTarget throwTarget = new ThrowTarget();
        List<AbstractInsnNode> insns = throwTarget.getTargets(this.injectionTargets, this.method, this.getTarget("", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.ATHROW, insns.get(0).getOpcode());
    }

}
