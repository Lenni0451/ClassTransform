package net.lenni0451.classtransform.transformer.impl.general;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Rename synthetic methods in transformers to avoid conflicts with the injected class.
 */
@ParametersAreNonnullByDefault
public class SyntheticMethodGeneralHandler extends AnnotationHandler {

    static void fillSyntheticMappings(final ClassNode source, final ClassNode target, final MapRemapper remapper) {
        for (MethodNode method : source.methods) {
            if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) continue;
            String newName = getUniqueName(target, source, method);
            remapper.addMethodMapping(source.name, method.name, method.desc, newName);
        }
    }

    private static String getUniqueName(final ClassNode target, final ClassNode owner, final MethodNode method) {
        int id = 0;
        String current;
        do {
            current = method.name + "$" + getSimpleName(owner) + id;
        } while (ASMUtils.getMethod(target, current, method.desc) != null);
        return current;
    }

    private static String getSimpleName(final ClassNode node) {
        int index = node.name.lastIndexOf('/');
        return index == -1 ? node.name : node.name.substring(index + 1);
    }


    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        MapRemapper remapper = new MapRemapper();
        fillSyntheticMappings(transformer, transformedClass, remapper);
        ClassNode remapped = Remapper.remap(transformer, remapper);
        Remapper.merge(transformer, remapped);
    }

}
