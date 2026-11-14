package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import net.lenni0451.classtransform.utils.CTargetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("Select array element accesses")
    public void selectArrayElementAccesses() {
        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "Test", "array", "[I"));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IALOAD));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "Test", "array", "[I"));
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IASTORE));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "Test", "array", "[I"));
        method.instructions.add(new InsnNode(Opcodes.ARRAYLENGTH));
        method.instructions.add(new InsnNode(Opcodes.RETURN));

        FieldTarget fieldTarget = new FieldTarget();
        List<AbstractInsnNode> get = fieldTarget.getTargets(this.injectionTargets, method, this.getArrayTarget("LTest;array:[I", "array=get"), this.emptySlice);
        assertEquals(1, get.size());
        assertEquals(0, method.instructions.indexOf(get.get(0)));
        List<AbstractInsnNode> set = fieldTarget.getTargets(this.injectionTargets, method, this.getArrayTarget("LTest;array:[I", "array=set"), this.emptySlice);
        assertEquals(1, set.size());
        assertEquals(3, method.instructions.indexOf(set.get(0)));
        List<AbstractInsnNode> length = fieldTarget.getTargets(this.injectionTargets, method, this.getArrayTarget("LTest;array:[I", "array=length"), this.emptySlice);
        assertEquals(1, length.size());
        assertEquals(7, method.instructions.indexOf(length.get(0)));
    }

    @Test
    @DisplayName("Limit array search with fuzz")
    public void limitArraySearchWithFuzz() {
        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "Test", "array", "[I"));
        method.instructions.add(new InsnNode(Opcodes.NOP));
        method.instructions.add(new InsnNode(Opcodes.NOP));
        method.instructions.add(new InsnNode(Opcodes.NOP));
        method.instructions.add(new InsnNode(Opcodes.IALOAD));
        method.instructions.add(new InsnNode(Opcodes.RETURN));

        FieldTarget fieldTarget = new FieldTarget();
        assertTrue(fieldTarget.getTargets(this.injectionTargets, method, this.getArrayTarget("LTest;array:[I", "array=get", "fuzz=1"), this.emptySlice).isEmpty());
        assertEquals(1, fieldTarget.getTargets(this.injectionTargets, method, this.getArrayTarget("LTest;array:[I", "array=get", "fuzz=2"), this.emptySlice).size());
        assertEquals(1, fieldTarget.getTargets(this.injectionTargets, method, this.getArrayTarget("LTest;array:[I", "array=get"), this.emptySlice).size());
    }

    @Test
    @DisplayName("Ignore non array fields for array accesses")
    public void ignoreNonArrayFieldsForArrayAccesses() {
        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "Test", "value", "I"));
        method.instructions.add(new InsnNode(Opcodes.IALOAD));
        method.instructions.add(new InsnNode(Opcodes.RETURN));

        FieldTarget fieldTarget = new FieldTarget();
        assertTrue(fieldTarget.getTargets(this.injectionTargets, method, this.getArrayTarget("LTest;value:I", "array=get"), this.emptySlice).isEmpty());
    }

    private CTarget getArrayTarget(final String target, final String... args) {
        return CTargetImpl.of("FIELD", target, CTarget.Shift.BEFORE, -1, false, args);
    }

}
