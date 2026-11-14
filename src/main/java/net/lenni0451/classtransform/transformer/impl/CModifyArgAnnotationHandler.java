package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyArg;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.CDescResolver;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.DescMatcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
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
 * The annotation handler for the {@link CModifyArg} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyArgAnnotationHandler extends InjectionAnnotationHandler<CModifyArg> {

    public CModifyArgAnnotationHandler() {
        super(CModifyArg.class, CModifyArg::method);
    }

    @Override
    protected Set<MethodNode> getTargetMethods(final CModifyArg annotation, final TransformerManager manager, final ClassNode target, final ClassNode transformer, final MethodNode method) {
        Set<MethodNode> targets = super.getTargetMethods(annotation, manager, target, transformer, method);
        CDesc[] descs = CDescResolver.resolve(annotation.desc(), manager, transformer, method);
        if (descs.length > 0) DescMatcher.addMethods(targets, target, descs);
        return targets;
    }

    @Override
    public void transform(final CModifyArg annotation, final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, MethodNode transformerMethod, final MethodNode target) {
        this.checkConstraints(transformerManager, annotation.constraints(), transformerMethod, transformer);
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        Type handlerReturnType = returnType(transformerMethod);
        Type[] handlerArguments = argumentTypes(transformerMethod);
        if (handlerReturnType.equals(Type.VOID_TYPE)) {
            throw new TransformerException(transformerMethod, transformer, "must return the modified argument")
                    .help(Codifier.of(transformerMethod).returnType(Type.INT_TYPE));
        }
        if (handlerArguments.length == 0) {
            throw new TransformerException(transformerMethod, transformer, "must accept the argument value or all invocation arguments");
        }

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyArg");
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
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
                int argumentIndex = this.findArgumentIndex(transformerMethod, transformer, invocationArguments, handlerReturnType, annotation.index());
                boolean singleArgument = handlerArguments.length == 1;
                if (singleArgument) {
                    if (!this.matches(handlerArguments[0], handlerReturnType) || !this.matches(handlerReturnType, handlerArguments[0])) {
                        throw new TransformerException(transformerMethod, transformer, "must accept and return the modified argument")
                                .help(Codifier.of(transformerMethod).param(handlerReturnType).returnType(handlerReturnType));
                    }
                } else if (!ASMUtils.compareTypes(invocationArguments, handlerArguments)) {
                    throw new TransformerException(transformerMethod, transformer, "must accept all invocation arguments")
                            .help(Codifier.of(transformerMethod).param(null).params(invocationArguments).returnType(invocationArguments[argumentIndex]));
                }
                if (!this.matches(invocationArguments[argumentIndex], handlerReturnType)) {
                    throw new TransformerException(transformerMethod, transformer, "must return the modified argument type")
                            .help(Codifier.of(transformerMethod).returnType(invocationArguments[argumentIndex]));
                }
                target.instructions.insertBefore(instruction, this.getModifyInstructions(transformedClass, target, transformerMethod, invocationArguments, argumentIndex, singleArgument, transformerMethodCalls));
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private int findArgumentIndex(final MethodNode transformerMethod, final ClassNode transformer, final Type[] arguments, final Type handlerReturnType, final int index) {
        if (index > -1) {
            if (index >= arguments.length) {
                throw new TransformerException(transformerMethod, transformer, "argument index " + index + " is out of bounds for " + Type.getMethodDescriptor(Type.VOID_TYPE, arguments));
            }
            if (!this.matches(arguments[index], handlerReturnType)) {
                throw new TransformerException(transformerMethod, transformer, "argument " + index + " has type " + arguments[index].getClassName() + " but the handler returns " + handlerReturnType.getClassName());
            }
            return index;
        }
        int found = -1;
        for (int i = 0; i < arguments.length; i++) {
            if (!arguments[i].equals(handlerReturnType)) continue;
            if (found != -1) {
                throw new TransformerException(transformerMethod, transformer, "found multiple arguments matching " + handlerReturnType.getClassName() + " at " + found + " and " + i)
                        .help("Specify the argument index");
            }
            found = i;
        }
        if (found == -1) {
            throw new TransformerException(transformerMethod, transformer, "could not find an argument with type " + handlerReturnType.getClassName())
                    .help("Specify the argument index");
        }
        return found;
    }

    private InsnList getModifyInstructions(final ClassNode transformedClass, final MethodNode targetMethod, final MethodNode transformerMethod, final Type[] invocationArguments, final int argumentIndex, final boolean singleArgument, final List<MethodInsnNode> transformerMethodCalls) {
        int[] indices = new int[invocationArguments.length];
        int nextIndex = ASMUtils.getFreeVarIndex(targetMethod);
        for (int i = 0; i < invocationArguments.length; i++) {
            indices[i] = nextIndex;
            nextIndex += invocationArguments[i].getSize();
        }

        InsnList instructions = new InsnList();
        for (int i = invocationArguments.length - 1; i >= 0; i--) {
            instructions.add(new VarInsnNode(ASMUtils.getStoreOpcode(invocationArguments[i]), indices[i]));
        }
        if (!Modifier.isStatic(transformerMethod.access)) instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (singleArgument) {
            instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(invocationArguments[argumentIndex]), indices[argumentIndex]));
        } else {
            for (int i = 0; i < invocationArguments.length; i++) {
                instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(invocationArguments[i]), indices[i]));
            }
        }

        MethodInsnNode transformerCall;
        if (Modifier.isStatic(transformerMethod.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        } else {
            transformerCall = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        }
        instructions.add(transformerCall);
        transformerMethodCalls.add(transformerCall);
        if (invocationArguments[argumentIndex].getSort() >= Type.ARRAY && !invocationArguments[argumentIndex].equals(returnType(transformerMethod))) {
            instructions.add(ASMUtils.getCast(invocationArguments[argumentIndex]));
        }
        instructions.add(new VarInsnNode(ASMUtils.getStoreOpcode(invocationArguments[argumentIndex]), indices[argumentIndex]));
        for (int i = 0; i < invocationArguments.length; i++) {
            instructions.add(new VarInsnNode(ASMUtils.getLoadOpcode(invocationArguments[i]), indices[i]));
        }
        return instructions;
    }

    private boolean matches(final Type source, final Type handler) {
        return ASMUtils.compareType(source, handler);
    }

}
