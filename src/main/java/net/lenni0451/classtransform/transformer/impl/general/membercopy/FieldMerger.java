package net.lenni0451.classtransform.transformer.impl.general.membercopy;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Merges the fields of a transformer class into the transformed class.
 * <p>
 * Copied fields are remapped to the transformed class, so references to the transformer class in their signature or value become references to the transformed class.
 */
public class FieldMerger {

    /**
     * Copies all fields of the transformer to the transformed class.
     * <p>
     * A field with the same name and descriptor as an existing field in the transformed class is a conflict.
     *
     * @param transformedClass The class to copy the fields to
     * @param transformer      The transformer class to copy the fields from
     * @throws IllegalStateException If a field with the same name and descriptor already exists in the transformed class
     */
    public static void mergeFields(final ClassNode transformedClass, final ClassNode transformer) {
        for (FieldNode field : transformer.fields) {
            if (ASMUtils.hasField(transformedClass, field.name, field.desc)) {
                throw new IllegalStateException("Field '" + field.name + field.desc + "' from transformer '" + transformer.name + "' already exists in class '" + transformedClass.name + "'");
            }
            Remapper.remapAndAdd(transformer, transformedClass, field);
        }
    }

}
