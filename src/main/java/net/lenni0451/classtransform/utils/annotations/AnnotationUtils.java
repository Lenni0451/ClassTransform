package net.lenni0451.classtransform.utils.annotations;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

import static net.lenni0451.classtransform.utils.Types.typeDescriptor;

@ParametersAreNonnullByDefault
public class AnnotationUtils {

    /**
     * Convert a list of key-value pairs to a map.<br>
     * The list must be in the format {@code [key, value, key, value, ...]}.
     *
     * @param list The list to convert
     * @return The converted map
     * @throws IndexOutOfBoundsException If the size of the list is not even
     * @throws ClassCastException        If the key is not a string
     */
    public static Map<String, Object> listToMap(@Nullable final List<Object> list) {
        Map<String, Object> map = new HashMap<>();
        if (list != null) {
            for (int i = 0; i < list.size(); i += 2) map.put((String) list.get(i), list.get(i + 1));
        }
        return map;
    }

    /**
     * Convert a map to a list of key-value pairs.<br>
     * The list will be in the format {@code [key, value, key, value, ...]}.
     *
     * @param map The map to convert
     * @return The converted list
     */
    public static List<Object> mapToList(@Nullable final Map<String, Object> map) {
        List<Object> list = new ArrayList<>();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue());
            }
        }
        return list;
    }

    /**
     * Clone an annotation node and all its values.
     *
     * @param node The annotation node to clone
     * @return The cloned annotation node
     */
    public static AnnotationNode clone(final AnnotationNode node) {
        AnnotationNode clone = new AnnotationNode(node.desc);
        if (node.values != null) {
            clone.values = new ArrayList<>(node.values);
            List<Object> values = clone.values;
            values.replaceAll(AnnotationUtils::cloneObject);
        }
        return clone;
    }

    private static Object cloneObject(final Object object) {
        if (object == null) return null;
        if (object instanceof AnnotationNode) {
            return clone((AnnotationNode) object);
        } else if (object.getClass().isArray()) {
            Object clone = Array.newInstance(object.getClass().getComponentType(), Array.getLength(object));
            for (int i = 0; i < Array.getLength(object); i++) Array.set(clone, i, cloneObject(Array.get(object, i)));
            return clone;
        } else if (object instanceof List) {
            List<?> list = (List<?>) object;
            List<Object> clone = new ArrayList<>(list.size());
            for (Object value : list) clone.add(cloneObject(value));
            return clone;
        } else {
            return object;
        }
    }


    /**
     * Iterate through all visible annotations in a {@link ClassNode}.
     *
     * @param classNode          The class node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEachVisible(final ClassNode classNode, final Consumer<AnnotationNode> annotationConsumer) {
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) annotationConsumer.accept(annotation);
        }
    }

    /**
     * Iterate through all visible annotations in a {@link FieldNode}.
     *
     * @param fieldNode          The field node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEachVisible(final FieldNode fieldNode, final Consumer<AnnotationNode> annotationConsumer) {
        if (fieldNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : fieldNode.visibleAnnotations) annotationConsumer.accept(annotation);
        }
    }

    /**
     * Iterate through all visible annotations in a {@link MethodNode}.
     *
     * @param methodNode         The method node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEachVisible(final MethodNode methodNode, final Consumer<AnnotationNode> annotationConsumer) {
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.visibleAnnotations) annotationConsumer.accept(annotation);
        }
    }

    /**
     * Iterate through all invisible annotations in a {@link ClassNode}.
     *
     * @param classNode          The class node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEachInvisible(final ClassNode classNode, final Consumer<AnnotationNode> annotationConsumer) {
        if (classNode.invisibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.invisibleAnnotations) annotationConsumer.accept(annotation);
        }
    }

    /**
     * Iterate through all invisible annotations in a {@link FieldNode}.
     *
     * @param fieldNode          The field node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEachInvisible(final FieldNode fieldNode, final Consumer<AnnotationNode> annotationConsumer) {
        if (fieldNode.invisibleAnnotations != null) {
            for (AnnotationNode annotation : fieldNode.invisibleAnnotations) annotationConsumer.accept(annotation);
        }
    }

    /**
     * Iterate through all invisible annotations in a {@link MethodNode}.
     *
     * @param methodNode         The method node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEachInvisible(final MethodNode methodNode, final Consumer<AnnotationNode> annotationConsumer) {
        if (methodNode.invisibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.invisibleAnnotations) annotationConsumer.accept(annotation);
        }
    }

    /**
     * Iterate through all annotations in a {@link ClassNode}.
     *
     * @param classNode          The class node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEach(final ClassNode classNode, final Consumer<AnnotationNode> annotationConsumer) {
        forEachVisible(classNode, annotationConsumer);
        forEachInvisible(classNode, annotationConsumer);
    }

    /**
     * Iterate through all annotations in a {@link FieldNode}.
     *
     * @param fieldNode          The field node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEach(final FieldNode fieldNode, final Consumer<AnnotationNode> annotationConsumer) {
        forEachVisible(fieldNode, annotationConsumer);
        forEachInvisible(fieldNode, annotationConsumer);
    }

    /**
     * Iterate through all annotations in a {@link MethodNode}.
     *
     * @param methodNode         The method node to iterate through
     * @param annotationConsumer The consumer to consume the annotations
     */
    public static void forEach(final MethodNode methodNode, final Consumer<AnnotationNode> annotationConsumer) {
        forEachVisible(methodNode, annotationConsumer);
        forEachInvisible(methodNode, annotationConsumer);
    }


    /**
     * Find a visible annotation in a {@link ClassNode}.
     *
     * @param classNode       The class node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findVisibleAnnotation(final ClassNode classNode, final Class<?> annotationClass) {
        return findAnnotation(classNode.visibleAnnotations, annotationClass);
    }

    /**
     * Find a visible annotation in a {@link ClassNode}.
     *
     * @param classNode            The class node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findVisibleAnnotation(final ClassNode classNode, final String annotationDescriptor) {
        return findAnnotation(classNode.visibleAnnotations, annotationDescriptor);
    }

    /**
     * Find a visible annotation in a {@link FieldNode}.
     *
     * @param fieldNode       The field node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findVisibleAnnotation(final FieldNode fieldNode, final Class<?> annotationClass) {
        return findAnnotation(fieldNode.visibleAnnotations, annotationClass);
    }

    /**
     * Find a visible annotation in a {@link FieldNode}.
     *
     * @param fieldNode            The field node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findVisibleAnnotation(final FieldNode fieldNode, final String annotationDescriptor) {
        return findAnnotation(fieldNode.visibleAnnotations, annotationDescriptor);
    }

    /**
     * Find a visible annotation in a {@link MethodNode}.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findVisibleAnnotation(final MethodNode methodNode, final Class<?> annotationClass) {
        return findAnnotation(methodNode.visibleAnnotations, annotationClass);
    }

    /**
     * Find a visible annotation in a {@link MethodNode}.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findVisibleAnnotation(final MethodNode methodNode, final String annotationDescriptor) {
        return findAnnotation(methodNode.visibleAnnotations, annotationDescriptor);
    }

    /**
     * Find visible parameter annotations in a {@link MethodNode}.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findVisibleParameterAnnotations(final MethodNode methodNode, final Class<?> annotationClass) {
        return findParameterAnnotations(methodNode.visibleParameterAnnotations, annotationClass);
    }

    /**
     * Find visible parameter annotations in a {@link MethodNode}.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findVisibleParameterAnnotations(final MethodNode methodNode, final String annotationDescriptor) {
        return findParameterAnnotations(methodNode.visibleParameterAnnotations, annotationDescriptor);
    }


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
     * Find an invisible annotation in a {@link ClassNode}.
     *
     * @param classNode            The class node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findInvisibleAnnotation(final ClassNode classNode, final String annotationDescriptor) {
        return findAnnotation(classNode.invisibleAnnotations, annotationDescriptor);
    }

    /**
     * Find an invisible annotation in a {@link FieldNode}.
     *
     * @param fieldNode       The field node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findInvisibleAnnotation(final FieldNode fieldNode, final Class<?> annotationClass) {
        return findAnnotation(fieldNode.invisibleAnnotations, annotationClass);
    }

    /**
     * Find an invisible annotation in a {@link FieldNode}.
     *
     * @param fieldNode            The field node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findInvisibleAnnotation(final FieldNode fieldNode, final String annotationDescriptor) {
        return findAnnotation(fieldNode.invisibleAnnotations, annotationDescriptor);
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
     * Find an invisible annotation in a {@link MethodNode}.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findInvisibleAnnotation(final MethodNode methodNode, final String annotationDescriptor) {
        return findAnnotation(methodNode.invisibleAnnotations, annotationDescriptor);
    }

    /**
     * Find invisible parameter annotations in a {@link MethodNode}.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findInvisibleParameterAnnotations(final MethodNode methodNode, final Class<?> annotationClass) {
        return findParameterAnnotations(methodNode.invisibleParameterAnnotations, annotationClass);
    }

    /**
     * Find invisible parameter annotations in a {@link MethodNode}.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findInvisibleParameterAnnotations(final MethodNode methodNode, final String annotationDescriptor) {
        return findParameterAnnotations(methodNode.invisibleParameterAnnotations, annotationDescriptor);
    }


    /**
     * Find a visible or invisible annotation in a {@link ClassNode}.
     *
     * @param classNode       The class node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(final ClassNode classNode, final Class<?> annotationClass) {
        Optional<AnnotationNode> annotationNode = findVisibleAnnotation(classNode, annotationClass);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleAnnotation(classNode, annotationClass);
    }

    /**
     * Find a visible or invisible annotation in a {@link ClassNode}.
     *
     * @param classNode            The class node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(final ClassNode classNode, final String annotationDescriptor) {
        Optional<AnnotationNode> annotationNode = findVisibleAnnotation(classNode, annotationDescriptor);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleAnnotation(classNode, annotationDescriptor);
    }

    /**
     * Find a visible or invisible annotation in a {@link FieldNode}.
     *
     * @param fieldNode       The field node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(final FieldNode fieldNode, final Class<?> annotationClass) {
        Optional<AnnotationNode> annotationNode = findVisibleAnnotation(fieldNode, annotationClass);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleAnnotation(fieldNode, annotationClass);
    }

    /**
     * Find a visible or invisible annotation in a {@link FieldNode}.
     *
     * @param fieldNode            The field node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(final FieldNode fieldNode, final String annotationDescriptor) {
        Optional<AnnotationNode> annotationNode = findVisibleAnnotation(fieldNode, annotationDescriptor);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleAnnotation(fieldNode, annotationDescriptor);
    }

    /**
     * Find a visible or invisible annotation in a {@link MethodNode}.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(final MethodNode methodNode, final Class<?> annotationClass) {
        Optional<AnnotationNode> annotationNode = findVisibleAnnotation(methodNode, annotationClass);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleAnnotation(methodNode, annotationClass);
    }

    /**
     * Find a visible or invisible annotation in a {@link MethodNode}.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(final MethodNode methodNode, final String annotationDescriptor) {
        Optional<AnnotationNode> annotationNode = findVisibleAnnotation(methodNode, annotationDescriptor);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleAnnotation(methodNode, annotationDescriptor);
    }

    /**
     * Find visible or invisible parameter annotations in a {@link MethodNode}.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findParameterAnnotations(final MethodNode methodNode, final Class<?> annotationClass) {
        Optional<AnnotationNode[]> annotationNode = findVisibleParameterAnnotations(methodNode, annotationClass);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleParameterAnnotations(methodNode, annotationClass);
    }

    /**
     * Find visible or invisible parameter annotations in a {@link MethodNode}.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findParameterAnnotations(final MethodNode methodNode, final String annotationDescriptor) {
        Optional<AnnotationNode[]> annotationNode = findVisibleParameterAnnotations(methodNode, annotationDescriptor);
        if (annotationNode.isPresent()) return annotationNode;
        return findInvisibleParameterAnnotations(methodNode, annotationDescriptor);
    }

    /**
     * Find an annotation in a list of annotations.
     *
     * @param annotations     The list of annotations to search in
     * @param annotationClass The annotation class to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(@Nullable final List<AnnotationNode> annotations, final Class<?> annotationClass) {
        return findAnnotation(annotations, typeDescriptor(annotationClass));
    }

    /**
     * Find an annotation in a list of annotations.
     *
     * @param annotations          The list of annotations to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotation if found
     */
    public static Optional<AnnotationNode> findAnnotation(@Nullable final List<AnnotationNode> annotations, final String annotationDescriptor) {
        if (annotations == null) return Optional.empty();
        for (AnnotationNode annotation : annotations) {
            if (annotation.desc.equals(annotationDescriptor)) return Optional.of(annotation);
        }
        return Optional.empty();
    }

    /**
     * Find parameter annotations in a list of annotations.
     *
     * @param annotations     The list of annotations to search in
     * @param annotationClass The annotation class to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findParameterAnnotations(@Nullable final List<AnnotationNode>[] annotations, final Class<?> annotationClass) {
        return findParameterAnnotations(annotations, typeDescriptor(annotationClass));
    }

    /**
     * Find parameter annotations in a list of annotations.
     *
     * @param annotations          The list of annotations to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return The annotations if found
     */
    public static Optional<AnnotationNode[]> findParameterAnnotations(@Nullable final List<AnnotationNode>[] annotations, final String annotationDescriptor) {
        if (annotations == null) return Optional.empty();
        AnnotationNode[] annotationNodes = new AnnotationNode[annotations.length];
        boolean found = false;
        for (int i = 0; i < annotations.length; i++) {
            Optional<AnnotationNode> annotationNode = findAnnotation(annotations[i], annotationDescriptor);
            annotationNodes[i] = annotationNode.orElse(null);
            if (annotationNode.isPresent()) found = true;
        }
        if (!found) return Optional.empty();
        else return Optional.of(annotationNodes);
    }


    /**
     * Check if a {@link ClassNode} has a visible annotation.
     *
     * @param classNode       The class node to search in
     * @param annotationClass The annotation class to search for
     * @return If the class node has the annotation
     */
    public static boolean hasVisibleAnnotation(final ClassNode classNode, final Class<?> annotationClass) {
        return findVisibleAnnotation(classNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link ClassNode} has a visible annotation.
     *
     * @param classNode            The class node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the class node has the annotation
     */
    public static boolean hasVisibleAnnotation(final ClassNode classNode, final String annotationDescriptor) {
        return findVisibleAnnotation(classNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link FieldNode} has a visible annotation.
     *
     * @param fieldNode       The field node to search in
     * @param annotationClass The annotation class to search for
     * @return If the field node has the annotation
     */
    public static boolean hasVisibleAnnotation(final FieldNode fieldNode, final Class<?> annotationClass) {
        return findVisibleAnnotation(fieldNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link FieldNode} has a visible annotation.
     *
     * @param fieldNode            The field node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the field node has the annotation
     */
    public static boolean hasVisibleAnnotation(final FieldNode fieldNode, final String annotationDescriptor) {
        return findVisibleAnnotation(fieldNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has a visible annotation.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return If the method node has the annotation
     */
    public static boolean hasVisibleAnnotation(final MethodNode methodNode, final Class<?> annotationClass) {
        return findVisibleAnnotation(methodNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has a visible annotation.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the method node has the annotation
     */
    public static boolean hasVisibleAnnotation(final MethodNode methodNode, final String annotationDescriptor) {
        return findVisibleAnnotation(methodNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has visible parameter annotations.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return If the method node has the annotation
     */
    public static boolean hasVisibleParameterAnnotations(final MethodNode methodNode, final Class<?> annotationClass) {
        return findVisibleParameterAnnotations(methodNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has visible parameter annotations.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the method node has the annotation
     */
    public static boolean hasVisibleParameterAnnotations(final MethodNode methodNode, final String annotationDescriptor) {
        return findVisibleParameterAnnotations(methodNode, annotationDescriptor).isPresent();
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
     * Check if a {@link ClassNode} has an invisible annotation.
     *
     * @param classNode            The class node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the class node has the annotation
     */
    public static boolean hasInvisibleAnnotation(final ClassNode classNode, final String annotationDescriptor) {
        return findInvisibleAnnotation(classNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link FieldNode} has an invisible annotation.
     *
     * @param fieldNode       The field node to search in
     * @param annotationClass The annotation class to search for
     * @return If the field node has the annotation
     */
    public static boolean hasInvisibleAnnotation(final FieldNode fieldNode, final Class<?> annotationClass) {
        return findInvisibleAnnotation(fieldNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link FieldNode} has an invisible annotation.
     *
     * @param fieldNode            The field node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the field node has the annotation
     */
    public static boolean hasInvisibleAnnotation(final FieldNode fieldNode, final String annotationDescriptor) {
        return findInvisibleAnnotation(fieldNode, annotationDescriptor).isPresent();
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
     * Check if a {@link MethodNode} has an invisible annotation.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the method node has the annotation
     */
    public static boolean hasInvisibleAnnotation(final MethodNode methodNode, final String annotationDescriptor) {
        return findInvisibleAnnotation(methodNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has invisible parameter annotations.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return If the method node has the annotation
     */
    public static boolean hasInvisibleParameterAnnotations(final MethodNode methodNode, final Class<?> annotationClass) {
        return findInvisibleParameterAnnotations(methodNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has invisible parameter annotations.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the method node has the annotation
     */
    public static boolean hasInvisibleParameterAnnotations(final MethodNode methodNode, final String annotationDescriptor) {
        return findInvisibleParameterAnnotations(methodNode, annotationDescriptor).isPresent();
    }


    /**
     * Check if a {@link ClassNode} has a visible or invisible annotation.
     *
     * @param classNode       The class node to search in
     * @param annotationClass The annotation class to search for
     * @return If the class node has the annotation
     */
    public static boolean hasAnnotation(final ClassNode classNode, final Class<?> annotationClass) {
        return findAnnotation(classNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link ClassNode} has a visible or invisible annotation.
     *
     * @param classNode            The class node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the class node has the annotation
     */
    public static boolean hasAnnotation(final ClassNode classNode, final String annotationDescriptor) {
        return findAnnotation(classNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link FieldNode} has a visible or invisible annotation.
     *
     * @param fieldNode       The field node to search in
     * @param annotationClass The annotation class to search for
     * @return If the field node has the annotation
     */
    public static boolean hasAnnotation(final FieldNode fieldNode, final Class<?> annotationClass) {
        return findAnnotation(fieldNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link FieldNode} has a visible or invisible annotation.
     *
     * @param fieldNode            The field node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the field node has the annotation
     */
    public static boolean hasAnnotation(final FieldNode fieldNode, final String annotationDescriptor) {
        return findAnnotation(fieldNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has a visible or invisible annotation.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return If the method node has the annotation
     */
    public static boolean hasAnnotation(final MethodNode methodNode, final Class<?> annotationClass) {
        return findAnnotation(methodNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has a visible or invisible annotation.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the method node has the annotation
     */
    public static boolean hasAnnotation(final MethodNode methodNode, final String annotationDescriptor) {
        return findAnnotation(methodNode, annotationDescriptor).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has visible or invisible parameter annotations.
     *
     * @param methodNode      The method node to search in
     * @param annotationClass The annotation class to search for
     * @return If the method node has the annotation
     */
    public static boolean hasParameterAnnotations(final MethodNode methodNode, final Class<?> annotationClass) {
        return findParameterAnnotations(methodNode, annotationClass).isPresent();
    }

    /**
     * Check if a {@link MethodNode} has visible or invisible parameter annotations.
     *
     * @param methodNode           The method node to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the method node has the annotation
     */
    public static boolean hasParameterAnnotations(final MethodNode methodNode, final String annotationDescriptor) {
        return findParameterAnnotations(methodNode, annotationDescriptor).isPresent();
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

    /**
     * Check if a list of annotations has an annotation.
     *
     * @param annotations          The list of annotations to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the list of annotations has the annotation
     */
    public static boolean hasAnnotation(@Nullable final List<AnnotationNode> annotations, final String annotationDescriptor) {
        return findAnnotation(annotations, annotationDescriptor).isPresent();
    }

    /**
     * Check if an array of parameter annotations has an annotation.
     *
     * @param annotations     The array of parameter annotations to search in
     * @param annotationClass The annotation class to search for
     * @return If the array of parameter annotations has the annotation
     */
    public static boolean hasParameterAnnotations(@Nullable final List<AnnotationNode>[] annotations, final Class<?> annotationClass) {
        return findParameterAnnotations(annotations, annotationClass).isPresent();
    }

    /**
     * Check if an array of parameter annotations has an annotation.
     *
     * @param annotations          The array of parameter annotations to search in
     * @param annotationDescriptor The descriptor of the annotation to search for
     * @return If the array of parameter annotations has the annotation
     */
    public static boolean hasParameterAnnotations(@Nullable final List<AnnotationNode>[] annotations, final String annotationDescriptor) {
        return findParameterAnnotations(annotations, annotationDescriptor).isPresent();
    }

}
