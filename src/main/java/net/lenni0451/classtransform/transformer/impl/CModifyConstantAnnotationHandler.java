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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CModifyConstant} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyConstantAnnotationHandler extends InjectionAnnotationHandler<CModifyConstant> implements IInjectionTarget {

    public CModifyConstantAnnotationHandler() {
        super(CModifyConstant.class, CModifyConstant::method);
    }

    @Override
    public void transform(CModifyConstant annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        IParsedAnnotation parsedAnnotation = (IParsedAnnotation) annotation;
        boolean hasNullValue = parsedAnnotation.wasSet("nullValue");
        boolean hasIntValue = parsedAnnotation.wasSet("intValue");
        boolean hasLongValue = parsedAnnotation.wasSet("longValue");
        boolean hasFloatValue = parsedAnnotation.wasSet("floatValue");
        boolean hasDoubleValue = parsedAnnotation.wasSet("doubleValue");
        boolean hasStringValue = parsedAnnotation.wasSet("stringValue");
        boolean hasTypeValue = parsedAnnotation.wasSet("typeValue");

        if (this.getTrueCount(hasNullValue, hasIntValue, hasLongValue, hasFloatValue, hasDoubleValue, hasStringValue, hasTypeValue) != 1) {
            throw new TransformerException(transformerMethod, transformer, "must have exactly one target constant");
        }
        Type constantType;
        if (hasNullValue) constantType = null;
        else if (hasIntValue) constantType = Type.INT_TYPE;
        else if (hasLongValue) constantType = Type.LONG_TYPE;
        else if (hasFloatValue) constantType = Type.FLOAT_TYPE;
        else if (hasDoubleValue) constantType = Type.DOUBLE_TYPE;
        else if (hasStringValue) constantType = type(String.class);
        else if (hasTypeValue) constantType = type(Class.class);
        else throw new IllegalStateException("Unknown return type wanted because of unknown constant. If you see this, please report this to the developer.");

        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }
        Type[] transformerArguments = argumentTypes(transformerMethod.desc);
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

        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyConstant");
        List<AbstractInsnNode> toReplace = InjectionPlan.select(transformerManager, target, this.getInjectionTargets(annotation, transformerManager).get(0), annotation.slice());
        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        for (int i = 0; i < toReplace.size(); i++) {
            AbstractInsnNode instruction = toReplace.get(i);

            MethodInsnNode invoke;
            InsnList instructions = new InsnList();
            if (!Modifier.isStatic(transformerMethod.access)) {
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                if (transformerArguments.length == 1) instructions.add(ASMUtils.swap(transformerArguments[0], Type.getType(Object.class)));
                invoke = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc);
            } else {
                invoke = new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
            }
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

    @Override
    protected List<CTarget> getInjectionTargets(CModifyConstant annotation, TransformerManager manager) {
        IParsedAnnotation parsed = (IParsedAnnotation) annotation;
        Map<String, Object> values = new HashMap<>();
        String constant = null;
        String[] names = {"nullValue", "intValue", "longValue", "floatValue", "doubleValue", "stringValue", "typeValue"};
        String[] types = {"null", "int", "long", "float", "double", "string", "type"};
        for (int i = 0; i < names.length; i++) {
            if (!parsed.wasSet(names[i])) continue;
            if (constant != null) throw new IllegalArgumentException("Exactly one constant discriminator is required");
            Object value = parsed.getValues().get(names[i]);
            if (i == 0) constant = Boolean.TRUE.equals(value) ? "null" : "null false";
            else if (i == 6) constant = "type " + type(value).getDescriptor();
            else constant = types[i] + " " + value;
        }
        if (constant == null) throw new IllegalArgumentException("Exactly one constant discriminator is required");
        values.put("value", "CONSTANT");
        values.put("target", constant);
        values.put("ordinal", annotation.ordinal());
        values.put("optional", annotation.optional());
        values.put("slice", annotation.sliceId());
        return Collections.singletonList(AnnotationParser.parse(CTarget.class, manager, values));
    }

    private int getTrueCount(final boolean... booleans) {
        int count = 0;
        for (boolean b : booleans) if (b) count++;
        return count;
    }

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, @Nullable CSlice slice) {
        return null;
    }

}
