package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.DescMatcher;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A target for field access instructions.<br>
 * e.g. {@code java/lang/System.out:Ljava/io/PrintStream;}<br>
 * Array element accesses can be selected with the {@code array=get|set|length} argument.
 */
@ParametersAreNonnullByDefault
public class FieldTarget implements IInjectionTarget {

    private static final int ARRAY_SEARCH_FUZZ_DEFAULT = 8;

    private final int nonStaticAccess;
    private final int staticAccess;

    public FieldTarget() {
        this(-1, -1);
    }

    public FieldTarget(final int nonStaticAccess, final int staticAccess) {
        this.nonStaticAccess = nonStaticAccess;
        this.staticAccess = staticAccess;
    }

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        CDesc desc = target.desc();
        boolean useDesc = !desc.value().isEmpty();
        MemberDeclaration memberDeclaration = useDesc ? null : ASMUtils.splitMemberDeclaration(target.target());
        if (!useDesc && memberDeclaration == null) return null;

        int arrayOpcode = this.getArrayOpcode(target);
        int fuzz = this.getFuzz(target);
        boolean allAccess = this.nonStaticAccess == -1 && this.staticAccess == -1;
        int i = 0;
        for (AbstractInsnNode instruction : this.getSlice(injectionTargets, method, slice)) {
            if (!(instruction instanceof FieldInsnNode)) continue;
            if (!allAccess && (this.nonStaticAccess != instruction.getOpcode() && this.staticAccess != instruction.getOpcode())) continue;
            FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
            boolean matches = useDesc ? DescMatcher.matchesField(desc, fieldInsnNode) : fieldInsnNode.owner.equals(memberDeclaration.getOwner()) && fieldInsnNode.name.equals(memberDeclaration.getName()) && fieldInsnNode.desc.equals(memberDeclaration.getDesc());
            if (!matches) continue;
            if (arrayOpcode != 0) {
                if (fieldInsnNode.getOpcode() != Opcodes.GETFIELD && fieldInsnNode.getOpcode() != Opcodes.GETSTATIC) continue;
                if (Type.getType(fieldInsnNode.desc).getSort() != Type.ARRAY) continue;
            }
            if (target.ordinal() == -1 || target.ordinal() == i) {
                if (arrayOpcode == 0 || this.hasArrayAccess(method, fieldInsnNode, arrayOpcode, fuzz)) targets.add(instruction);
            }
            i++;
        }
        if (useDesc) DescMatcher.checkMatchCount(desc, i);
        return targets;
    }

    private int getArrayOpcode(final CTarget target) {
        for (String argument : target.args()) {
            if (!argument.startsWith("array=")) continue;
            String value = argument.substring(6);
            if ("get".equalsIgnoreCase(value)) return Opcodes.IALOAD;
            if ("set".equalsIgnoreCase(value)) return Opcodes.IASTORE;
            if ("length".equalsIgnoreCase(value)) return Opcodes.ARRAYLENGTH;
            return 0;
        }
        return 0;
    }

    private int getFuzz(final CTarget target) {
        for (String argument : target.args()) {
            if (!argument.startsWith("fuzz=")) continue;
            return Math.min(Math.max(Integer.parseInt(argument.substring(5)), 1), 32);
        }
        return ARRAY_SEARCH_FUZZ_DEFAULT;
    }

    private boolean hasArrayAccess(final MethodNode method, final FieldInsnNode fieldInsnNode, final int arrayOpcode, final int fuzz) {
        int opcode = arrayOpcode == Opcodes.ARRAYLENGTH ? arrayOpcode : Type.getType(fieldInsnNode.desc).getElementType().getOpcode(arrayOpcode);
        int position = 0;
        for (int index = method.instructions.indexOf(fieldInsnNode) + 1; index < method.instructions.size(); index++) {
            AbstractInsnNode instruction = method.instructions.get(index);
            if (instruction.getOpcode() == opcode) return true;
            if (instruction.getOpcode() == Opcodes.ARRAYLENGTH && position == 0) return false;
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode other = (FieldInsnNode) instruction;
                if (other.owner.equals(fieldInsnNode.owner) && other.name.equals(fieldInsnNode.name) && other.desc.equals(fieldInsnNode.desc)) return false;
            }
            if (position++ > fuzz) return false;
        }
        return false;
    }

}
