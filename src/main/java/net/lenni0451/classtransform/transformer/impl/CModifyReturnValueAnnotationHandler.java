package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyReturnValue;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CoerceUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.InjectionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * Applies the {@link CModifyReturnValue} annotation by calling the transformer method before each selected return instruction and returning its result instead.
 * <p>
 * The handler must accept and return the target method return type, so void targets and non-return instructions are rejected.
 */
@ParametersAreNonnullByDefault
public class CModifyReturnValueAnnotationHandler extends InjectionAnnotationHandler<CModifyReturnValue> {

    /**
     * Creates a handler for the {@link CModifyReturnValue} annotation.
     */
    public CModifyReturnValueAnnotationHandler() {
        super(CModifyReturnValue.class, CModifyReturnValue::method);
    }

    @Override
    public void transform(CModifyReturnValue annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }

        Type returnType = returnType(target);
        if (returnType.equals(Type.VOID_TYPE)) throw new TransformerException(transformerMethod, transformer, "target method returns void");
        int capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, target, returnType);
        Type handlerReturnType = returnType(transformerMethod);
        if (!CoerceUtils.canCoerceReturn(returnType, handlerReturnType, CoerceUtils.hasCoerceReturn(transformerMethod))) {
            throw new TransformerException(transformerMethod, transformer, "must accept and return the target method return type")
                    .help(Codifier.of(transformerMethod).returnType(returnType).params(null, returnType));
        }

        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        Set<AbstractInsnNode> instructions = new LinkedHashSet<>();
        for (CTarget returnTarget : annotation.target()) {
            IInjectionTarget injectionTarget = injectionTargets.get(returnTarget.value().toUpperCase(Locale.ROOT));
            if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, returnTarget.value(), injectionTargets.keySet());
            List<AbstractInsnNode> targets = InjectionPlan.select(transformerManager, target, returnTarget, annotation.slice());
            if (targets == null) throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, returnTarget.target());
            for (AbstractInsnNode instruction : targets) {
                if (instruction.getOpcode() != returnType.getOpcode(Opcodes.IRETURN)) {
                    throw new TransformerException(transformerMethod, transformer, "target must be a return instruction");
                }
                instructions.add(instruction);
            }
        }

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyReturnValue");
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (AbstractInsnNode instruction : instructions) {
            InsnList insns = new InsnList();
            boolean isStatic = Modifier.isStatic(target.access);
            boolean isInterface = Modifier.isInterface(transformedClass.access);
            if (!isStatic) {
                insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insns.add(ASMUtils.swap(returnType, T_Object));
            }
            int opcode = isStatic ? Opcodes.INVOKESTATIC : isInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL;
            insns.add(InjectionUtils.loadCapturedArguments(target, capturedArguments));
            MethodInsnNode call = new MethodInsnNode(opcode, transformedClass.name, copiedTransformerMethod.name, copiedTransformerMethod.desc, isInterface);
            insns.add(call);
            transformerMethodCalls.add(call);
            if (!returnType.equals(handlerReturnType) && returnType.getSort() >= Type.ARRAY) insns.add(ASMUtils.getCast(returnType));
            target.instructions.insertBefore(instruction, insns);
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

}
