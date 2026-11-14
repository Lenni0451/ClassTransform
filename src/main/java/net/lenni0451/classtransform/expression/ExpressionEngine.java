package net.lenni0451.classtransform.expression;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.expression.CDefinition;
import net.lenni0451.classtransform.annotations.expression.CDefinitions;
import net.lenni0451.classtransform.annotations.expression.CExpression;
import net.lenni0451.classtransform.annotations.expression.CExpressions;
import net.lenni0451.classtransform.annotations.injection.*;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * Parse expressions, analyze the target method flow and turn matches into operation instructions.
 */
@ParametersAreNonnullByDefault
public class ExpressionEngine {

    private static final ThreadLocal<IdentityHashMap<MethodNode, Map<String, List<ExpressionMatch>>>> CACHE = ThreadLocal.withInitial(IdentityHashMap::new);
    private static int helperCounter;

    /**
     * Clear all cached matches. Called after each class transformation.
     */
    public static void clear() {
        CACHE.remove();
    }

    /**
     * Match an expression against a method.
     *
     * @param targetClass       The transformed class
     * @param targetMethod      The target method
     * @param transformerMethod The handler method containing the expressions and definitions
     * @param id                The expression id
     * @param matches           The map to store match details in
     * @return The captured instructions
     */
    public static List<AbstractInsnNode> match(final ClassNode targetClass, final MethodNode targetMethod, final MethodNode transformerMethod, final String id, final Map<AbstractInsnNode, ExpressionMatch> matches) {
        return match(null, targetClass, targetMethod, transformerMethod, id, matches);
    }

    /**
     * Match an expression against a method.
     *
     * @param manager           The transformer manager used for class hierarchy lookups
     * @param targetClass       The transformed class
     * @param targetMethod      The target method
     * @param transformerMethod The handler method containing the expressions and definitions
     * @param id                The expression id
     * @param matches           The map to store match details in
     * @return The captured instructions
     */
    public static List<AbstractInsnNode> match(@Nullable final TransformerManager manager, final ClassNode targetClass, final MethodNode targetMethod, final MethodNode transformerMethod, final String id, final Map<AbstractInsnNode, ExpressionMatch> matches) {
        String signature = signature(transformerMethod) + "|" + id;
        Map<String, List<ExpressionMatch>> methodCache = CACHE.get().computeIfAbsent(targetMethod, k -> new HashMap<>());
        List<ExpressionMatch> cached = methodCache.get(signature);
        if (cached == null) {
            cached = matchUncached(manager, targetClass, targetMethod, transformerMethod, id);
            methodCache.put(signature, cached);
        }
        List<AbstractInsnNode> result = new ArrayList<>();
        for (ExpressionMatch match : cached) {
            matches.put(match.getTarget(), match);
            result.add(match.getTarget());
        }
        return result;
    }

