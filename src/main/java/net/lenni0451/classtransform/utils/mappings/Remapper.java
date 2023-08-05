package net.lenni0451.classtransform.utils.mappings;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.FieldRemapper;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Util methods to remap classes, methods and fields.
 */
@ParametersAreNonnullByDefault
public class Remapper {

    /**
     * Remap a method node and add it to the target class.<br>
     * The original class/method will not be modified.
     *
     * @param source     The owner of the method node
     * @param target     The new owner of the remapped method node
     * @param methodNode The method node to remap
     */
    public static void remapAndAdd(final ClassNode source, final ClassNode target, final MethodNode methodNode) {
        remapAndAdd(source.name, target.name, target, methodNode);
    }

    /**
     * Remap a field node and add it to the target class.<br>
     * The original class/field will not be modified.
     *
     * @param source    The owner of the field node
     * @param target    The new owner of the remapped field node
     * @param fieldNode The field node to remap
     */
    public static void remapAndAdd(final ClassNode source, final ClassNode target, final FieldNode fieldNode) {
        remapAndAdd(source.name, target.name, target, fieldNode);
    }

    /**
     * Remap a method node and add it to the target class.<br>
     * The original class/method will not be modified.
     *
     * @param sourceName The original name of the class
     * @param targetName The new name of the class
     * @param holder     The class node to which the remapped method node gets added
     * @param methodNode The method node to remap
     */
    public static void remapAndAdd(final String sourceName, final String targetName, final ClassNode holder, final MethodNode methodNode) {
        MapRemapper remapper = new MapRemapper(sourceName, targetName);
        MethodVisitor newNode = holder.visitMethod(methodNode.access, remapper.mapMethodName(sourceName, methodNode.name, methodNode.desc), remapper.mapDesc(methodNode.desc), methodNode.signature, methodNode.exceptions == null ? null : remapper.mapTypes(methodNode.exceptions.toArray(new String[0])));
        MethodRemapper methodRemapper = new MethodRemapper(newNode, remapper);
        methodNode.accept(methodRemapper);
    }

    /**
     * Remap a field node and add it to the target class.<br>
     * The original class/field will not be modified.
     *
     * @param sourceName The original name of the class
     * @param targetName The new name of the class
     * @param holder     The class node to which the remapped field node gets added
     * @param fieldNode  The field node to remap
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
     * Remap a class node from the given name to the new name.<br>
     * The original class will not be modified.
     *
     * @param sourceName The original name of the class
     * @param targetName The new name of the class
     * @param node       The class node to remap
     * @return The remapped class node
     */
    public static ClassNode remap(final String sourceName, final String targetName, final ClassNode node) {
        return remap(node, new MapRemapper(sourceName, targetName));
    }

    /**
     * Remap a class node using a custom remapper.<br>
     * The original class will not be modified.
     *
     * @param node     The class node to remap
     * @param remapper The remapper to use
     * @return The remapped class node
     */
    public static ClassNode remap(final ClassNode node, final MapRemapper remapper) {
        ClassNode remappedNode = new ClassNode();
        ClassRemapper classRemapper = new ClassRemapper(remappedNode, remapper);
        node.accept(classRemapper);
        return remappedNode;
    }

    /**
     * Merge one class node into another overwriting all members.<br>
     * Use if you can't return a new class node but still want to remap it.<br>
     * This uses reflection to copy all non-static, mutable and public fields from the source to the target.<br>
     * The original class will not be modified.
     *
     * @param original The original class node
     * @param toMerge  The class node to merge into the original
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
