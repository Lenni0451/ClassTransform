package net.lenni0451.classtransform.utils.annotations;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;

@ParametersAreNonnullByDefault
public class AnnotationUtils {

    /**
     * Find an invisible annotation in a {@link ClassNode}.
     *
     * @param classNode       The class node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findInvisibleAnnotation(final ClassNode classNode, final Class<?> annotationClass) {
        return findAnnotation(classNode.invisibleAnnotations, annotationClass);
    }

    /**
     * Find an invisible annotation in a {@link MethodNode}.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findInvisibleAnnotation(final MethodNode methodNode, final Class<?> annotationClass) {
        return findAnnotation(methodNode.invisibleAnnotations, annotationClass);
    }

    /**
     * Find an annotation in a list of annotations.
     *
     * @param annotations     The list of annotations to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(@Nullable final List<AnnotationNode> annotations, final Class<?> annotationClass) {
        if (annotations == null) return Optional.empty();
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(typeDescriptor(annotationClass))) return Optional.of(annotation);
        }
        return Optional.empty();
    }

    /**
     * Check if a {@link ClassNode} has an invisible annotation.
     *
     * @param classNode       The class node to search in
     * @param annotationClass The annotation class to search for
     * @return If the class node has the annotation
     */
    public static boolean hasInvisibleAnnotation(final ClassNode classNode, final Class<?> annotationClass) {
        return findInvisibleAnnotation(classNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has an invisible annotation.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return If the method node has the annotation
     */
    public static boolean hasInvisibleAnnotation(final MethodNode methodNode, final Class<?> annotationClass) {
        return findInvisibleAnnotation(methodNode, annotationClass).isPresent();
    }

    /**
     * Check if a list of annotations has an annotation.
     *
     * @param annotations     The list of annotations to search in
     * @param annotationClass The annotation class to search for
     * @return If the list of annotations has the annotation
     */
    public static boolean hasAnnotation(@Nullable final List<AnnotationNode> annotations, final Class<?> annotationClass) {
        return findAnnotation(annotations, annotationClass).isPresent();
    }

}