    private static List<ExpressionMatch> matchUncached(@Nullable final TransformerManager manager, final ClassNode targetClass, final MethodNode targetMethod, final MethodNode transformerMethod, final String id) {
        List<AnnotationNode> expressionAnnotations = findAnnotations(transformerMethod, CExpression.class, CExpressions.class);
        List<AnnotationNode> definitionAnnotations = findAnnotations(transformerMethod, CDefinition.class, CDefinitions.class);
        ExpressionIdentifiers identifiers = new ExpressionIdentifiers(targetClass, targetMethod, manager);
        for (AnnotationNode definition : definitionAnnotations) {
            Map<String, Object> values = AnnotationUtils.listToMap(definition.values);
            String definitionId = string(values.get("id"), "");
            identifiers.addDefinition(
                    definitionId,
                    strings(values.get("method")),
                    strings(values.get("field")),
                    types(values.get("type")),
                    annotations(values.get("local"))
            );
        }

        List<String> expressions = new ArrayList<>();
        for (AnnotationNode annotation : expressionAnnotations) {
            Map<String, Object> values = AnnotationUtils.listToMap(annotation.values);
            if (!string(values.get("id"), "").equals(id)) continue;
            expressions.addAll(strings(values.get("value")));
        }
        if (expressions.isEmpty()) {
            throw new IllegalArgumentException("No expression found" + (id.isEmpty() ? "" : " for id '" + id + "'") + " on " + transformerMethod.name + transformerMethod.desc);
        }

        ExpressionContext.ExpressionContextType contextType = contextType(transformerMethod);
        Collection<ExpressionFlow.FlowValue> flows = ExpressionFlow.analyze(targetClass, targetMethod);
        Map<AbstractInsnNode, ExpressionMatch> expanded = new IdentityHashMap<>();
        Map<AbstractInsnNode, ExpressionMatch> result = new LinkedHashMap<>();
        for (String expressionString : expressions) {
            Expression expression = ExpressionParser.parse(expressionString);
            for (ExpressionFlow.FlowValue flow : flows) {
                ExpressionContext ctx = new ExpressionContext(identifiers, contextType, false);
                if (!expression.matches(flow, ctx)) continue;
                if (ctx.getCaptures().isEmpty()) expression.capture(flow, ctx);
                for (ExpressionContext.Capture capture : ctx.getCaptures()) {
                    ExpressionMatch match = createMatch(targetClass, targetMethod, capture, expanded);
                    if (match != null) result.put(match.getTarget(), match);
                }
            }
        }
        List<ExpressionMatch> matches = new ArrayList<>(result.values());
        matches.sort(Comparator.comparingInt(match -> targetMethod.instructions.indexOf(match.getTarget())));
        return matches;
    }

    @Nullable
    private static ExpressionMatch createMatch(final ClassNode targetClass, final MethodNode targetMethod, final ExpressionContext.Capture capture, final Map<AbstractInsnNode, ExpressionMatch> expanded) {
        if (capture.getJump() != null) {
            ExpressionMatch existing = expanded.get(capture.getJump());
            if (existing != null) return existing;
            ExpressionMatch match = expandComparison(targetClass, targetMethod, capture);
            if (match != null) expanded.put(capture.getJump(), match);
            return match;
        }
        AbstractInsnNode instruction = capture.getInstruction();
        if (instruction instanceof IincInsnNode) {
            if (!targetMethod.instructions.contains(instruction)) return null;
            instruction = expandIinc(targetMethod, (IincInsnNode) instruction);
        }
        Type valueType = capture.getValueType() == null ? Type.VOID_TYPE : capture.getValueType();
        int index = targetMethod.instructions.indexOf(instruction);
        return new ExpressionMatch(instruction, valueType, capture.getReceiverType(), capture.getArgumentTypes(), null, null, false, index, index);
    }

    private static AbstractInsnNode expandIinc(final MethodNode targetMethod, final IincInsnNode iinc) {
        InsnNode add = new InsnNode(Opcodes.IADD);
        InsnList replacement = new InsnList();
        replacement.add(new VarInsnNode(Opcodes.ILOAD, iinc.var));
        replacement.add(new LdcInsnNode(iinc.incr));
        replacement.add(add);
        replacement.add(new VarInsnNode(Opcodes.ISTORE, iinc.var));
        targetMethod.instructions.insertBefore(iinc, replacement);
        targetMethod.instructions.remove(iinc);
        return add;
    }

