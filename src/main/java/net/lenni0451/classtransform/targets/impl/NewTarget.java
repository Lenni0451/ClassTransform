package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;
import static net.lenni0451.classtransform.utils.Types.argumentTypes;
import static net.lenni0451.classtransform.utils.Types.returnType;

/**
 * A target for {@link Opcodes#NEW} instructions.<br>
 * e.g. {@code java/lang/String}
 */
@ParametersAreNonnullByDefault
public class NewTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        int i = 0;
        for (AbstractInsnNode instruction : this.getSlice(injectionTargets, method, slice)) {
            if (instruction.getOpcode() != Opcodes.INVOKESPECIAL) continue;
            MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
            if (!this.isTarget(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, target.target())) continue;
            if (target.ordinal() == -1 || target.ordinal() == i) targets.add(instruction);
            i++;
        }
        return targets;
    }

    @Nullable
    @Override
    public RemapType dynamicRemap(AMapper mapper, Class<?> annotation, Map<String, Object> values, Method remappedMethod, TransformerManager transformerManager, ClassNode target, ClassNode transformer) {
        String targetString = (String) values.get("target");
        if (targetString == null) return null;

        MemberDeclaration memberDeclaration = ASMUtils.splitMemberDeclaration(targetString);
        if (memberDeclaration != null) return RemapType.MEMBER;
        else if (targetString.startsWith("(")) return RemapType.DESCRIPTOR;
        else return RemapType.CLASS;
    }

    private boolean isTarget(final String owner, final String name, final String desc, String target) {
        if (!name.equals("<init>")) return false;
        MemberDeclaration declaration = ASMUtils.splitMemberDeclaration(target);
        if (declaration != null) {
            return declaration.is(owner, name, desc);
        } else if (target.startsWith("(")) {
            if (desc.equals(target)) return true;

            Type expectedOwner = returnType(target);
            Type[] expectedArgs = argumentTypes(target);
            Type[] actualArgs = argumentTypes(desc);
            return owner.equals(expectedOwner.getInternalName()) && Arrays.equals(expectedArgs, actualArgs);
        } else {
            if (target.startsWith("L") && target.endsWith(";")) target = target.substring(1, target.length() - 1);
            return owner.equals(slash(target));
        }
    }

}
