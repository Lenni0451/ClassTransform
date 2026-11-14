package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CDesc;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
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
            for (MethodNode method : target.methods) {
                if (method.name.equals(desc.value()) && method.desc.equals(descriptor)) methods.add(method);
            }
        }
    }

}
