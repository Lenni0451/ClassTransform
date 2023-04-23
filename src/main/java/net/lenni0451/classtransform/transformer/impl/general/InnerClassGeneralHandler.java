package net.lenni0451.classtransform.transformer.impl.general;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;

/**
 * Remap and make transformer inner classes accessible from the transformed classes.
 */
public class InnerClassGeneralHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassTree classTree, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode injectedClass, ClassNode transformer) {
        final ClassNode fInjectedClass = ASMUtils.cloneClass(injectedClass);
        final ClassNode fTransformer = ASMUtils.cloneClass(transformer);
        boolean hasInnerClasses = false;
        for (InnerClassNode innerClass : transformer.innerClasses) {
            if (innerClass.outerName != null) continue;
            hasInnerClasses = true;

            transformerManager.addRawTransformer(dot(innerClass.name), (tm, transformedClass) -> {
                for (MethodNode method : transformedClass.methods) method.access = ASMUtils.setAccess(method.access, Opcodes.ACC_PUBLIC);
                for (FieldNode field : transformedClass.fields) field.access = ASMUtils.setAccess(field.access, Opcodes.ACC_PUBLIC);
                transformedClass.access = ASMUtils.setAccess(transformedClass.access, Opcodes.ACC_PUBLIC);
                transformedClass.outerClass = null;

                MapRemapper remapper = new MapRemapper();
                remapper.addClassMapping(fTransformer.name, fInjectedClass.name);
                SyntheticMethodGeneralHandler.fillSyntheticMappings(fTransformer, fInjectedClass, remapper);
                return Remapper.remap(transformedClass, remapper);
            });
        }
        if (hasInnerClasses) {
            for (MethodNode method : transformer.methods) {
                if ((method.access & Opcodes.ACC_SYNTHETIC) != 0) {
                    method.access = ASMUtils.setAccess(method.access, Opcodes.ACC_PUBLIC);
                    method.access &= ~Opcodes.ACC_BRIDGE;
                }
            }
        }
    }

}
