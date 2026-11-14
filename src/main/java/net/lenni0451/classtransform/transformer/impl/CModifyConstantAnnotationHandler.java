package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CModifyConstant} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyConstantAnnotationHandler extends InjectionAnnotationHandler<CModifyConstant> implements IInjectionTarget {

    private static final String[] CONSTANT_MEMBERS = {"nullValue", "intValue", "longValue", "floatValue", "doubleValue", "stringValue", "typeValue"};
    private static final String[] CONSTANT_TYPES = {"null", "int", "long", "float", "double", "string", "type"};

    public CModifyConstantAnnotationHandler() {
        super(CModifyConstant.class, CModifyConstant::method);
    }

    @Override
    public void transform(CModifyConstant annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        List<CTarget> injectionTargets = this.getInjectionTargets(annotation, transformerManager);
        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        Type[] transformerArguments = argumentTypes(transformerMethod.desc);
        for (CTarget injectionTarget : injectionTargets) {
            Type constantType = this.getConstantType(injectionTarget.target());
            boolean expandZeroConditions = this.hasArgument(injectionTarget.args(), "expandZeroConditions=");
            if (expandZeroConditions && !Type.INT_TYPE.equals(constantType)) {
                throw new TransformerException(transformerMethod, transformer, "must target an int constant to expand zero conditions");
            }
            if (expandZeroConditions && (transformerArguments.length != 1 || !Type.INT_TYPE.equals(transformerArguments[0]))) {
                throw new TransformerException(transformerMethod, transformer, "must have the int constant as argument to expand zero conditions")
                        .help(Codifier.of(transformerMethod).param(Type.INT_TYPE));
            }
            if (constantType != null) {
                if (transformerArguments.length != 0 && (transformerArguments.length != 1 || !transformerArguments[0].equals(constantType))) {
                    throw new TransformerException(transformerMethod, transformer, "must have no arguments or the constant as argument")
                            .help(Codifier.of(transformerMethod).param(null).param(constantType));
                }
                if (!returnType(transformerMethod.desc).equals(constantType)) {
                    throw new TransformerException(transformerMethod, transformer, "must have return type of modified constant")
                            .help(Codifier.of(transformerMethod).returnType(constantType));
                }
            } else {
                if (transformerArguments.length != 0) {
                    throw new TransformerException(transformerMethod, transformer, "must have no arguments")
                            .help(Codifier.of(transformerMethod).param(null));
                }
                Type methodReturnType = returnType(transformerMethod.desc);
                if (methodReturnType.equals(Type.VOID_TYPE) || methodReturnType.getDescriptor().length() == 1) {
                    throw new TransformerException(transformerMethod, transformer, "must have any object return type")
                            .help(Codifier.of(transformerMethod).returnType(type(Object.class)));
                }
            }
        }

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyConstant");
        Set<AbstractInsnNode> selected = new LinkedHashSet<>();
        for (CTarget injectionTarget : injectionTargets) {
            selected.addAll(InjectionPlan.select(transformerManager, target, injectionTarget, annotation.slice()));
        }
        List<AbstractInsnNode> toReplace = new ArrayList<>(selected);
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (int i = 0; i < toReplace.size(); i++) {
            AbstractInsnNode instruction = toReplace.get(i);
            if (instruction instanceof JumpInsnNode) {
                JumpInsnNode jump = (JumpInsnNode) instruction;
                int opcode = jump.getOpcode();
                if (opcode < Opcodes.IFLT || opcode > Opcodes.IFLE) {
                    throw new TransformerException(transformerMethod, transformer, "selected an invalid opcode to expand zero conditions").targetMethod(target);
                }
                InsnList instructions = new InsnList();
                if (!Modifier.isStatic(transformerMethod.access)) instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                instructions.add(new InsnNode(Opcodes.ICONST_0));
                MethodInsnNode invoke = this.createInvoke(transformedClass, transformerMethod);
                instructions.add(invoke);
                instructions.add(new JumpInsnNode(opcode + (Opcodes.IF_ICMPLT - Opcodes.IFLT), jump.label));
                target.instructions.insertBefore(instruction, instructions);
                target.instructions.remove(instruction);
                InjectionPlan.replace(instruction, invoke);
                transformerMethodCalls.add(invoke);
                continue;
            }

            InsnList instructions = new InsnList();
            if (!Modifier.isStatic(transformerMethod.access)) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                if (transformerArguments.length == 1) instructions.add(ASMUtils.swap(transformerArguments[0], Type.getType(Object.class)));
            }
            MethodInsnNode invoke = this.createInvoke(transformedClass, transformerMethod);
            instructions.add(invoke);
            if (transformerArguments.length == 1) target.instructions.insert(InjectionPlan.after(instruction), instructions);
            else {
                target.instructions.insertBefore(instruction, instructions);
                target.instructions.remove(instruction);
            }
            InjectionPlan.replace(instruction, invoke);
            transformerMethodCalls.add(invoke);
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

    private MethodInsnNode createInvoke(final ClassNode transformedClass, final MethodNode transformerMethod) {
        if (Modifier.isStatic(transformerMethod.access)) {
            return new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
        } else {
            return new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc);
        }
    }

    @Override
    protected List<CTarget> getInjectionTargets(final CModifyConstant annotation, final TransformerManager manager) {
        IParsedAnnotation parsed = (IParsedAnnotation) annotation;
        CModifyConstant.CConstant[] constants = annotation.constants();
        List<CTarget> targets = new ArrayList<>();
        if (constants.length == 0) {
            targets.add(this.getConstantTarget(parsed, annotation.ordinal(), annotation.sliceId(), annotation.expandZeroConditions(), annotation.log(), annotation.optional(), manager));
            return targets;
        }
        for (String member : CONSTANT_MEMBERS) {
            if (parsed.wasSet(member)) throw new IllegalArgumentException("Constant selectors can not be combined with '" + member + "'");
        }
        for (CModifyConstant.CConstant constant : constants) {
            if (!(constant instanceof IParsedAnnotation)) throw new IllegalArgumentException("Unsupported constant selector");
            targets.add(this.getConstantTarget((IParsedAnnotation) constant, constant.ordinal(), constant.slice(), constant.expandZeroConditions(), constant.log(), annotation.optional(), manager));
        }
        return targets;
    }

    private CTarget getConstantTarget(final IParsedAnnotation parsed, final int ordinal, final String slice, final CModifyConstant.Condition[] expandZeroConditions, final boolean log, final boolean optional, final TransformerManager manager) {
        Map<String, Object> values = new HashMap<>();
        String constant = null;
        for (int i = 0; i < CONSTANT_MEMBERS.length; i++) {
            if (!parsed.wasSet(CONSTANT_MEMBERS[i])) continue;
            if (constant != null) throw new IllegalArgumentException("Exactly one constant discriminator is required");
            Object value = parsed.getValues().get(CONSTANT_MEMBERS[i]);
            if (i == 0) constant = Boolean.TRUE.equals(value) ? "null" : "null false";
            else if (i == 6) constant = "type " + type(value).getDescriptor();
            else constant = CONSTANT_TYPES[i] + " " + value;
        }
        boolean implicitZero = constant == null && expandZeroConditions.length > 0;
        if (implicitZero) constant = "int 0";
        if (constant == null) throw new IllegalArgumentException("Exactly one constant discriminator is required");
        values.put("value", "CONSTANT");
        values.put("target", constant);
        values.put("ordinal", ordinal);
        values.put("optional", optional);
        values.put("slice", slice);
        List<String> args = new ArrayList<>();
        if (expandZeroConditions.length > 0) {
            StringBuilder conditions = new StringBuilder();
            for (CModifyConstant.Condition condition : expandZeroConditions) {
                if (conditions.length() > 0) conditions.append(',');
                conditions.append(condition.name());
            }
            args.add("expandZeroConditions=" + conditions);
            if (implicitZero) args.add("implicitZero=true");
        }
        if (log) args.add("log=true");
        if (!args.isEmpty()) values.put("args", args.toArray(new String[0]));
        return AnnotationParser.parse(CTarget.class, manager, values);
    }

    private Type getConstantType(final String target) {
        String value = target.toLowerCase(Locale.ROOT);
        if (value.equals("null") || value.equals("null false")) return null;
        if (value.startsWith("int")) return Type.INT_TYPE;
        if (value.startsWith("long")) return Type.LONG_TYPE;
        if (value.startsWith("float")) return Type.FLOAT_TYPE;
        if (value.startsWith("double")) return Type.DOUBLE_TYPE;
        if (value.startsWith("string")) return type(String.class);
        if (value.startsWith("type")) return type(Class.class);
        throw new IllegalArgumentException("Unknown constant type '" + target + "'");
    }

    private boolean hasArgument(final String[] args, final String prefix) {
        for (String argument : args) {
            if (argument.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        return null;
    }

}
