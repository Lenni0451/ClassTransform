package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeadTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get head target")
    public void getHeadTarget() {
        HeadTarget headTarget = new HeadTarget();
        List<AbstractInsnNode> insns = headTarget.getTargets(this.injectionTargets, this.method, this.getTarget("", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.GETSTATIC, insns.get(0).getOpcode());
    }

}