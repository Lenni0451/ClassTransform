package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
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
 * e.g. {@code java/lang/System.out:Ljava/io/PrintStream;}
 */
@ParametersAreNonnullByDefault
public class FieldTarget implements IInjectionTarget {

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
        MemberDeclaration memberDeclaration = ASMUtils.splitMemberDeclaration(target.target());
        if (memberDeclaration == null) return null;

        boolean allAccess = this.nonStaticAccess == -1 && this.staticAccess == -1;
        int i = 0;
        for (AbstractInsnNode instruction : this.getSlice(injectionTargets, method, slice)) {
            if (!(instruction instanceof FieldInsnNode)) continue;
            if (!allAccess && (this.nonStaticAccess != instruction.getOpcode() && this.staticAccess != instruction.getOpcode())) continue;
            FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
            if (fieldInsnNode.owner.equals(memberDeclaration.getOwner()) && fieldInsnNode.name.equals(memberDeclaration.getName()) && fieldInsnNode.desc.equals(memberDeclaration.getDesc())) {
                if (target.ordinal() == -1 || target.ordinal() == i) targets.add(instruction);
                i++;
            }
        }
        return targets;
    }

}
