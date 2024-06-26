package net.lenni0451.classtransform.transformer.impl.general.membercopy;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class FieldMerger {

    public static void mergeFields(final ClassNode transformedClass, final ClassNode transformer) {
        for (FieldNode field : transformer.fields) {
            if (ASMUtils.hasField(transformedClass, field.name, field.desc)) {
                throw new IllegalStateException("Field '" + field.name + field.desc + "' from transformer '" + transformer.name + "' already exists in class '" + transformedClass.name + "'");
            }
            Remapper.remapAndAdd(transformer, transformedClass, field);
        }
    }

}
