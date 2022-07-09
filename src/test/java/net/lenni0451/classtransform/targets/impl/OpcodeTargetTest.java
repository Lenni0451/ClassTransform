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
            Opcodes.GETSTATIC + ", GETSTATIC",
            Opcodes.PUTSTATIC + ", PUTSTATIC",
            Opcodes.GETFIELD + ", GETFIELD",
            Opcodes.PUTFIELD + ", PUTFIELD",
            Opcodes.INVOKEINTERFACE + ", INVOKEINTERFACE",
            Opcodes.INVOKEVIRTUAL + ", INVOKEVIRTUAL",
            Opcodes.INVOKESPECIAL + ", INVOKESPECIAL",
            Opcodes.INVOKESTATIC + ", INVOKESTATIC",
            Opcodes.NEW + ", NEW",
            Opcodes.ACONST_NULL + ", ACONST_NULL",
            Opcodes.ATHROW + ", ATHROW",
            Opcodes.DRETURN + ", DRETURN"
    })
    @DisplayName("Get opcode targets")
    public void getOpcodeTargets(final int opcode, final String name) {
        OpcodeTarget opcodeTarget = new OpcodeTarget();
        List<AbstractInsnNode> insns = opcodeTarget.getTargets(this.injectionTargets, this.method, this.getTarget(String.valueOf(opcode), CTarget.Shift.BEFORE, -1), this.emptySlice);
        insns.addAll(opcodeTarget.getTargets(this.injectionTargets, this.method, this.getTarget(name, CTarget.Shift.BEFORE, -1), this.emptySlice));
        assertEquals(2, insns.size());
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