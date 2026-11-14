package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CDesc;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
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
            int matches = 0;
            for (MethodNode method : target.methods) {
                if (method.name.equals(desc.value()) && method.desc.equals(descriptor)) {
                    methods.add(method);
                    matches++;
                }
            }
            checkMatchCount(desc, matches);
        }
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

    private static void checkMatchCount(final CDesc desc, final int matches) {
        if (matches < desc.min()) throw new IllegalArgumentException("Descriptor '" + desc.value() + "' matched " + matches + " times but requires at least " + desc.min());
        if (desc.max() >= 1 && matches > desc.max()) throw new IllegalArgumentException("Descriptor '" + desc.value() + "' matched " + matches + " times but allows at most " + desc.max());
    }

}
