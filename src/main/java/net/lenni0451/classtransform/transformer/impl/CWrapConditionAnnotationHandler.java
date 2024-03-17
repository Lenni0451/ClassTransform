package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CWrapCondition;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.impl.wrapcondition.CWrapConditionField;
import net.lenni0451.classtransform.transformer.impl.wrapcondition.CWrapConditionMethod;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.returnType;

/**
 * The annotation handler for the {@link CWrapCondition} annotation.
 */
@ParametersAreNonnullByDefault
public class CWrapConditionAnnotationHandler extends RemovingTargetAnnotationHandler<CWrapCondition> {

    private final CWrapConditionField wrapConditionField = new CWrapConditionField();
    private final CWrapConditionMethod wrapConditionMethod = new CWrapConditionMethod();

    public CWrapConditionAnnotationHandler() {
        super(CWrapCondition.class, CWrapCondition::method);
    }

    @Override
    public void transform(CWrapCondition annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        if (!returnType(transformerMethod).equals(Type.BOOLEAN_TYPE)) throw new TransformerException(transformerMethod, transformer, "must return 'boolean'");

        MethodNode copiedTransformerMethod = null;
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (CTarget wrapTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(wrapTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, wrapTarget.target(), injectionTargets.keySet());
            List<AbstractInsnNode> targetInstructions = injectionTarget.getTargets(injectionTargets, target, wrapTarget, annotation.slice());
            if (targetInstructions.isEmpty()) throw new TransformerException(transformerMethod, transformer, "No valid method/field target found");

            if (copiedTransformerMethod == null) copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CWrapCondition");
            for (AbstractInsnNode targetInstruction : targetInstructions) {
                if (targetInstruction instanceof FieldInsnNode) {
                    if (targetInstruction.getOpcode() != Opcodes.PUTSTATIC && targetInstruction.getOpcode() != Opcodes.PUTFIELD) {
                        throw new TransformerException(transformerMethod, transformer, "target must be a field getter");
                    }
                    transformerMethodCalls.add(this.wrapConditionField.inject(transformedClass, transformer, copiedTransformerMethod, target, (FieldInsnNode) targetInstruction));
                } else if (targetInstruction instanceof MethodInsnNode) {
                    if (!returnType(((MethodInsnNode) targetInstruction).desc).equals(Type.VOID_TYPE)) {
                        throw new TransformerException(transformerMethod, transformer, "target method must have void as return type");
                    }
                    transformerMethodCalls.add(this.wrapConditionMethod.inject(transformedClass, transformer, copiedTransformerMethod, target, (MethodInsnNode) targetInstruction));
                } else {
                    throw new TransformerException(transformerMethod, transformer, "Target is not a valid method/field invocation");
                }
            }
        }
        if (copiedTransformerMethod == null) throw new TransformerException(transformerMethod, transformer, "No valid target found");
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
    }

}
