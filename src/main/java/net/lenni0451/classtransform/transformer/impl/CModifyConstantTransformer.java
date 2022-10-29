package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CSlice;
import net.lenni0451.classtransform.annotations.CTarget;
import net.lenni0451.classtransform.annotations.injection.CModifyConstant;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.types.ARemovingTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.annotations.IParsedAnnotation;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.lenni0451.classtransform.utils.Types.*;

public class CModifyConstantTransformer extends ARemovingTransformer<CModifyConstant> implements IInjectionTarget {

    public CModifyConstantTransformer() {
        super(CModifyConstant.class);
    }

    @Override
    public void transform(CModifyConstant annotation, TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod) {
        IParsedAnnotation parsedAnnotation = (IParsedAnnotation) annotation;
        boolean hasNullValue = parsedAnnotation.wasSet("nullValue");
        boolean hasIntValue = parsedAnnotation.wasSet("intValue");
        boolean hasLongValue = parsedAnnotation.wasSet("longValue");
        boolean hasFloatValue = parsedAnnotation.wasSet("floatValue");
        boolean hasDoubleValue = parsedAnnotation.wasSet("doubleValue");
        boolean hasStringValue = parsedAnnotation.wasSet("stringValue");
        boolean hasTypeValue = parsedAnnotation.wasSet("typeValue");
        Type typeValue = hasTypeValue ? type(parsedAnnotation.getValues().get("typeValue")) : null;

        for (String targetCombi : annotation.method()) {
            List<MethodNode> targets = ASMUtils.getMethodsFromCombi(transformedClass, targetCombi);
            if (targets.isEmpty()) throw new MethodNotFoundException(transformedClass, transformer, targetCombi);
            for (MethodNode target : targets) {
                if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
                    boolean isStatic = Modifier.isStatic(target.access);
                    throw new TransformerException(transformerMethod, transformer, "must " + (isStatic ? "" : "not ") + "be static")
                            .help(Codifier.of(transformerMethod).access(isStatic ? transformerMethod.access | Modifier.STATIC : transformerMethod.access & ~Modifier.STATIC));
                }
                if (argumentTypes(transformerMethod.desc).length != 0) {
                    throw new TransformerException(transformerMethod, transformer, "must have no arguments")
                            .help(Codifier.of(transformerMethod).param(null));
                }
                if (this.getTrueCount(hasNullValue, hasIntValue, hasLongValue, hasFloatValue, hasDoubleValue, hasStringValue, hasTypeValue) != 1) {
                    throw new TransformerException(transformerMethod, transformer, "must have exactly one target constant");
                }
                {
                    Type returnType;
                    if (hasNullValue) returnType = null;
                    else if (hasIntValue) returnType = Type.INT_TYPE;
                    else if (hasLongValue) returnType = Type.LONG_TYPE;
                    else if (hasFloatValue) returnType = Type.FLOAT_TYPE;
                    else if (hasDoubleValue) returnType = Type.DOUBLE_TYPE;
                    else if (hasStringValue) returnType = type(String.class);
                    else if (hasTypeValue) returnType = type(Class.class);
                    else throw new IllegalStateException("Unknown return type wanted because of unknown constant. If you see this, please report this to the developer.");
                    if (returnType != null) {
                        if (!returnType(transformerMethod.desc).equals(returnType)) {
                            throw new TransformerException(transformerMethod, transformer, "must have return type of modified constant")
                                    .help(Codifier.of(transformerMethod).returnType(returnType));
                        }
                    } else {
                        Type methodReturnType = returnType(transformerMethod.desc);
                        if (methodReturnType.equals(Type.VOID_TYPE) || methodReturnType.getDescriptor().length() == 1) {
                            throw new TransformerException(transformerMethod, transformer, "must have any object return type")
                                    .help(Codifier.of(transformerMethod).returnType(type(Object.class)));
                        }
                    }
                }

                this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CModifyConstant");
                List<AbstractInsnNode> toReplace = new ArrayList<>();
                for (AbstractInsnNode instruction : this.getSlice(injectionTargets, target, annotation.slice())) {
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
                        if (instruction.getOpcode() == Opcodes.LDC && ((LdcInsnNode) instruction).cst.equals(typeValue)) {
                            toReplace.add(instruction);
                        }
                    }
                }

                for (int i = 0; i < toReplace.size(); i++) {
                    AbstractInsnNode instruction = toReplace.get(i);
                    if (annotation.ordinal() != -1 && i != annotation.ordinal()) continue;

                    if (!Modifier.isStatic(transformerMethod.access)) {
                        target.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ALOAD, 0));
                        target.instructions.set(instruction, new MethodInsnNode(Modifier.isInterface(transformedClass.access) ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, transformedClass.name, transformerMethod.name, transformerMethod.desc));
                    } else {
                        target.instructions.set(instruction, new MethodInsnNode(Opcodes.INVOKESTATIC, transformedClass.name, transformerMethod.name, transformerMethod.desc, Modifier.isInterface(transformedClass.access)));
                    }
                }
            }
        }
    }

    private int getTrueCount(final boolean... booleans) {
        int count = 0;
        for (boolean b : booleans) if (b) count++;
        return count;
    }

    @Override
    public List<AbstractInsnNode> getTargets(Map<String, IInjectionTarget> injectionTargets, MethodNode method, CTarget target, CSlice slice) {
        return null;
    }

}
