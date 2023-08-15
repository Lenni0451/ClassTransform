package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstantTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get null value")
    public void getNullValue() {
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, this.method, this.getTarget("null", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.ACONST_NULL, insns.get(0).getOpcode());
    }

    @ParameterizedTest
    @CsvSource({
            Opcodes.BIPUSH + ", 0",
            Opcodes.SIPUSH + ", 1",
            Opcodes.LDC + ", 2"
    })
    @DisplayName("Get int value")
    public void getIntValue(final int opcode, final int number) {
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, this.method, this.getTarget("int " + number, CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(opcode, insns.get(0).getOpcode());
        if (opcode == Opcodes.LDC) assertEquals(number, ((LdcInsnNode) insns.get(0)).cst);
        else assertEquals(number, ((IntInsnNode) insns.get(0)).operand);
    }

    @Test
    @DisplayName("Get long value")
    public void getLongValue() {
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, this.method, this.getTarget("long 3", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.LDC, insns.get(0).getOpcode());
        assertEquals(3L, ((LdcInsnNode) insns.get(0)).cst);
    }

    @Test
    @DisplayName("Get float value")
    public void getFloatValue() {
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, this.method, this.getTarget("float 4", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.LDC, insns.get(0).getOpcode());
        assertEquals(4F, ((LdcInsnNode) insns.get(0)).cst);
    }

    @Test
    @DisplayName("Get double value")
    public void getDoubleValue() {
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, this.method, this.getTarget("double 5", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.LDC, insns.get(0).getOpcode());
        assertEquals(5D, ((LdcInsnNode) insns.get(0)).cst);
    }

    @Test
    @DisplayName("Get string value")
    public void getStringValue() {
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, this.method, this.getTarget("string 6th string", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.LDC, insns.get(0).getOpcode());
        assertEquals("6th string", ((LdcInsnNode) insns.get(0)).cst);
    }

    @Test
    @DisplayName("Get type value")
    public void getTypeValue() {
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, this.method, this.getTarget("type Ljava/lang/Object;", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.LDC, insns.get(0).getOpcode());
        assertEquals(Type.getType(Object.class), ((LdcInsnNode) insns.get(0)).cst);
    }

}
