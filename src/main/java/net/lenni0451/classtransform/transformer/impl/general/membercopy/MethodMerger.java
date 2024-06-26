package net.lenni0451.classtransform.transformer.impl.general.membercopy;

import net.lenni0451.classtransform.annotations.injection.CASM;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class MethodMerger {

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
