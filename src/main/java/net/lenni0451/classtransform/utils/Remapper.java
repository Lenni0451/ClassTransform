package net.lenni0451.classtransform.utils;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.FieldRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Remapper {

    /**
     * Remap a {@link MethodNode} using a {@link MethodRemapper}<br>
     * This is a wrapper for {@link #remapAndAdd(String, String, ClassNode, MethodNode)} using the {@param target} {@link ClassNode} as the new holder
     *
     * @param source     The owner of the {@link MethodNode}
     * @param target     The new owner of the remapped {@link MethodNode}
     * @param methodNode The {@link MethodNode} to remap
     */
    public static void remapAndAdd(final ClassNode source, final ClassNode target, final MethodNode methodNode) {
        remapAndAdd(source.name, target.name, target, methodNode);
    }

    /**
     * Remap a {@link FieldNode} using a {@link FieldRemapper}<br>
     * This is a wrapper for {@link #remapAndAdd(String, String, ClassNode, FieldNode)} using the {@param target} {@link ClassNode} as the new holder
     *
     * @param source    The owner of the {@link MethodNode}
     * @param target    The new owner of the remapped {@link MethodNode}
     * @param fieldNode The {@link MethodNode} to remap
     */
    public static void remapAndAdd(final ClassNode source, final ClassNode target, final FieldNode fieldNode) {
        remapAndAdd(source.name, target.name, target, fieldNode);
    }

    /**
     * Remap a {@link MethodNode} using a {@link MethodRemapper}
     *
     * @param sourceName The original name of the class
     * @param targetName The new name of the class
     * @param holder     The {@link ClassNode} to which the new node gets added
     * @param methodNode The {@link MethodNode} to remap
     */
    public static void remapAndAdd(final String sourceName, final String targetName, final ClassNode holder, final MethodNode methodNode) {
        MapRemapper remapper = new MapRemapper(sourceName, targetName);
        MethodVisitor newNode = holder.visitMethod(methodNode.access, remapper.mapMethodName(sourceName, methodNode.name, methodNode.desc), remapper.mapDesc(methodNode.desc), methodNode.signature, methodNode.exceptions == null ? null : remapper.mapTypes(methodNode.exceptions.toArray(new String[0])));
        MethodRemapper methodRemapper = new MethodRemapper(newNode, remapper);
        methodNode.accept(methodRemapper);
    }

    /**
     * Remap a {@link FieldNode} using a {@link FieldRemapper}
     *
     * @param sourceName The original name of the class
     * @param targetName The new name of the class
     * @param holder     The {@link ClassNode} to which the new node gets added
     * @param fieldNode  The {@link FieldNode} to remap
     */
    public static void remapAndAdd(final String sourceName, final String targetName, final ClassNode holder, final FieldNode fieldNode) {
        MapRemapper remapper = new MapRemapper(sourceName, targetName);
        FieldVisitor newNode = holder.visitField(fieldNode.access, remapper.mapFieldName(sourceName, fieldNode.name, fieldNode.desc), remapper.mapDesc(fieldNode.desc), remapper.mapSignature(fieldNode.signature, true), fieldNode.value == null ? null : remapper.mapValue(fieldNode.value));
        FieldRemapper fieldRemapper = new FieldRemapper(newNode, remapper);
        if (fieldNode.visibleTypeAnnotations != null) {
            for (TypeAnnotationNode annotation : fieldNode.visibleTypeAnnotations) {
                annotation.accept(fieldRemapper.visitTypeAnnotation(annotation.typeRef, annotation.typePath, annotation.desc, true));
            }
        }
        if (fieldNode.invisibleTypeAnnotations != null) {
            for (TypeAnnotationNode annotation : fieldNode.invisibleTypeAnnotations) {
                annotation.accept(fieldRemapper.visitTypeAnnotation(annotation.typeRef, annotation.typePath, annotation.desc, false));
            }
        }
        if (fieldNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : fieldNode.visibleAnnotations) {
                annotation.accept(fieldRemapper.visitAnnotation(annotation.desc, true));
            }
        }
        if (fieldNode.invisibleAnnotations != null) {
            for (AnnotationNode annotation : fieldNode.invisibleAnnotations) {
                annotation.accept(fieldRemapper.visitAnnotation(annotation.desc, false));
            }
        }
    }

    /**
     * Remap a {@link ClassNode} using a {@link ClassRemapper}
     *
     * @param sourceName The original name of the class
     * @param targetName The new name of the class
     * @param node       The {@link ClassNode} to remap
     * @return The remapped {@link ClassNode}
     */
    public static ClassNode remap(final String sourceName, final String targetName, final ClassNode node) {
        return remap(node, new MapRemapper(sourceName, targetName));
    }

    /**
     * Remap a {@link ClassNode} using a {@link ClassRemapper}
     *
     * @param node     The {@link ClassNode} to remap
     * @param remapper The {@link MapRemapper} to use
     * @return The remapped {@link ClassNode}
     */
    public static ClassNode remap(final ClassNode node, final MapRemapper remapper) {
        ClassNode remappedNode = new ClassNode();
        ClassRemapper classRemapper = new ClassRemapper(remappedNode, remapper);
        node.accept(classRemapper);
        return remappedNode;
    }

    /**
     * Merge one {@link ClassNode} into another overwriting all members<br>
     * Use if you can't return a new {@link ClassNode} but still want to remap it
     *
     * @param original The original {@link ClassNode}
     * @param toMerge  The {@link ClassNode} to merge into the original
     */
    public static void merge(final ClassNode original, final ClassNode toMerge) {
        for (Field field : ClassNode.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (Modifier.isFinal(field.getModifiers())) continue;
            if (!Modifier.isPublic(field.getModifiers())) continue;

            try {
                field.set(original, field.get(toMerge));
            } catch (Throwable t) {
                throw new RuntimeException("Failed to merge class nodes", t);
            }
        }
    }

}
