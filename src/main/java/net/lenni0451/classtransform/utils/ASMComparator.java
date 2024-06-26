package net.lenni0451.classtransform.utils;

import org.objectweb.asm.tree.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A utility class to compare ASM types with each other.<br>
 * This is required because some ASM types do not override the {@code equals} method.
 */
public class ASMComparator {

    /**
     * Compare two InsnList instance with each other.
     *
     * @param insnList1 The first InsnList
     * @param insnList2 The second InsnList
     * @return If the two InsnList instances are equal
     */
    public static boolean equals(final InsnList insnList1, final InsnList insnList2) {
        if (insnList1 == insnList2) return true;
        if (insnList1 == null || insnList2 == null) return false;
        if (insnList1.size() != insnList2.size()) return false;

        for (int i = 0; i < insnList1.size(); i++) {
            AbstractInsnNode insn1 = insnList1.get(i);
            AbstractInsnNode insn2 = insnList2.get(i);
            if (!equals(insn1, insn2)) return false;
        }
        return true;
    }

    /**
     * Compare two lists with AbstractInsnNode instances with each other.
     *
     * @param insnList1 The first list
     * @param insnList2 The second list
     * @return If the two lists are equal
     */
    public static boolean equals(final List<? extends AbstractInsnNode> insnList1, final List<? extends AbstractInsnNode> insnList2) {
        if (insnList1 == insnList2) return true;
        if (insnList1 == null || insnList2 == null) return false;
        if (insnList1.size() != insnList2.size()) return false;

        for (int i = 0; i < insnList1.size(); i++) {
            AbstractInsnNode insn1 = insnList1.get(i);
            AbstractInsnNode insn2 = insnList2.get(i);
            if (!equals(insn1, insn2)) return false;
        }
        return true;
    }

