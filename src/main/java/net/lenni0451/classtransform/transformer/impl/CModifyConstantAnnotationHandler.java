package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * The annotation handler for the {@link CModifyConstant} annotation.
 */
@ParametersAreNonnullByDefault
public class CModifyConstantAnnotationHandler extends RemovingTargetAnnotationHandler<CModifyConstant> implements IInjectionTarget {

    public CModifyConstantAnnotationHandler() {
        super(CModifyConstant.class, CModifyConstant::method);
    }

    @Override
    public void transform(CModifyConstant annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
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

        this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyConstant");
        List<AbstractInsnNode> toReplace = new ArrayList<>();
        for (AbstractInsnNode instruction : this.getSlice(transformerManager.getInjectionTargets(), target, annotation.slice())) {
            if (hasNullValue) {
                if (instruction.getOpcode() == Opcodes.ACONST_NULL && annotation.nullValue()) {
                    toReplace.add(instruction);
                }
            } else if (hasIntValue) {
                Number number = ASMUtils.getNumber(instruction);
                if ((number instanceof Byte || number instanceof Short || number instanceof Integer) && number.intValue() == annotation.intValue()) {
                    toReplace.add(instruction);
                }
            } else if (hasLongValue) {
                Number number = ASMUtils.getNumber(instruction);
                if (number instanceof Long && number.longValue() == annotation.longValue()) {
                    toReplace.add(instruction);
                }
            } else if (hasFloatValue) {
                Number number = ASMUtils.getNumber(instruction);
                if (number instanceof Float && number.floatValue() == annotation.floatValue()) {
                    toReplace.add(instruction);
                }
            } else if (hasDoubleValue) {
                Number number = ASMUtils.getNumber(instruction);
                if (number instanceof Double && number.doubleValue() == annotation.doubleValue()) {
                    toReplace.add(instruction);
                }
            } else if (hasStringValue) {
                if (instruction.getOpcode() == Opcodes.LDC && ((LdcInsnNode) instruction).cst.equals(annotation.stringValue())) {
                    toReplace.add(instruction);
                }
            } else if (hasTypeValue) {
                if (instruction.getOpcode() == Opcodes.LDC && ((LdcInsnNode) instruction).cst.equals(type(parsedAnnotation.getValues().get("typeValue")))) {
                    toReplace.add(instruction);
                }
            }
        }

        if (toReplace.isEmpty() && !annotation.optional()) {
            throw new TransformerException(transformerMethod, transformer, "target constant could not be found")
                    .help("e.g. intValue = 0");
        }
        for (int i = 0; i < toReplace.size(); i++) {
            AbstractInsnNode instruction = toReplace.get(i);
            if (annotation.ordinal() != -1 && i != annotation.ordinal()) continue;

            if (!Modifier.isStatic(transformerMethod.access)) {
                target.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ALOAD, 0));
                MethodInsnNode invoke = new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc);
                if (transformerArguments.length == 1) target.instructions.insert(instruction, invoke);
                else target.instructions.set(instruction, invoke);
            } else {
                MethodInsnNode invoke = new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access));
                if (transformerArguments.length == 1) target.instructions.insert(instruction, invoke);
                else target.instructions.set(instruction, invoke);
            }
        }
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
