package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyVariable;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.coprocessor.impl.LocalVariableLookup;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.utils.CDescResolver;
import net.lenni0451.classtransform.utils.CTargetImpl;
import net.lenni0451.classtransform.utils.DescMatcher;
import net.lenni0451.classtransform.utils.InjectionUtils;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CModifyVariable} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyVariableAnnotationHandler extends InjectionAnnotationHandler<CModifyVariable> {

    private static final String LOAD = "LOAD";
    private static final String STORE = "STORE";
    private static final String ARG_ORDINAL = "modifyVariableOrdinal=";
    private static final String ARG_OPCODE = "modifyVariableOpcode=";

    public CModifyVariableAnnotationHandler() {
        super(CModifyVariable.class, CModifyVariable::method);
    }

    @Override
    protected Set<MethodNode> getTargetMethods(final CModifyVariable annotation, final TransformerManager manager, final ClassNode target, final ClassNode transformer, final MethodNode method) {
        Set<MethodNode> targets = super.getTargetMethods(annotation, manager, target, transformer, method);
        CDesc[] descs = CDescResolver.resolve(annotation.desc(), manager, transformer, method);
        if (descs.length > 0) DescMatcher.addMethods(targets, target, descs);
        return targets;
    }

    @Override
    protected List<CTarget> getInjectionTargets(final CModifyVariable annotation, final TransformerManager manager, final MethodNode method) {
        List<CTarget> configured = new ArrayList<>();
        if (!annotation.target().value().isEmpty()) configured.add(annotation.target());
        Collections.addAll(configured, annotation.targets());

        List<CTarget> targets = new ArrayList<>();
        for (CTarget target : configured) {
            String value = target.value().toUpperCase(Locale.ROOT);
            if (!value.equals(LOAD) && !value.equals(STORE)) {
                targets.add(target);
                continue;
            }
            List<String> args = new ArrayList<>(Arrays.asList(target.args()));
            args.add(ARG_ORDINAL + target.ordinal());
            args.add(ARG_OPCODE + target.opcode());
            targets.add(CTargetImpl.of(target.value(), target.target(), target.shift(), -1, target.optional(), args.toArray(new String[0]), target.desc()));
        }
        return targets;
    }

    @Override
    public void transform(final CModifyVariable annotation, final TransformerManager transformerManager, final ClassNode transformedClass, final ClassNode transformer, MethodNode transformerMethod, final MethodNode target) {
        this.checkConstraints(transformerManager, annotation.constraints(), transformerMethod, transformer);
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        Type valueType = returnType(transformerMethod);
        if (valueType.equals(Type.VOID_TYPE)) {
            throw new TransformerException(transformerMethod, transformer, "must return the modified value");
        }
        int capturedArguments = InjectionUtils.getCapturedArgumentCount(transformer, transformerMethod, target, valueType);

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyVariable");
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
            String value = injectionTarget.value().toUpperCase(Locale.ROOT);
            if (value.equals(LOAD) || value.equals(STORE)) {
                selected = this.selectVariableOperations(transformedClass, transformer, transformerMethod, target, selected, value.equals(LOAD), annotation, valueType, this.getIntArgument(injectionTarget, ARG_ORDINAL, injectionTarget.ordinal()), this.getIntArgument(injectionTarget, ARG_OPCODE, injectionTarget.opcode()));
            }
            for (AbstractInsnNode instruction : new LinkedHashSet<>(selected)) {
                int position = target.instructions.indexOf(instruction);
                if (annotation.print()) {
                    LocalVariableLookup.print(transformedClass, target, position, valueType, annotation.argsOnly());
                }
                LocalVariableLookup.Local local = this.findVariable(transformedClass, transformer, transformerMethod, target, position, annotation, valueType);
                if (local == null) continue;
                if (!local.getType().equals(valueType)) {
                    throw new TransformerException(transformerMethod, transformer, "local variable " + local.getIndex() + " has type " + local.getType().getClassName() + " but " + valueType.getClassName() + " is required");
                }
                target.instructions.insertBefore(instruction, this.getModifyInstructions(transformedClass, target, transformerMethod, local, capturedArguments, transformerMethodCalls));
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private List<AbstractInsnNode> selectVariableOperations(final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target, final List<AbstractInsnNode> selected, final boolean load, final CModifyVariable annotation, final Type valueType, final int ordinal, final int opcode) {
        int expectedOpcode = opcode != -1 ? opcode : valueType.getOpcode(load ? Opcodes.ILOAD : Opcodes.ISTORE);
        List<AbstractInsnNode> result = new ArrayList<>();
        int currentOrdinal = 0;
        for (AbstractInsnNode instruction : selected) {
            AbstractInsnNode operation = load ? instruction : instruction.getPrevious();
            if (!(operation instanceof VarInsnNode) || operation.getOpcode() != expectedOpcode) continue;
            int position = target.instructions.indexOf(instruction);
            LocalVariableLookup.Local local = this.findVariable(transformedClass, transformer, transformerMethod, target, position, annotation, valueType);
            if (local == null || local.getIndex() != ((VarInsnNode) operation).var) continue;
            if (ordinal == -1 || ordinal == currentOrdinal) result.add(instruction);
            currentOrdinal++;
        }
        return result;
    }

    private LocalVariableLookup.Local findVariable(final ClassNode transformedClass, final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target, final int position, final CModifyVariable annotation, final Type valueType) {
        boolean implicit = annotation.ordinal() < 0 && annotation.index() < 0 && annotation.name().length == 0;
        try {
            return LocalVariableLookup.findVariable(transformedClass, transformer, transformerMethod, target, position, annotation.name(), annotation.ordinal(), annotation.index(), annotation.argsOnly(), valueType);
        } catch (TransformerException e) {
            if (implicit) throw e;
            return null;
        }
    }

    private InsnList getModifyInstructions(final ClassNode transformedClass, final MethodNode targetMethod, final MethodNode transformerMethod, final LocalVariableLookup.Local local, final int capturedArguments, final List<MethodInsnNode> transformerMethodCalls) {
        InsnList instructions = new InsnList();
        if (!Modifier.isStatic(transformerMethod.access)) instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        instructions.add(new VarInsnNode(local.getType().getOpcode(Opcodes.ILOAD), local.getIndex()));
        instructions.add(InjectionUtils.loadCapturedArguments(targetMethod, capturedArguments));

        MethodInsnNode transformerCall;
        if (Modifier.isStatic(transformerMethod.access)) {
            transformerCall = new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        } else {
            transformerCall = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        }
        instructions.add(transformerCall);
        transformerMethodCalls.add(transformerCall);
        instructions.add(new VarInsnNode(local.getType().getOpcode(Opcodes.ISTORE), local.getIndex()));
        return instructions;
    }

    private int getIntArgument(final CTarget target, final String prefix, final int fallback) {
        for (String argument : target.args()) {
            if (!argument.startsWith(prefix)) continue;
            try {
                return Integer.parseInt(argument.substring(prefix.length()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid argument '" + argument + "'");
            }
        }
        return fallback;
    }

}
