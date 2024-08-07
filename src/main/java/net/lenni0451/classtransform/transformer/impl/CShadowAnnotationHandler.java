package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.exceptions.FieldNotFoundException;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.List;

/**
 * The annotation handler for the {@link CShadow} annotation.
 */
@ParametersAreNonnullByDefault
public class CShadowAnnotationHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        MapRemapper remapper = new MapRemapper();
        this.checkFields(transformerManager, transformedClass, transformer, remapper);
        this.checkMethods(transformerManager, transformedClass, transformer, remapper);

        if (remapper.isEmpty()) return;
        ClassNode mappedNode = Remapper.remap(transformer, remapper);
        Remapper.merge(transformer, mappedNode);
    }

    private void checkFields(final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer, final MapRemapper remapper) {
        Iterator<FieldNode> it = transformer.fields.iterator();
        while (it.hasNext()) {
            FieldNode field = it.next();
            CShadow annotation = this.getAnnotation(CShadow.class, field, transformerManager);
            if (annotation == null) continue;
            it.remove();

            List<FieldNode> targets = ASMUtils.getFieldsFromCombi(target, annotation.value());
            if (targets.isEmpty()) throw new FieldNotFoundException(target, transformer, annotation.value());
            for (FieldNode targetField : targets) {
                if (annotation.makePublic()) targetField.access = ASMUtils.setAccess(targetField.access, Opcodes.ACC_PUBLIC);
                if (annotation.makeMutable()) targetField.access = ASMUtils.setModifier(targetField.access, Opcodes.ACC_FINAL, false);
                if (field.name.equals(targetField.name) && field.desc.equals(targetField.desc)) continue;
                remapper.addFieldMapping(transformer.name, field.name, field.desc, targetField.name);
            }
        }
    }

    private void checkMethods(final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer, final MapRemapper remapper) {
        Iterator<MethodNode> it = transformer.methods.iterator();
        while (it.hasNext()) {
            MethodNode method = it.next();
            CShadow annotation = this.getAnnotation(CShadow.class, method, transformerManager);
            if (annotation == null) continue;
            it.remove();

            List<MethodNode> targets = ASMUtils.getMethodsFromCombi(target, annotation.value());
            if (targets.isEmpty()) throw new MethodNotFoundException(target, transformer, annotation.value());
            for (MethodNode targetMethod : targets) {
                if (annotation.makePublic()) targetMethod.access = ASMUtils.setAccess(targetMethod.access, Opcodes.ACC_PUBLIC);
                if (annotation.makeMutable()) targetMethod.access = ASMUtils.setModifier(targetMethod.access, Opcodes.ACC_FINAL, false);
                if (method.name.equals(targetMethod.name) && method.desc.equals(targetMethod.desc)) continue;
                remapper.addMethodMapping(transformer.name, method.name, method.desc, targetMethod.name);
            }
        }
    }

}
