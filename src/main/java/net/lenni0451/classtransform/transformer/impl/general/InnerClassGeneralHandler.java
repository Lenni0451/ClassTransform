package net.lenni0451.classtransform.transformer.impl.general;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CShadow;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;

/**
 * Remap and make transformer inner classes accessible from the transformed classes.
 */
@ParametersAreNonnullByDefault
public class InnerClassGeneralHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassNode injectedClass, ClassNode transformer) {
        final ClassNode fInjectedClass = ASMUtils.cloneClass(injectedClass);
        final ClassNode fTransformer = ASMUtils.cloneClass(transformer);
        boolean hasInnerClasses = false;
        for (InnerClassNode innerClass : transformer.innerClasses) {
            if (innerClass.outerName != null) continue;
            hasInnerClasses = true;

            transformerManager.addRawTransformer(dot(innerClass.name), (tm, transformedClass) -> {
                for (FieldNode field : transformedClass.fields) field.access = ASMUtils.setAccess(field.access, Opcodes.ACC_PUBLIC);
                for (MethodNode method : transformedClass.methods) method.access = ASMUtils.setAccess(method.access, Opcodes.ACC_PUBLIC);
                transformedClass.access = ASMUtils.setAccess(transformedClass.access, Opcodes.ACC_PUBLIC);
                transformedClass.outerClass = null;

                MapRemapper remapper = new MapRemapper();
                remapper.addClassMapping(fTransformer.name, fInjectedClass.name);
                SyntheticMethodGeneralHandler.fillSyntheticMappings(fTransformer, fInjectedClass, remapper);
                return Remapper.remap(transformedClass, remapper);
            });
        }
        if (hasInnerClasses) {
            this.makeFieldsPublic(transformer);
            this.makeMethodsPublic(transformer);
        }
    }

    private void makeFieldsPublic(final ClassNode transformer) {
        for (FieldNode field : transformer.fields) {
            field.access = ASMUtils.setAccess(field.access, Opcodes.ACC_PUBLIC);
            AnnotationUtils.findInvisibleAnnotation(field, CShadow.class).ifPresent(annotation -> {
                Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
                values.put("makePublic", true);
                annotation.values = AnnotationParser.mapToList(values);
            });
        }
    }

    private void makeMethodsPublic(final ClassNode transformer) {
        for (MethodNode method : transformer.methods) {
            if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
                method.access = ASMUtils.setAccess(method.access, Opcodes.ACC_PUBLIC);
                method.access &= ~Opcodes.ACC_BRIDGE;
                AnnotationUtils.findInvisibleAnnotation(method, CShadow.class).ifPresent(annotation -> {
                    Map<String, Object> values = AnnotationParser.listToMap(annotation.values);
                    values.put("makePublic", true);
                    annotation.values = AnnotationParser.mapToList(values);
                });
            }
        }
    }

}