    /**
     * Compare two AbstractInsnNode instances with each other.
     *
     * @param insn1 The first AbstractInsnNode
     * @param insn2 The second AbstractInsnNode
     * @return If the two AbstractInsnNode instances are equal
     */
    public static boolean equals(final AbstractInsnNode insn1, final AbstractInsnNode insn2) {
        if (insn1 == insn2) return true;
        if (insn1 == null || insn2 == null) return false;
        if (insn1.getOpcode() != insn2.getOpcode()) return false;
        if (insn1.getType() != insn2.getType()) return false;

        if (insn1.getType() == AbstractInsnNode.INSN) {
            //The opcode is the only thing that matters
            return true;
        } else if (insn1.getType() == AbstractInsnNode.INT_INSN) {
            IntInsnNode intInsn1 = (IntInsnNode) insn1;
            IntInsnNode intInsn2 = (IntInsnNode) insn2;
            return intInsn1.operand == intInsn2.operand;
        } else if (insn1.getType() == AbstractInsnNode.VAR_INSN) {
            VarInsnNode varInsn1 = (VarInsnNode) insn1;
            VarInsnNode varInsn2 = (VarInsnNode) insn2;
            return varInsn1.var == varInsn2.var;
        } else if (insn1.getType() == AbstractInsnNode.TYPE_INSN) {
            TypeInsnNode typeInsn1 = (TypeInsnNode) insn1;
            TypeInsnNode typeInsn2 = (TypeInsnNode) insn2;
            return Objects.equals(typeInsn1.desc, typeInsn2.desc);
        } else if (insn1.getType() == AbstractInsnNode.FIELD_INSN) {
            FieldInsnNode fieldInsn1 = (FieldInsnNode) insn1;
            FieldInsnNode fieldInsn2 = (FieldInsnNode) insn2;
            return Objects.equals(fieldInsn1.owner, fieldInsn2.owner)
                    && Objects.equals(fieldInsn1.name, fieldInsn2.name)
                    && Objects.equals(fieldInsn1.desc, fieldInsn2.desc);
        } else if (insn1.getType() == AbstractInsnNode.METHOD_INSN) {
            MethodInsnNode methodInsn1 = (MethodInsnNode) insn1;
            MethodInsnNode methodInsn2 = (MethodInsnNode) insn2;
            return Objects.equals(methodInsn1.owner, methodInsn2.owner)
                    && Objects.equals(methodInsn1.name, methodInsn2.name)
                    && Objects.equals(methodInsn1.desc, methodInsn2.desc)
                    && methodInsn1.itf == methodInsn2.itf;
        } else if (insn1.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
            InvokeDynamicInsnNode invokeDynamicInsn1 = (InvokeDynamicInsnNode) insn1;
            InvokeDynamicInsnNode invokeDynamicInsn2 = (InvokeDynamicInsnNode) insn2;
            return Objects.equals(invokeDynamicInsn1.name, invokeDynamicInsn2.name)
                    && Objects.equals(invokeDynamicInsn1.desc, invokeDynamicInsn2.desc)
                    && Objects.equals(invokeDynamicInsn1.bsm, invokeDynamicInsn2.bsm)
                    && dynamicEquals(Arrays.asList(invokeDynamicInsn1.bsmArgs), Arrays.asList(invokeDynamicInsn2.bsmArgs));
        } else if (insn1.getType() == AbstractInsnNode.JUMP_INSN) {
            JumpInsnNode jumpInsn1 = (JumpInsnNode) insn1;
            JumpInsnNode jumpInsn2 = (JumpInsnNode) insn2;
            return equals(jumpInsn1.label, jumpInsn2.label);
        } else if (insn1.getType() == AbstractInsnNode.LABEL) {
            //Label instances are not equal, so we can't compare them. It should be enough to know that they exist in both lists
            return true;
        } else if (insn1.getType() == AbstractInsnNode.LDC_INSN) {
            LdcInsnNode ldcInsn1 = (LdcInsnNode) insn1;
            LdcInsnNode ldcInsn2 = (LdcInsnNode) insn2;
            return Objects.equals(ldcInsn1.cst, ldcInsn2.cst);
        } else if (insn1.getType() == AbstractInsnNode.IINC_INSN) {
            IincInsnNode iincInsn1 = (IincInsnNode) insn1;
            IincInsnNode iincInsn2 = (IincInsnNode) insn2;
            return iincInsn1.var == iincInsn2.var
                    && iincInsn1.incr == iincInsn2.incr;
        } else if (insn1.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
            TableSwitchInsnNode tableSwitchInsn1 = (TableSwitchInsnNode) insn1;
            TableSwitchInsnNode tableSwitchInsn2 = (TableSwitchInsnNode) insn2;
            return tableSwitchInsn1.min == tableSwitchInsn2.min
                    && tableSwitchInsn1.max == tableSwitchInsn2.max
                    && equals(tableSwitchInsn1.dflt, tableSwitchInsn2.dflt)
                    && equals(tableSwitchInsn1.labels, tableSwitchInsn2.labels);
        } else if (insn1.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
            LookupSwitchInsnNode lookupSwitchInsn1 = (LookupSwitchInsnNode) insn1;
            LookupSwitchInsnNode lookupSwitchInsn2 = (LookupSwitchInsnNode) insn2;
            return equals(lookupSwitchInsn1.dflt, lookupSwitchInsn2.dflt)
                    && Objects.equals(lookupSwitchInsn1.keys, lookupSwitchInsn2.keys)
                    && equals(lookupSwitchInsn1.labels, lookupSwitchInsn2.labels);
        } else if (insn1.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN) {
            MultiANewArrayInsnNode multiANewArrayInsn1 = (MultiANewArrayInsnNode) insn1;
            MultiANewArrayInsnNode multiANewArrayInsn2 = (MultiANewArrayInsnNode) insn2;
            return Objects.equals(multiANewArrayInsn1.desc, multiANewArrayInsn2.desc)
                    && multiANewArrayInsn1.dims == multiANewArrayInsn2.dims;
        } else if (insn1.getType() == AbstractInsnNode.FRAME) {
            FrameNode frameNode1 = (FrameNode) insn1;
            FrameNode frameNode2 = (FrameNode) insn2;
            return frameNode1.type == frameNode2.type
                    && dynamicEquals(frameNode1.local, frameNode2.local)
                    && dynamicEquals(frameNode1.stack, frameNode2.stack);
        } else if (insn1.getType() == AbstractInsnNode.LINE) {
            LineNumberNode lineNumberNode1 = (LineNumberNode) insn1;
            LineNumberNode lineNumberNode2 = (LineNumberNode) insn2;
            return lineNumberNode1.line == lineNumberNode2.line
                    && equals(lineNumberNode1.start, lineNumberNode2.start);
        }
        return false;
    }

    private static boolean dynamicEquals(final List<Object> list1, final List<Object> list2) {
        if (list1 == list2) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            Object obj1 = list1.get(i);
            Object obj2 = list2.get(i);
            if (obj1 == obj2) continue;
            if (obj1 == null || obj2 == null) return false;

            if (obj1 instanceof AbstractInsnNode && obj2 instanceof AbstractInsnNode) {
                if (!equals((AbstractInsnNode) obj1, (AbstractInsnNode) obj2)) return false;
            } else {
                if (!Objects.equals(obj1, obj2)) return false;
            }
        }
        return true;
    }

}
