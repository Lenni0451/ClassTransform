package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The parts of a string concatenation.<br>
 * Supports both {@code invokedynamic} string concat factories and {@link StringBuilder} chains.
 */
@ParametersAreNonnullByDefault
public class StringConcatInfo {

    private static final String STRING_CONCAT_FACTORY = "java/lang/invoke/StringConcatFactory";
    private static final String STRING_BUILDER = Type.getInternalName(StringBuilder.class);

    /**
     * Get the concatenation information of a flow value.
     *
     * @param node The flow value
     * @return The information or null if the value is not a string concatenation
     */
    @Nullable
    public static StringConcatInfo get(final ExpressionFlow.FlowValue node) {
        if (node.isComplex()) return null;
        if (node.getInsn() instanceof InvokeDynamicInsnNode) return fromInvokeDynamic(node);
        if (node.getInsn() instanceof MethodInsnNode) return fromStringBuilder(node);
        return null;
    }

    @Nullable
    private static StringConcatInfo fromInvokeDynamic(final ExpressionFlow.FlowValue node) {
        InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node.getInsn();
        if (!STRING_CONCAT_FACTORY.equals(indy.bsm.getOwner())) return null;
        List<ExpressionFlow.FlowValue> parts = new ArrayList<>();
        if (indy.bsm.getName().equals("makeConcat")) {
            if (node.inputCount() != Type.getArgumentTypes(indy.desc).length) return null;
            for (int i = 0; i < node.inputCount(); i++) parts.add(node.getInput(i));
        } else if (indy.bsm.getName().equals("makeConcatWithConstants")) {
            if (indy.bsmArgs.length < 1 || !(indy.bsmArgs[0] instanceof String)) return null;
            String template = (String) indy.bsmArgs[0];
            int argument = 0;
            int constant = 1;
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < template.length(); i++) {
                char c = template.charAt(i);
                if (c != '\u0001' && c != '\u0002') {
                    text.append(c);
                    continue;
                }
                if (text.length() > 0) {
                    parts.add(createConstant(text.toString()));
                    text.setLength(0);
                }
                if (c == '\u0001') {
                    if (argument >= node.inputCount()) return null;
                    parts.add(unwrapArgument(node.getInput(argument++)));
                } else {
                    if (constant >= indy.bsmArgs.length) return null;
                    parts.add(createConstant(indy.bsmArgs[constant++]));
                }
            }
            if (text.length() > 0) parts.add(createConstant(text.toString()));
            if (argument != node.inputCount()) return null;
        } else {
            return null;
        }
        return parts.size() < 2 ? null : new StringConcatInfo(parts);
    }

    @Nullable
    private static StringConcatInfo fromStringBuilder(final ExpressionFlow.FlowValue node) {
        MethodInsnNode call = (MethodInsnNode) node.getInsn();
        if (!call.owner.equals(STRING_BUILDER) || !call.name.equals("toString")) return null;
        if (node.inputCount() < 1) return null;

        List<ExpressionFlow.FlowValue> parts = new ArrayList<>();
        ExpressionFlow.FlowValue current = node.getInput(0);
        while (true) {
            if (current.isComplex()) return null;
            if (current.getInsn() instanceof TypeInsnNode) {
                TypeInsnNode type = (TypeInsnNode) current.getInsn();
                if (type.getOpcode() != Opcodes.NEW || !type.desc.equals(STRING_BUILDER)) return null;
                if (!hasEmptyConstructor(current)) return null;
                break;
            }
            if (!(current.getInsn() instanceof MethodInsnNode)) return null;
            MethodInsnNode method = (MethodInsnNode) current.getInsn();
            if (!method.owner.equals(STRING_BUILDER) || !method.name.equals("append")) return null;
            if (current.inputCount() != 2 || Type.getArgumentTypes(method.desc).length != 1) return null;
            parts.add(unwrapArgument(current.getInput(1)));
            current = current.getInput(0);
        }
        if (parts.size() < 2) return null;
        Collections.reverse(parts);
        return new StringConcatInfo(parts);
    }

    private static boolean hasEmptyConstructor(final ExpressionFlow.FlowValue builder) {
        for (ExpressionFlow.FlowValue next : builder.getNext()) {
            if (next.isComplex() || !(next.getInsn() instanceof MethodInsnNode)) continue;
            MethodInsnNode method = (MethodInsnNode) next.getInsn();
            if (method.getOpcode() == Opcodes.INVOKESPECIAL
                    && method.owner.equals(STRING_BUILDER)
                    && method.name.equals("<init>")
                    && method.desc.equals("()V")
                    && next.inputCount() >= 1
                    && next.getInput(0) == builder) {
                return true;
            }
        }
        return false;
    }

    private static ExpressionFlow.FlowValue unwrapArgument(final ExpressionFlow.FlowValue value) {
        if (value.isComplex() || !(value.getInsn() instanceof MethodInsnNode) || value.inputCount() != 1) return value;
        MethodInsnNode call = (MethodInsnNode) value.getInsn();
        if (call.getOpcode() == Opcodes.INVOKESTATIC
                && call.owner.equals("java/lang/String")
                && call.name.equals("valueOf")
                && call.desc.equals("(Ljava/lang/Object;)Ljava/lang/String;")) {
            return value.getInput(0);
        }
        return value;
    }

    private static ExpressionFlow.FlowValue createConstant(final Object value) {
        AbstractInsnNode insn = new LdcInsnNode(value);
        return new ExpressionFlow.FlowValue(ExpressionTypes.getNewType(insn), insn);
    }


    private final List<ExpressionFlow.FlowValue> parts;

    private StringConcatInfo(final List<ExpressionFlow.FlowValue> parts) {
        this.parts = parts;
    }

    /**
     * @return The parts of the concatenation in order
     */
    public List<ExpressionFlow.FlowValue> getParts() {
        return this.parts;
    }

}