    @Nullable
    private static ExpressionMatch expandComparison(final ClassNode targetClass, final MethodNode targetMethod, final ExpressionContext.Capture capture) {
        JumpInsnNode jump = capture.getJump();
        if (jump == null || !targetMethod.instructions.contains(jump)) return null;
        Type[] argumentTypes = normalize(capture.getArgumentTypes());
        int opcode = capture.isInverted() ? invert(jump.getOpcode()) : jump.getOpcode();
        if (opcode == -1) return null;

        String name = uniqueName(targetClass, "ct$expression$cmp$" + helperCounter++);
        MethodNode helper = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, name, Type.getMethodDescriptor(Type.BOOLEAN_TYPE, argumentTypes), null, null);
        int index = 0;
        for (Type argumentType : argumentTypes) {
            helper.instructions.add(new VarInsnNode(argumentType.getOpcode(Opcodes.ILOAD), index));
            index += argumentType.getSize();
        }
        AbstractInsnNode comparison = capture.getComparison();
        if (comparison != null) {
            helper.instructions.add(comparison.clone(Collections.emptyMap()));
        }
        LabelNode trueLabel = new LabelNode();
        helper.instructions.add(new JumpInsnNode(opcode, trueLabel));
        helper.instructions.add(new InsnNode(Opcodes.ICONST_0));
        helper.instructions.add(new InsnNode(Opcodes.IRETURN));
        helper.instructions.add(trueLabel);
        helper.instructions.add(new InsnNode(Opcodes.ICONST_1));
        helper.instructions.add(new InsnNode(Opcodes.IRETURN));
        targetClass.methods.add(helper);

        AbstractInsnNode marker = comparison != null && targetMethod.instructions.contains(comparison) ? comparison : null;
        InsnList replacement = new InsnList();
        MethodInsnNode helperCall = new MethodInsnNode(Opcodes.INVOKESTATIC, targetClass.name, helper.name, helper.desc, (targetClass.access & Opcodes.ACC_INTERFACE) != 0);
        replacement.add(helperCall);
        replacement.add(new JumpInsnNode(capture.isInverted() ? Opcodes.IFEQ : Opcodes.IFNE, jump.label));
        targetMethod.instructions.insertBefore(jump, replacement);
        targetMethod.instructions.remove(jump);
        if (marker != null) targetMethod.instructions.remove(marker);

