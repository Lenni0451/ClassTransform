package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConstructorHeadTargetTest extends ATargetTest {

    @Test
    @DisplayName("Get constructor head after super call")
    public void getConstructorHeadAfterSuperCall() {
        MethodNode constructor = this.constructor();
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));

        List<AbstractInsnNode> insns = new ConstructorHeadTarget().getTargets(this.injectionTargets, constructor, this.getTarget("", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.RETURN, insns.get(0).getOpcode());
    }

    @Test
    @DisplayName("Get constructor head after this call")
    public void getConstructorHeadAfterThisCall() {
        MethodNode constructor = this.constructor();
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "Test", "<init>", "()V", false));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, "Test", "field", "I"));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));

        List<AbstractInsnNode> insns = new ConstructorHeadTarget().getTargets(this.injectionTargets, constructor, this.getTarget("", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.PUTFIELD, insns.get(0).getOpcode());
    }

    @Test
    @DisplayName("Ignore allocations in super call arguments")
    public void ignoreAllocationsInSuperCallArguments() {
        MethodNode constructor = this.constructor();
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Object"));
        constructor.instructions.add(new InsnNode(Opcodes.DUP));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "Test", "<init>", "(Ljava/lang/Object;)V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));

        List<AbstractInsnNode> insns = new ConstructorHeadTarget().getTargets(this.injectionTargets, constructor, this.getTarget("", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(Opcodes.RETURN, insns.get(0).getOpcode());
    }

    @Test
    @DisplayName("Fall back to head for regular methods")
    public void fallBackToHeadForRegularMethods() {
        List<AbstractInsnNode> insns = new ConstructorHeadTarget().getTargets(this.injectionTargets, this.method, this.getTarget("", CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(this.method.instructions.getFirst(), insns.get(0));
    }

    private MethodNode constructor() {
        return new MethodNode(0, "<init>", "()V", null, null);
    }

}
