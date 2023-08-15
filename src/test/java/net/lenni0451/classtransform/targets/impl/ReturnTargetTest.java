package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReturnTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get return target")
    public void getReturnTarget() {
        ReturnTarget returnTarget = new ReturnTarget();
        List<AbstractInsnNode> insns = returnTarget.getTargets(this.injectionTargets, this.method, this.getTarget("", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(2, insns.size());
        assertEquals(Opcodes.IRETURN, insns.get(0).getOpcode());
        assertEquals(Opcodes.DRETURN, insns.get(1).getOpcode());
    }

}
