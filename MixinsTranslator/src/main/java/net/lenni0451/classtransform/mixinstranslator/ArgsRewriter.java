package net.lenni0451.classtransform.mixinstranslator;

import net.lenni0451.classtransform.CArgs;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
final class ArgsRewriter {

    private static final Type ARGS = Type.getType("Lorg/spongepowered/asm/mixin/injection/Args;");
    private static final Type CARGS = Type.getType(CArgs.class);
    private static final String CARGS_GET_TYPE_DESCRIPTOR = "(I)Lorg/objectweb/asm/Type;";

    static void rewrite(final MethodNode method) {
        Type[] parameterTypes = Type.getArgumentTypes(method.desc);
        Type returnType = Type.getReturnType(method.desc);
        boolean changed = false;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].equals(ARGS)) {
                parameterTypes[i] = CARGS;
                changed = true;
            }
        }
        if (returnType.equals(ARGS)) {
            returnType = CARGS;
            changed = true;
        }
        if (changed) {
            method.desc = Type.getMethodDescriptor(returnType, parameterTypes);
            if (method.signature != null) {
                method.signature = method.signature.replace(ARGS.getDescriptor(), CARGS.getDescriptor());
            }
        }

        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
                if (ARGS.getInternalName().equals(methodInstruction.owner) && "getType".equals(methodInstruction.name)) {
                    methodInstruction.desc = CARGS_GET_TYPE_DESCRIPTOR;
                } else {
                    methodInstruction.desc = replaceDescriptor(methodInstruction.desc);
                }
                methodInstruction.owner = replaceType(methodInstruction.owner);
            } else if (instruction instanceof FieldInsnNode) {
                FieldInsnNode fieldInstruction = (FieldInsnNode) instruction;
                fieldInstruction.desc = replaceDescriptor(fieldInstruction.desc);
            } else if (instruction instanceof TypeInsnNode) {
                TypeInsnNode typeInstruction = (TypeInsnNode) instruction;
                typeInstruction.desc = replaceType(typeInstruction.desc);
            } else if (instruction instanceof LdcInsnNode) {
                LdcInsnNode ldcInstruction = (LdcInsnNode) instruction;
                if (ldcInstruction.cst instanceof Type) {
                    Type type = (Type) ldcInstruction.cst;
                    if (type.equals(ARGS)) ldcInstruction.cst = CARGS;
                    else if (type.getSort() == Type.METHOD) ldcInstruction.cst = Type.getMethodType(replaceDescriptor(type.getDescriptor()));
                }
            }
        }
        if (method.localVariables != null) {
            for (LocalVariableNode localVariable : method.localVariables) {
                localVariable.desc = replaceDescriptor(localVariable.desc);
            }
        }
    }

    private static String replaceType(final String type) {
        return ARGS.getInternalName().equals(type) ? CARGS.getInternalName() : type;
    }

    private static String replaceDescriptor(final String descriptor) {
        return descriptor.replace(ARGS.getDescriptor(), CARGS.getDescriptor());
    }

}