        int indexOfCall = targetMethod.instructions.indexOf(helperCall);
        return new ExpressionMatch(helperCall, Type.BOOLEAN_TYPE, null, argumentTypes, null, null, false, indexOfCall, indexOfCall);
    }

    private static Type[] normalize(final Type[] types) {
        Type[] result = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            if (ExpressionTypes.isIntLike(type)) result[i] = Type.INT_TYPE;
            else if (type == null || type.equals(ExpressionTypes.NULL_TYPE)) result[i] = Type.getType(Object.class);
            else result[i] = type;
        }
        return result;
    }

    private static int invert(final int opcode) {
        switch (opcode) {
            case Opcodes.IFEQ: return Opcodes.IFNE;
            case Opcodes.IFNE: return Opcodes.IFEQ;
            case Opcodes.IFLT: return Opcodes.IFGE;
            case Opcodes.IFGE: return Opcodes.IFLT;
            case Opcodes.IFGT: return Opcodes.IFLE;
            case Opcodes.IFLE: return Opcodes.IFGT;
            case Opcodes.IF_ICMPEQ: return Opcodes.IF_ICMPNE;
            case Opcodes.IF_ICMPNE: return Opcodes.IF_ICMPEQ;
            case Opcodes.IF_ICMPLT: return Opcodes.IF_ICMPGE;
            case Opcodes.IF_ICMPGE: return Opcodes.IF_ICMPLT;
            case Opcodes.IF_ICMPGT: return Opcodes.IF_ICMPLE;
            case Opcodes.IF_ICMPLE: return Opcodes.IF_ICMPGT;
            case Opcodes.IF_ACMPEQ: return Opcodes.IF_ACMPNE;
            case Opcodes.IF_ACMPNE: return Opcodes.IF_ACMPEQ;
            case Opcodes.IFNULL: return Opcodes.IFNONNULL;
            case Opcodes.IFNONNULL: return Opcodes.IFNULL;
            default: return -1;
        }
    }

    private static String uniqueName(final ClassNode targetClass, final String base) {
        String name = base;
        int suffix = 0;
        while (hasMethod(targetClass, name)) name = base + "$" + suffix++;
        return name;
    }

    private static boolean hasMethod(final ClassNode targetClass, final String name) {
        for (MethodNode method : targetClass.methods) if (method.name.equals(name)) return true;
        return false;
    }

    private static String signature(final MethodNode method) {
        StringBuilder builder = new StringBuilder();
        for (AnnotationNode annotation : findAnnotations(method, CExpression.class, CExpressions.class)) {
            builder.append(annotation.desc);
            builder.append(AnnotationUtils.listToMap(annotation.values));
            builder.append(';');
        }
        for (AnnotationNode annotation : findAnnotations(method, CDefinition.class, CDefinitions.class)) {
            builder.append(annotation.desc);
            builder.append(AnnotationUtils.listToMap(annotation.values));
            builder.append(';');
        }
        return builder.toString();
    }

    private static ExpressionContext.ExpressionContextType contextType(final MethodNode method) {
        if (hasAnnotation(method, CInject.class)) return ExpressionContext.ExpressionContextType.INJECT;
        if (hasAnnotation(method, CRedirect.class)) return ExpressionContext.ExpressionContextType.REDIRECT;
        if (hasAnnotation(method, CWrapOperation.class)) return ExpressionContext.ExpressionContextType.WRAP_OPERATION;
        if (hasAnnotation(method, CModifyExpressionValue.class)) return ExpressionContext.ExpressionContextType.MODIFY_EXPRESSION_VALUE;
        if (hasAnnotation(method, CModifyReceiver.class)) return ExpressionContext.ExpressionContextType.MODIFY_RECEIVER;
        if (hasAnnotation(method, CModifyReturnValue.class)) return ExpressionContext.ExpressionContextType.MODIFY_RETURN_VALUE;
        if (hasAnnotation(method, CModifyConstant.class)) return ExpressionContext.ExpressionContextType.MODIFY_CONSTANT;
        if (hasAnnotation(method, CWrapCondition.class)) return ExpressionContext.ExpressionContextType.WRAP_WITH_CONDITION;
        return ExpressionContext.ExpressionContextType.CUSTOM;
    }

    private static boolean hasAnnotation(final MethodNode method, final Class<?> annotation) {
        String desc = Type.getDescriptor(annotation);
        if (method.visibleAnnotations != null) for (AnnotationNode node : method.visibleAnnotations) if (node.desc.equals(desc)) return true;
        if (method.invisibleAnnotations != null) for (AnnotationNode node : method.invisibleAnnotations) if (node.desc.equals(desc)) return true;
        return false;
    }

    private static List<AnnotationNode> findAnnotations(final MethodNode method, final Class<?> annotation, final Class<?> container) {
        List<AnnotationNode> result = new ArrayList<>();
        String annotationDesc = Type.getDescriptor(annotation);
        String containerDesc = Type.getDescriptor(container);
        for (List<AnnotationNode> annotations : Arrays.asList(method.visibleAnnotations, method.invisibleAnnotations)) {
            if (annotations == null) continue;
            for (AnnotationNode node : annotations) {
                if (node.desc.equals(annotationDesc)) result.add(node);
                else if (node.desc.equals(containerDesc)) {
                    Map<String, Object> values = AnnotationUtils.listToMap(node.values);
                    for (AnnotationNode nested : annotations(values.get("value"))) result.add(nested);
                }
            }
        }
        return result;
    }

    private static List<String> strings(@Nullable final Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof String) result.add((String) value);
        else if (value instanceof List) for (Object entry : (List<?>) value) if (entry instanceof String) result.add((String) entry);
        return result;
    }

    private static List<Type> types(@Nullable final Object value) {
        List<Type> result = new ArrayList<>();
        if (value instanceof Type) result.add((Type) value);
        else if (value instanceof List) for (Object entry : (List<?>) value) if (entry instanceof Type) result.add((Type) entry);
        return result;
    }

    private static List<AnnotationNode> annotations(@Nullable final Object value) {
        List<AnnotationNode> result = new ArrayList<>();
        if (value instanceof AnnotationNode) result.add((AnnotationNode) value);
        else if (value instanceof List) for (Object entry : (List<?>) value) if (entry instanceof AnnotationNode) result.add((AnnotationNode) entry);
        return result;
    }

    private static String string(@Nullable final Object value, final String fallback) {
        return value instanceof String ? (String) value : fallback;
    }

}
