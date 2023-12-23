package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpcodeTargetTest extends ATargetTest {

    @ParameterizedTest
    @CsvSource({
            Opcodes.GETSTATIC + ", GETSTATIC, 2",
            Opcodes.PUTSTATIC + ", PUTSTATIC, 2",
            Opcodes.GETFIELD + ", GETFIELD, 2",
            Opcodes.PUTFIELD + ", PUTFIELD, 2",
            Opcodes.INVOKEINTERFACE + ", INVOKEINTERFACE, 2",
            Opcodes.INVOKEVIRTUAL + ", INVOKEVIRTUAL, 2",
            Opcodes.INVOKESPECIAL + ", INVOKESPECIAL, 4",
            Opcodes.INVOKESTATIC + ", INVOKESTATIC, 2",
            Opcodes.NEW + ", NEW, 2",
            Opcodes.ACONST_NULL + ", ACONST_NULL, 2",
            Opcodes.ATHROW + ", ATHROW, 2",
            Opcodes.DRETURN + ", DRETURN, 2"
    })
    @DisplayName("Get opcode targets")
    public void getOpcodeTargets(final int opcode, final String name, final int count) {
        OpcodeTarget opcodeTarget = new OpcodeTarget();
        List<AbstractInsnNode> insns = opcodeTarget.getTargets(this.injectionTargets, this.method, this.getTarget(String.valueOf(opcode), CTarget.Shift.BEFORE, -1), this.emptySlice);
        insns.addAll(opcodeTarget.getTargets(this.injectionTargets, this.method, this.getTarget(name, CTarget.Shift.BEFORE, -1), this.emptySlice));
        assertEquals(count, insns.size());
        assertEquals(insns.get(0).getOpcode(), opcode);
        assertEquals(insns.get(0).getOpcode(), insns.get(1).getOpcode());
    }

    @Test
    @DisplayName("Get invalid opcode")
    public void getInvalidOpcode() {
        OpcodeTarget opcodeTarget = new OpcodeTarget();
        List<AbstractInsnNode> insns = opcodeTarget.getTargets(this.injectionTargets, this.method, this.getTarget("INVALID", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertNull(insns);
    }

}
