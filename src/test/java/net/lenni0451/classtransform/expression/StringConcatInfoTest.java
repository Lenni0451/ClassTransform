package net.lenni0451.classtransform.expression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class StringConcatInfoTest {

    @Test
    @DisplayName("Unwrap String.valueOf in StringBuilder chains")
    public void unwrapStringValueOf() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "concat", "(Ljava/lang/Object;)Ljava/lang/String;", null, null);
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        method.instructions.add(new LdcInsnNode("value: "));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));

        method.maxLocals = 1;
        method.maxStack = 3;
        ClassNode classNode = new ClassNode();
        classNode.name = "Test";
        classNode.version = Opcodes.V1_8;
        classNode.methods.add(method);

        ExpressionFlow.FlowValue concat = null;
        for (ExpressionFlow.FlowValue flow : ExpressionFlow.analyze(classNode, method)) {
            if (flow.getInsn() instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) flow.getInsn();
                if (call.owner.equals("java/lang/StringBuilder") && call.name.equals("toString")) concat = flow;
            }
        }

        assertNotNull(concat);
        StringConcatInfo info = StringConcatInfo.get(concat);
        assertNotNull(info);
        List<ExpressionFlow.FlowValue> parts = info.getParts();
        assertEquals(2, parts.size());
        assertEquals("value: ", ((LdcInsnNode) parts.get(0).getInsn()).cst);
        AbstractInsnNode argument = parts.get(1).getInsn();
        assertSame(Opcodes.ALOAD, argument.getOpcode());
        assertEquals(0, ((VarInsnNode) argument).var);
    }

}
