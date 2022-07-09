package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.ATargetTest;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvokeTargetTest extends ATargetTest {

    @ParameterizedTest
    @CsvSource({
            "LTest;invokeInterface()V, " + Opcodes.INVOKEINTERFACE,
            "LTest;invokeVirtual(Ljava/lang/String;)Z, " + Opcodes.INVOKEVIRTUAL,
            "LTest;invokeSpecial()Ljava/io/FileInputStream;, " + Opcodes.INVOKESPECIAL,
            "LTest;invokeStatic(Ljava/lang/Integer;)I, " + Opcodes.INVOKESTATIC
    })
    @DisplayName("Get all invokes")
    public void getAllInvokes(final String target, final int opcode) {
        MemberDeclaration memberDeclaration = ASMUtils.splitMemberDeclaration(target);
        InvokeTarget invokeTarget = new InvokeTarget();
        List<AbstractInsnNode> insns = invokeTarget.getTargets(this.injectionTargets, this.method, this.getTarget(target, CTarget.Shift.BEFORE, -1), this.emptySlice);
        assertEquals(1, insns.size());
        assertEquals(opcode, insns.get(0).getOpcode());
        MethodInsnNode methodInsnNode = (MethodInsnNode) insns.get(0);
        assertEquals(memberDeclaration.getOwner(), methodInsnNode.owner);
        assertEquals(memberDeclaration.getName(), methodInsnNode.name);
        assertEquals(memberDeclaration.getDesc(), methodInsnNode.desc);
    }

}