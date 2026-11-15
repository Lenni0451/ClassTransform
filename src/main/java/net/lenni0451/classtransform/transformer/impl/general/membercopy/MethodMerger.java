package net.lenni0451.classtransform.transformer.impl.general.membercopy;

import net.lenni0451.classtransform.annotations.injection.CASM;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Merges the methods of a transformer class into the transformed class.
 * <p>
 * Methods which are not copied may still be injected into existing methods with annotations such as {@link net.lenni0451.classtransform.annotations.injection.COverride} or {@link net.lenni0451.classtransform.annotations.injection.CASM}.
 */
public class MethodMerger {

    /**
     * Copies all methods of the transformer to the transformed class.
     * <p>
     * Constructors, the static initializer and {@link CASM} methods are skipped. A remaining method whose name and descriptor already exist in the transformed class is a conflict and has to be handled by another injection instead.
     *
     * @param transformedClass The class to copy the methods to
     * @param transformer      The transformer class to copy the methods from
     * @throws IllegalStateException If a method with the same name and descriptor already exists in the transformed class
     */
    public static void mergeMethods(final ClassNode transformedClass, final ClassNode transformer) {
        for (MethodNode method : transformer.methods) {
            if (method.name.startsWith("<")) continue;
            if (AnnotationUtils.hasAnnotation(method, CASM.class)) continue; //Special case for CASM bottom handler
            if (ASMUtils.hasMethod(transformedClass, method.name, method.desc)) {
                throw new IllegalStateException("Method '" + method.name + method.desc + "' from transformer '" + transformer.name + "' already exists in class '" + transformedClass.name + "' and does not override it");
            }
            Remapper.remapAndAdd(transformer, transformedClass, method);
        }
    }

}
