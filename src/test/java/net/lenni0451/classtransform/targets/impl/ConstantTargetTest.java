package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import net.lenni0451.classtransform.utils.CTargetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

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

    @Test
    @DisplayName("Expand zero conditions")
    public void expandZeroConditions() {
        MethodNode method = this.createComparisonMethod();
        ConstantTarget constantTarget = new ConstantTarget();

        List<AbstractInsnNode> lessThanZero = constantTarget.getTargets(this.injectionTargets, method, this.getExpandedTarget("LESS_THAN_ZERO"), this.emptySlice);
        assertEquals(2, lessThanZero.size());
        assertEquals(Opcodes.IFLT, lessThanZero.get(0).getOpcode());
        assertEquals(Opcodes.IFGE, lessThanZero.get(1).getOpcode());

        List<AbstractInsnNode> greaterThanZero = constantTarget.getTargets(this.injectionTargets, method, this.getExpandedTarget("GREATER_THAN_ZERO"), this.emptySlice);
        assertEquals(2, greaterThanZero.size());
        assertEquals(Opcodes.IFLE, greaterThanZero.get(0).getOpcode());
        assertEquals(Opcodes.IFGT, greaterThanZero.get(1).getOpcode());
    }

    @Test
    @DisplayName("Expand zero conditions with explicit zero constants")
    public void expandZeroConditionsWithExplicitZeroConstants() {
        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new LdcInsnNode(0));
        method.instructions.add(new InsnNode(Opcodes.RETURN));

        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> explicit = constantTarget.getTargets(this.injectionTargets, method, this.getExpandedTarget("LESS_THAN_ZERO"), this.emptySlice);
        assertEquals(2, explicit.size());
        assertEquals(Opcodes.ICONST_0, explicit.get(0).getOpcode());
        assertEquals(Opcodes.LDC, explicit.get(1).getOpcode());

        List<AbstractInsnNode> implicit = constantTarget.getTargets(this.injectionTargets, method, this.getExpandedTarget("LESS_THAN_ZERO", "implicitZero=true"), this.emptySlice);
        assertEquals(0, implicit.size());
    }

    @Test
    @DisplayName("Ignore conditional opcodes after comparisons")
    public void ignoreConditionalOpcodesAfterComparisons() {
        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        LabelNode label = new LabelNode();
        method.instructions.add(new InsnNode(Opcodes.LCMP));
        method.instructions.add(new JumpInsnNode(Opcodes.IFLE, label));
        method.instructions.add(new JumpInsnNode(Opcodes.IFGT, label));
        method.instructions.add(label);
        method.instructions.add(new InsnNode(Opcodes.RETURN));

        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, method, this.getExpandedTarget("LESS_THAN_OR_EQUAL_TO_ZERO"), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.IFGT, insns.get(0).getOpcode());
    }

    @Test
    @DisplayName("Expand zero conditions with ordinal and log")
    public void expandZeroConditionsWithOrdinalAndLog() {
        MethodNode method = this.createComparisonMethod();
        ConstantTarget constantTarget = new ConstantTarget();
        List<AbstractInsnNode> insns = constantTarget.getTargets(this.injectionTargets, method, CTargetImpl.of("CONSTANT", "int 0", CTarget.Shift.BEFORE, 1, false, new String[]{"expandZeroConditions=LESS_THAN_ZERO", "log=true"}), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.IFGE, insns.get(0).getOpcode());
    }

    private MethodNode createComparisonMethod() {
        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        LabelNode label = new LabelNode();
        method.instructions.add(new JumpInsnNode(Opcodes.IFLT, label));
        method.instructions.add(new JumpInsnNode(Opcodes.IFLE, label));
        method.instructions.add(new JumpInsnNode(Opcodes.IFGE, label));
        method.instructions.add(new JumpInsnNode(Opcodes.IFGT, label));
        method.instructions.add(label);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private CTarget getExpandedTarget(final String condition, final String... extraArgs) {
        String[] args = new String[extraArgs.length + 1];
        args[0] = "expandZeroConditions=" + condition;
        System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        return CTargetImpl.of("CONSTANT", "int 0", CTarget.Shift.BEFORE, -1, false, args);
    }

}
