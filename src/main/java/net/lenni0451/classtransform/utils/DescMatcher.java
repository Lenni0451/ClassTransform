package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CDesc;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Match members in a class with {@link CDesc} selectors.
 */
@ParametersAreNonnullByDefault
public class DescMatcher {

    /**
     * Add all methods matching the given descriptors to the target set.
     *
     * @param methods The set to add the matched methods to
     * @param target  The class to search in
     * @param descs   The descriptors to match
     */
    public static void addMethods(final Set<MethodNode> methods, final ClassNode target, final CDesc[] descs) {
        for (CDesc desc : descs) {
            Type owner = Type.getType(desc.owner());
            if (owner.getSort() != Type.VOID && !owner.getInternalName().equals(target.name)) continue;
            Type[] args = new Type[desc.args().length];
            for (int i = 0; i < args.length; i++) args[i] = Type.getType(desc.args()[i]);
            String descriptor = Type.getMethodDescriptor(Type.getType(desc.ret()), args);
            List<MethodNode> current = new ArrayList<>();
            for (MethodNode method : target.methods) {
                if (method.name.equals(desc.value()) && method.desc.equals(descriptor)) current.add(method);
            }
            checkCount(desc.value(), desc.min(), desc.max(), current.size());
            for (CDesc.Next next : desc.next()) {
                List<MethodNode> nested = new ArrayList<>();
                for (MethodNode method : current) addNestedMethods(nested, target, method, next);
                checkCount(next.name(), next.min(), next.max(), nested.size());
                current = nested;
                if (current.isEmpty()) break;
            }
            methods.addAll(current);
        }
    }

    private static void addNestedMethods(final List<MethodNode> methods, final ClassNode target, final MethodNode method, final CDesc.Next next) {
        Type[] args = new Type[next.args().length];
        for (int i = 0; i < args.length; i++) args[i] = Type.getType(next.args()[i]);
        String descriptor = Type.getMethodDescriptor(Type.getType(next.ret()), args);
        for (AbstractInsnNode instruction : method.instructions) {
            if (!(instruction instanceof InvokeDynamicInsnNode)) continue;
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) instruction;
            if (!"java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner())) continue;
            if (indy.bsmArgs.length < 2 || !(indy.bsmArgs[1] instanceof Handle)) continue;
            Handle implementation = (Handle) indy.bsmArgs[1];
            if (!implementation.getOwner().equals(target.name)) continue;
            if (!next.name().isEmpty() && !implementation.getName().equals(next.name())) continue;
            if (!implementation.getDesc().equals(descriptor)) continue;
            MethodNode nested = getMethod(target, implementation.getName(), implementation.getDesc());
            if (nested != null && !methods.contains(nested)) methods.add(nested);
        }
    }

    private static MethodNode getMethod(final ClassNode target, final String name, final String desc) {
        for (MethodNode method : target.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }

    /**
     * @param desc   The descriptor
     * @param method The method instruction
     * @return If the instruction matches the descriptor
     */
    public static boolean matchesMethod(final CDesc desc, final MethodInsnNode method) {
        Type owner = Type.getType(desc.owner());
        if (owner.getSort() != Type.VOID && !owner.getInternalName().equals(method.owner)) return false;
        if (!desc.value().isEmpty() && !desc.value().equals(method.name)) return false;
        Type[] args = new Type[desc.args().length];
        for (int i = 0; i < args.length; i++) args[i] = Type.getType(desc.args()[i]);
        return Type.getMethodDescriptor(Type.getType(desc.ret()), args).equals(method.desc);
    }

    /**
     * @param desc  The descriptor
     * @param field The field instruction
     * @return If the instruction matches the descriptor
     */
    public static boolean matchesField(final CDesc desc, final FieldInsnNode field) {
        Type owner = Type.getType(desc.owner());
        if (owner.getSort() != Type.VOID && !owner.getInternalName().equals(field.owner)) return false;
        if (!desc.value().isEmpty() && !desc.value().equals(field.name)) return false;
        return Type.getType(desc.ret()).getDescriptor().equals(field.desc);
    }

    /**
     * Validate that a descriptor matched the configured number of times.
     *
     * @param desc    The descriptor
     * @param matches The number of matches
     */
    public static void checkMatchCount(final CDesc desc, final int matches) {
        checkCount(desc.value(), desc.min(), desc.max(), matches);
    }

    private static void checkCount(final String name, final int min, final int max, final int matches) {
        if (matches < min) throw new IllegalArgumentException("Descriptor '" + name + "' matched " + matches + " times but requires at least " + min);
        if (max >= 1 && matches > max) throw new IllegalArgumentException("Descriptor '" + name + "' matched " + matches + " times but allows at most " + max);
    }

}
