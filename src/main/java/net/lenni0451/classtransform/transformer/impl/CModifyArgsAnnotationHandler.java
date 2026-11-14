package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.CArgs;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyArgs;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CDescResolver;
import net.lenni0451.classtransform.utils.DescMatcher;
import net.lenni0451.classtransform.utils.InjectionUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CModifyArgs} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyArgsAnnotationHandler extends InjectionAnnotationHandler<CModifyArgs> {

    private static final String ARGS_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(Object[].class));

    public CModifyArgsAnnotationHandler() {
        super(CModifyArgs.class, CModifyArgs::method);
    }

    @Override
    protected Set<MethodNode> getTargetMethods(final CModifyArgs annotation, final TransformerManager manager, final ClassNode target, final ClassNode transformer, final MethodNode method) {
        Set<MethodNode> targets = super.getTargetMethods(annotation, manager, target, transformer, method);
        CDesc[] descs = CDescResolver.resolve(annotation.desc(), manager, transformer, method);
        if (descs.length > 0) DescMatcher.addMethods(targets, target, descs);
        return targets;
    }

    @Override
    public void transform(final CModifyArgs annotation, final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, MethodNode transformerMethod, final MethodNode target) {
        this.checkConstraints(transformerManager, annotation.constraints(), transformerMethod, transformer);
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (!returnType(transformerMethod).equals(Type.VOID_TYPE)) {
            throw TransformerException.mustReturnVoid(transformerMethod, transformer);
        }
        if (Modifier.isStatic(target.access) && !Modifier.isStatic(transformerMethod.access)) {
            throw new TransformerException(transformerMethod, transformer, "must be static when the target method is static");
        }
        int capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, target, type(CArgs.class));

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyArgs");
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        int delegateIndex = this.findDelegateInit(target);
        for (CTarget injectionTarget : this.getInjectionTargets(annotation, transformerManager, transformerMethod)) {
            IInjectionTarget targetImplementation = injectionTargets.get(injectionTarget.value().toUpperCase(Locale.ROOT));
            if (targetImplementation == null) {
                throw new InvalidTargetException(transformerMethod, transformer, injectionTarget.value(), injectionTargets.keySet());
            }
            List<AbstractInsnNode> selected = InjectionPlan.select(transformerManager, target, injectionTarget, annotation.slice());
            if (selected == null) {
                throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, injectionTarget.target());
            }
            for (AbstractInsnNode instruction : new LinkedHashSet<>(selected)) {
                if (!(instruction instanceof MethodInsnNode)) {
                    throw new TransformerException(transformerMethod, transformer, "target must be a method invocation");
                }
                MethodInsnNode invocation = (MethodInsnNode) instruction;
                Type[] invocationArguments = argumentTypes(invocation.desc);
                if (invocationArguments.length == 0) {
                    throw new TransformerException(transformerMethod, transformer, "target invocation has no arguments");
                }
                if (!Modifier.isStatic(transformerMethod.access) && delegateIndex >= 0 && target.instructions.indexOf(instruction) <= delegateIndex) {
                    throw new TransformerException(transformerMethod, transformer, "must be static when targeting a constructor before the super() or this() call");
                }
                target.instructions.insertBefore(instruction, this.getModifyInstructions(transformedClass, target, transformerMethod, invocation, invocationArguments, capturedArguments, transformerMethodCalls));
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private InsnList getModifyInstructions(final ClassNode transformedClass, final MethodNode targetMethod, final MethodNode transformerMethod, final MethodInsnNode invocation, final Type[] invocationArguments, final int capturedArguments, final List<MethodInsnNode> transformerMethodCalls) {
        int[] indices = new int[invocationArguments.length];
        int nextIndex = ASMUtils.getFreeVarIndex(targetMethod);
        for (int i = 0; i < invocationArguments.length; i++) {
            indices[i] = nextIndex;
            nextIndex += invocationArguments[i].getSize();
        }
        int argsIndex = nextIndex;

        InsnList instructions = new InsnList();
        for (int i = invocationArguments.length - 1; i >= 0; i--) {
            instructions.add(new VarInsnNode(ASMUtils.getStoreOpcode(invocationArguments[i]), indices[i]));
        }
        instructions.add(new TypeInsnNode(Opcodes.NEW, internalName(CArgs.class)));
        instructions.add(new InsnNode(Opcodes.DUP));
        instructions.add(new LdcInsnNode(invocation.desc));
        instructions.add(ASMUtils.intPush(invocationArguments.length));
        instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, internalName(Object.class)));
        for (int i = 0; i < invocationArguments.length; i++) {
            instructions.add(new InsnNode(Opcodes.DUP));
            instructions.add(ASMUtils.intPush(i));
            instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(invocationArguments[i]), indices[i]));
            AbstractInsnNode box = ASMUtils.getPrimitiveToObject(invocationArguments[i]);
            if (box != null) instructions.add(box);
            instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalName(CArgs.class), "<init>", ARGS_DESC, false));
        instructions.add(new VarInsnNode(Opcodes.ASTORE, argsIndex));

        if (!Modifier.isStatic(transformerMethod.access)) instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, argsIndex));
        instructions.add(InjectionUtils.loadCapturedArguments(targetMethod, capturedArguments));
        MethodInsnNode transformerCall;
        if (Modifier.isStatic(transformerMethod.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        } else {
            transformerCall = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        }
        instructions.add(transformerCall);
        transformerMethodCalls.add(transformerCall);

        for (int i = 0; i < invocationArguments.length; i++) {
            instructions.add(new VarInsnNode(Opcodes.ALOAD, argsIndex));
            instructions.add(ASMUtils.intPush(i));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, internalName(CArgs.class), "get", methodDescriptor(Object.class, int.class), false));
            instructions.add(ASMUtils.getCast(invocationArguments[i]));
        }
        return instructions;
    }

    private int findDelegateInit(final MethodNode target) {
        if (!"<init>".equals(target.name)) return -1;
        int allocations = 0;
        int index = 0;
        for (AbstractInsnNode instruction : target.instructions) {
            if (instruction.getOpcode() == Opcodes.NEW) {
                allocations++;
            } else if (instruction.getOpcode() == Opcodes.INVOKESPECIAL && instruction instanceof MethodInsnNode && "<init>".equals(((MethodInsnNode) instruction).name)) {
                if (allocations > 0) allocations--;
                else return index;
            }
            index++;
        }
        return -1;
    }

}
