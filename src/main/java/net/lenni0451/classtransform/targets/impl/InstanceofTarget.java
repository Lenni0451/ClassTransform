package net.lenni0451.classtransform.targets.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A target for {@code instanceof} instructions.<br>
 * e.g. {@code java/lang/String}
 * <p>
 * The target is compared against the internal name of the tested type; slashes are normalized.
 * The ordinal is not applied here and is handled by
 * {@link net.lenni0451.classtransform.targets.InjectionTargets#find} instead.
 * Returns an empty list when no instruction matches.
 */
@ParametersAreNonnullByDefault
public class InstanceofTarget implements IInjectionTarget {

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        List<AbstractInsnNode> targets = new ArrayList<>();
        String type = ASMUtils.slash(target.target());
        for (AbstractInsnNode instruction : this.getSlice(injectionTargets, method, slice)) {
            if (instruction.getOpcode() != Opcodes.INSTANCEOF) continue;
            if (!((TypeInsnNode) instruction).desc.equals(type)) continue;
            targets.add(instruction);
        }
        return targets;
    }

    @Nullable
    @Override
    public RemapType dynamicRemap(AMapper mapper, Class<?> annotation, Map<String, Object> values, Method remappedMethod, TransformerManager transformerManager, ClassNode target, ClassNode transformer) {
        return RemapType.CLASS;
    }

}
