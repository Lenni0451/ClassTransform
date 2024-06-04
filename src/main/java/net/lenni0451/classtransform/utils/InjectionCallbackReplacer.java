package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.InjectionCallback;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static net.lenni0451.classtransform.utils.Types.*;

public class InjectionCallbackReplacer {

    /**
     * Replace all {@link InjectionCallback} instances with an {@code Object[]}.<br>
     * This can be used if the {@link InjectionCallback} class is not available in the target environment.<br>
     * The object array will contain the following values: {@code {cancellable, cancelled, returnValue, returnValueSet}}<br>
     * All important checks from the {@link InjectionCallback} class are also included in the replacement.
     *
     * @param classNode The {@link ClassNode} to replace the {@link InjectionCallback} instances in
     */
    public static void replaceCallback(final ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            replaceParameters(method);
            replaceConstructor(method);
            replaceInvokes(method);
        }
    }

    private static void replaceParameters(final MethodNode methodNode) {
        Type injectionCallback = type(InjectionCallback.class);
        Type objectArray = type(Object[].class);
        if (methodNode.signature != null && methodNode.signature.contains(injectionCallback.getDescriptor())) {
            methodNode.signature = methodNode.signature.replace(injectionCallback.getDescriptor(), objectArray.getDescriptor());
        }

        methodNode.desc = replaceParameter(methodNode.desc, injectionCallback, objectArray);
        for (AbstractInsnNode insn : methodNode.instructions) {
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode methodInsn = (MethodInsnNode) insn;

            methodInsn.desc = replaceParameter(methodInsn.desc, injectionCallback, objectArray);
        }
    }

    private static void replaceConstructor(final MethodNode methodNode) {
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.NEW) {
                TypeInsnNode typeInsn = (TypeInsnNode) insn;
                if (!typeInsn.desc.equals(internalName(InjectionCallback.class))) continue;

                InsnList replacement = new InsnList();
                replacement.add(ASMUtils.intPush(4));
                replacement.add(new TypeInsnNode(Opcodes.ANEWARRAY, internalName(Object.class)));

                methodNode.instructions.insertBefore(typeInsn, replacement);
                methodNode.instructions.remove(typeInsn);
            } else if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (!methodInsn.owner.equals(internalName(InjectionCallback.class))) continue;
                if (!methodInsn.name.equals(MN_Init)) throw new UnsupportedOperationException("Unknown InjectionCallback constructor: " + methodInsn.name);

                InsnList replacement = new InsnList();
                if (methodInsn.desc.equals(methodDescriptor(void.class, boolean.class))) { //Object[], boolean
                    //Change the stack -> Object[], Object[], boolean
                    replacement.add(new InsnNode(Opcodes.SWAP));   //boolean, Object[]
                    replacement.add(new InsnNode(Opcodes.DUP_X1)); //Object[], boolean, Object[]
                    replacement.add(new InsnNode(Opcodes.SWAP));   //Object[], Object[], boolean

                    //Set the cancellable value
                    replacement.add(ASMUtils.intPush(0));
                    replacement.add(new InsnNode(Opcodes.SWAP));
                    replacement.add(ASMUtils.getPrimitiveToObject(Type.BOOLEAN_TYPE));
                    replacement.add(new InsnNode(Opcodes.AASTORE));

                    //Set cancelled to false
                    replacement.add(new InsnNode(Opcodes.DUP));
                    replacement.add(ASMUtils.intPush(1));
                    replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, internalName(Boolean.class), "FALSE", typeDescriptor(Boolean.class)));
                    replacement.add(new InsnNode(Opcodes.AASTORE));

                    //Mark the return value as not set
                    replacement.add(new InsnNode(Opcodes.DUP));
                    replacement.add(ASMUtils.intPush(3));
                    replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, internalName(Boolean.class), "FALSE", typeDescriptor(Boolean.class)));
                    replacement.add(new InsnNode(Opcodes.AASTORE));
                } else if (methodInsn.desc.equals(methodDescriptor(void.class, boolean.class, Object.class))) { //Object[], boolean, Object
                    //Change the stack -> Object[], Object, Object[], boolean
                    replacement.add(new InsnNode(Opcodes.DUP_X2)); //Object, Object[], boolean, Object
                    replacement.add(new InsnNode(Opcodes.POP));    //Object, Object[], boolean
                    replacement.add(new InsnNode(Opcodes.SWAP));   //Object, boolean, Object[]
                    replacement.add(new InsnNode(Opcodes.DUP_X2)); //Object[], Object, boolean, Object[]
                    replacement.add(new InsnNode(Opcodes.SWAP));   //Object[], Object, Object[], boolean

                    //Set the cancellable value
                    replacement.add(ASMUtils.intPush(0));
                    replacement.add(new InsnNode(Opcodes.SWAP));
                    replacement.add(ASMUtils.getPrimitiveToObject(Type.BOOLEAN_TYPE));
                    replacement.add(new InsnNode(Opcodes.AASTORE));

                    //Set the return value
                    replacement.add(new InsnNode(Opcodes.SWAP));
                    replacement.add(new InsnNode(Opcodes.DUP_X1));
                    replacement.add(new InsnNode(Opcodes.SWAP));
                    replacement.add(ASMUtils.intPush(2));
                    replacement.add(new InsnNode(Opcodes.SWAP));
                    replacement.add(new InsnNode(Opcodes.AASTORE));

                    //Set cancelled to false
                    replacement.add(new InsnNode(Opcodes.DUP));
                    replacement.add(ASMUtils.intPush(1));
                    replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, internalName(Boolean.class), "FALSE", typeDescriptor(Boolean.class)));
                    replacement.add(new InsnNode(Opcodes.AASTORE));

                    //Mark the return value as set
                    replacement.add(new InsnNode(Opcodes.DUP));
                    replacement.add(ASMUtils.intPush(3));
                    replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, internalName(Boolean.class), "TRUE", typeDescriptor(Boolean.class)));
                    replacement.add(new InsnNode(Opcodes.AASTORE));
                } else {
                    throw new UnsupportedOperationException("Unknown InjectionCallback constructor: " + methodInsn.desc);
                }
                methodNode.instructions.insertBefore(methodInsn, replacement);
                methodNode.instructions.remove(methodInsn);
            }
        }
    }

    private static void replaceInvokes(final MethodNode methodNode) {
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode methodInsn = (MethodInsnNode) insn;
            if (!methodInsn.owner.equals(internalName(InjectionCallback.class))) continue;

            InsnList replacement = new InsnList();
            if (methodInsn.name.equals("isCancelled")) { //Object[] > boolean
                replacement.add(ASMUtils.intPush(1));
                replacement.add(new InsnNode(Opcodes.AALOAD));
                replacement.add(ASMUtils.getCast(Type.BOOLEAN_TYPE));
            } else if (methodInsn.name.equals("setCancelled")) { //Object[], boolean > void
                int arrayIndex = ASMUtils.getFreeVarIndex(methodNode);
                int booleanIndex = arrayIndex + 1;
                //Store the values in local variables
                replacement.add(new VarInsnNode(Opcodes.ISTORE, booleanIndex));
                replacement.add(new VarInsnNode(Opcodes.ASTORE, arrayIndex));

                LabelNode elseLabel = new LabelNode();
                //Check if the cancel value is true
                replacement.add(new VarInsnNode(Opcodes.ILOAD, booleanIndex));
                replacement.add(new JumpInsnNode(Opcodes.IFEQ, elseLabel));
                //Check if the callback is cancellable
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(0));
                replacement.add(new InsnNode(Opcodes.AALOAD));
                replacement.add(ASMUtils.getCast(Type.BOOLEAN_TYPE));
                replacement.add(new JumpInsnNode(Opcodes.IFNE, elseLabel));

                //Throw an exception if the callback is not cancellable
                replacement.add(new TypeInsnNode(Opcodes.NEW, internalName(IllegalArgumentException.class)));
                replacement.add(new InsnNode(Opcodes.DUP));
                replacement.add(new LdcInsnNode("Cannot cancel a non-cancellable callback"));
                replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalName(IllegalArgumentException.class), MN_Init, methodDescriptor(void.class, String.class), false));
                replacement.add(new InsnNode(Opcodes.ATHROW));

                replacement.add(elseLabel);
                //Set the callback cancelled
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(1));
                replacement.add(new VarInsnNode(Opcodes.ILOAD, booleanIndex));
                replacement.add(ASMUtils.getPrimitiveToObject(Type.BOOLEAN_TYPE));
                replacement.add(new InsnNode(Opcodes.AASTORE));
            } else if (methodInsn.name.equals("isCancellable")) { //Object[] > boolean
                replacement.add(ASMUtils.intPush(0));
                replacement.add(new InsnNode(Opcodes.AALOAD));
                replacement.add(ASMUtils.getCast(Type.BOOLEAN_TYPE));
            } else if (methodInsn.name.equals("getReturnValue") || methodInsn.name.equals("castReturnValue")) { //Object[] > Object
                int arrayIndex = ASMUtils.getFreeVarIndex(methodNode);
                //Store the array in a local variable
                replacement.add(new VarInsnNode(Opcodes.ASTORE, arrayIndex));

                LabelNode elseLabel = new LabelNode();
                //Check if the return value is set
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(3));
                replacement.add(new InsnNode(Opcodes.AALOAD));
                replacement.add(ASMUtils.getCast(Type.BOOLEAN_TYPE));
                replacement.add(new JumpInsnNode(Opcodes.IFNE, elseLabel));

                //Throw an exception if the return value is not set
                replacement.add(new TypeInsnNode(Opcodes.NEW, internalName(IllegalStateException.class)));
                replacement.add(new InsnNode(Opcodes.DUP));
                replacement.add(new LdcInsnNode("Return value not set"));
                replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalName(IllegalStateException.class), MN_Init, methodDescriptor(void.class, String.class), false));
                replacement.add(new InsnNode(Opcodes.ATHROW));

                replacement.add(elseLabel);
                //Get the return value
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(2));
                replacement.add(new InsnNode(Opcodes.AALOAD));
            } else if (methodInsn.name.equals("setReturnValue")) { //Object[], Object > void
                int arrayIndex = ASMUtils.getFreeVarIndex(methodNode);
                int objectIndex = arrayIndex + 1;
                //Store the values in local variables
                replacement.add(new VarInsnNode(Opcodes.ASTORE, objectIndex));
                replacement.add(new VarInsnNode(Opcodes.ASTORE, arrayIndex));

                LabelNode elseLabel = new LabelNode();
                //Check if the callback is cancellable
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(0));
                replacement.add(new InsnNode(Opcodes.AALOAD));
                replacement.add(ASMUtils.getCast(Type.BOOLEAN_TYPE));
                replacement.add(new JumpInsnNode(Opcodes.IFNE, elseLabel));

                //Throw an exception if the callback is not cancellable
                replacement.add(new TypeInsnNode(Opcodes.NEW, internalName(IllegalStateException.class)));
                replacement.add(new InsnNode(Opcodes.DUP));
                replacement.add(new LdcInsnNode("Cannot cancel a non-cancellable callback"));
                replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, internalName(IllegalStateException.class), MN_Init, methodDescriptor(void.class, String.class), false));
                replacement.add(new InsnNode(Opcodes.ATHROW));

                replacement.add(elseLabel);
                //Set the callback cancelled
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(1));
                replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, internalName(Boolean.class), "TRUE", typeDescriptor(Boolean.class)));
                replacement.add(new InsnNode(Opcodes.AASTORE));

                //Set the return value
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(2));
                replacement.add(new VarInsnNode(Opcodes.ALOAD, objectIndex));
                replacement.add(new InsnNode(Opcodes.AASTORE));

                //Mark the return value as set
                replacement.add(new VarInsnNode(Opcodes.ALOAD, arrayIndex));
                replacement.add(ASMUtils.intPush(3));
                replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, internalName(Boolean.class), "TRUE", typeDescriptor(Boolean.class)));
                replacement.add(new InsnNode(Opcodes.AASTORE));
            } else {
                throw new UnsupportedOperationException("Unknown InjectionCallback method: " + methodInsn.name);
            }
            methodNode.instructions.insertBefore(methodInsn, replacement);
            methodNode.instructions.remove(methodInsn);
        }
    }

    private static String replaceParameter(final String methodDescriptor, final Type toReplace, final Type replacement) {
        Type returnType = returnType(methodDescriptor);
        Type[] parameterTypes = argumentTypes(methodDescriptor);
        if (returnType.equals(toReplace)) returnType = replacement;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].equals(toReplace)) parameterTypes[i] = replacement;
        }
        return methodDescriptor(returnType, (Object[]) parameterTypes);
    }

}
