package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get all field accesses")
    public void getAllFields() {
        FieldTarget fieldTarget = new FieldTarget();
        List<AbstractInsnNode> insns = fieldTarget.getTargets(this.injectionTargets, this.method, this.getTarget("LTest;static:I", CTarget.Shift.BEFORE, -1), this.emptySlice);
        insns.addAll(fieldTarget.getTargets(this.injectionTargets, this.method, this.getTarget("LTest;virtual:Z", CTarget.Shift.BEFORE, -1), this.emptySlice));
        assertEquals(4, insns.size());
    }

    @Test
    @DisplayName("Get GET field accesses")
    public void getGETFields() {
        FieldTarget fieldTarget = new FieldTarget(Opcodes.GETFIELD, Opcodes.GETSTATIC);
        List<AbstractInsnNode> insns = fieldTarget.getTargets(this.injectionTargets, this.method, this.getTarget("LTest;static:I", CTarget.Shift.BEFORE, -1), this.emptySlice);
        insns.addAll(fieldTarget.getTargets(this.injectionTargets, this.method, this.getTarget("LTest;virtual:Z", CTarget.Shift.BEFORE, -1), this.emptySlice));
        assertEquals(2, insns.size());
        assertEquals(Opcodes.GETSTATIC, insns.get(0).getOpcode());
        assertEquals(Opcodes.GETFIELD, insns.get(1).getOpcode());
    }

    @Test
    @DisplayName("Get SET field accesses")
    public void getPUTFields() {
        FieldTarget fieldTarget = new FieldTarget(Opcodes.PUTFIELD, Opcodes.PUTSTATIC);
        List<AbstractInsnNode> insns = fieldTarget.getTargets(this.injectionTargets, this.method, this.getTarget("LTest;static:I", CTarget.Shift.BEFORE, -1), this.emptySlice);
        insns.addAll(fieldTarget.getTargets(this.injectionTargets, this.method, this.getTarget("LTest;virtual:Z", CTarget.Shift.BEFORE, -1), this.emptySlice));
        assertEquals(2, insns.size());
        assertEquals(Opcodes.PUTSTATIC, insns.get(0).getOpcode());
        assertEquals(Opcodes.PUTFIELD, insns.get(1).getOpcode());
    }

}