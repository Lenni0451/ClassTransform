package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CDescs;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

/**
 * Resolve {@link CDesc} references by id from the transformer method or class.
 */
@ParametersAreNonnullByDefault
public class CDescResolver {

    /**
     * Resolve a descriptor reference by id.
     * <p>
     * The transformer method annotations are searched before the transformer class annotations. A descriptor with an
     * inline value or without an id is returned unchanged.
     *
     * @param desc              The descriptor, either inline or referencing an id
     * @param manager           The transformer manager
     * @param transformer       The transformer class
     * @param transformerMethod The transformer method
     * @return The resolved descriptor
     * @throws IllegalArgumentException If no descriptor with the id exists or the resolved descriptor has no value
     */
    public static CDesc resolve(final CDesc desc, final TransformerManager manager, final ClassNode transformer, final MethodNode transformerMethod) {
        if (!desc.value().isEmpty() || desc.id().isEmpty()) return desc;
        CDesc resolved = find(manager, desc.id(), transformerMethod);
        if (resolved == null) resolved = find(manager, desc.id(), transformer);
        if (resolved == null) throw new IllegalArgumentException("Unknown descriptor id '" + desc.id() + "'");
        if (resolved.value().isEmpty()) throw new IllegalArgumentException("Descriptor '" + desc.id() + "' does not specify a value");
        return resolved;
    }

    /**
     * Resolve all descriptor references by id.
     *
     * @param descs             The descriptors, either inline or referencing an id
     * @param manager           The transformer manager
     * @param transformer       The transformer class
     * @param transformerMethod The transformer method
     * @return The resolved descriptors
     * @throws IllegalArgumentException If a descriptor id is unknown or has no value
     */
    public static CDesc[] resolve(final CDesc[] descs, final TransformerManager manager, final ClassNode transformer, final MethodNode transformerMethod) {
        CDesc[] resolved = new CDesc[descs.length];
        for (int i = 0; i < descs.length; i++) resolved[i] = resolve(descs[i], manager, transformer, transformerMethod);
        return resolved;
    }

    /**
     * Resolve the descriptor reference of a target.
     *
     * @param target            The target
     * @param manager           The transformer manager
     * @param transformer       The transformer class
     * @param transformerMethod The transformer method
     * @return The target with the resolved descriptor, or the given target if its descriptor does not reference an id
     * @throws IllegalArgumentException If the descriptor id is unknown or has no value
     */
    public static CTarget resolve(final CTarget target, final TransformerManager manager, final ClassNode transformer, final MethodNode transformerMethod) {
        CDesc desc = target.desc();
        CDesc resolved = resolve(desc, manager, transformer, transformerMethod);
        return resolved == desc ? target : CTargetImpl.withDesc(target, resolved);
    }

    @Nullable
    private static CDesc find(final TransformerManager manager, final String id, final MethodNode method) {
        CDesc found = find(manager, id, method.visibleAnnotations);
        if (found == null) found = find(manager, id, method.invisibleAnnotations);
        return found;
    }

    @Nullable
    private static CDesc find(final TransformerManager manager, final String id, final ClassNode transformer) {
        CDesc found = find(manager, id, transformer.visibleAnnotations);
        if (found == null) found = find(manager, id, transformer.invisibleAnnotations);
        return found;
    }

    @Nullable
    private static CDesc find(final TransformerManager manager, final String id, @Nullable final List<AnnotationNode> annotations) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(Type.getDescriptor(CDesc.class))) {
                CDesc desc = parse(manager, annotation);
                if (id.equals(desc.id())) return desc;
            } else if (annotation.desc.equals(Type.getDescriptor(CDescs.class))) {
                Object value = AnnotationUtils.listToMap(annotation.values).get("value");
                if (value instanceof List) {
                    for (Object entry : (List<?>) value) {
                        if (!(entry instanceof AnnotationNode)) continue;
                        CDesc desc = parse(manager, (AnnotationNode) entry);
                        if (id.equals(desc.id())) return desc;
                    }
                }
            }
        }
        return null;
    }

    private static CDesc parse(final TransformerManager manager, final AnnotationNode annotation) {
        Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
        return AnnotationParser.parse(CDesc.class, manager, values);
    }

}
