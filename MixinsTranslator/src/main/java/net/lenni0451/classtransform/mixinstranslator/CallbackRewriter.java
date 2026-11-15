package net.lenni0451.classtransform.mixinstranslator;

import net.lenni0451.classtransform.InjectionCallback;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Rewrites Mixin callback parameters of transformer methods to ClassTransform's {@link InjectionCallback}.
 * <p>
 * {@code CallbackInfo} and {@code CallbackInfoReturnable} parameters, return types, local variables and instructions
 * are replaced with {@link InjectionCallback}. Calls to {@code CallbackInfo#cancel()} become
 * {@link InjectionCallback#setCancelled(boolean)} calls with {@code true} pushed in front.
 * {@link MixinsTranslator} invokes this for every method after the annotations of the transformer have been
 * translated.
 */
@ParametersAreNonnullByDefault
class CallbackRewriter {

    private static final Type CALLBACK_INFO = Type.getType("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;");
    private static final Type CALLBACK_INFO_RETURNABLE = Type.getType("Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;");
    private static final Type INJECTION_CALLBACK = Type.getType(InjectionCallback.class);

    /**
     * Rewrite all Mixin callback references of the given method to {@link InjectionCallback}.
     * <p>
     * Rewrites the method descriptor and all instructions and local variables that mention
     * {@code CallbackInfo} or {@code CallbackInfoReturnable}. A generic signature is only rewritten for
     * {@code CallbackInfoReturnable}, because the signature rewriter looks for that type; a signature that
     * mentions plain {@code CallbackInfo} is left unchanged. The method node is modified in place.
     *
     * @param methodNode The transformer method to rewrite
     */
    static void rewrite(final MethodNode methodNode) {
        Type[] parameter = Type.getArgumentTypes(methodNode.desc);
        Type returnType = Type.getReturnType(methodNode.desc);

        boolean setDescriptor = false;
        for (int i = 0; i < parameter.length; i++) {
            Type type = parameter[i];
            if (type.equals(CALLBACK_INFO) || type.equals(CALLBACK_INFO_RETURNABLE)) {
                parameter[i] = INJECTION_CALLBACK;
                setDescriptor = true;
            }
        }
        if (returnType.equals(CALLBACK_INFO) || returnType.equals(CALLBACK_INFO_RETURNABLE)) {
            returnType = INJECTION_CALLBACK;
            setDescriptor = true;
        }

        if (setDescriptor) {
            methodNode.desc = Type.getMethodDescriptor(returnType, parameter);
            if (methodNode.signature != null) {
                while (true) {
                    String cirStart = "L" + CALLBACK_INFO_RETURNABLE.getInternalName();
                    int start = methodNode.signature.indexOf(cirStart);
                    if (start == -1) break;

                    String rest = methodNode.signature.substring(start + cirStart.length());
                    int index = 0;
                    int open = 0;
                    for (char c : rest.toCharArray()) {
                        index++;
                        if (c == ';' && open <= 0) break;
                        if (c == '<') open++;
                        else if (c == '>') open--;
                    }
                    methodNode.signature = methodNode.signature.substring(0, start) + INJECTION_CALLBACK.getDescriptor() + methodNode.signature.substring(start + cirStart.length() + index);
                }
            }
        }

        visitMethodInsn(methodNode);
        visitInstructions(methodNode);
        visitLocalVariables(methodNode);
    }

    private static void visitMethodInsn(final MethodNode methodNode) {
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (!(insn instanceof MethodInsnNode)) continue;
            MethodInsnNode method = (MethodInsnNode) insn;
            method.desc = replaceCallbackTypes(method.desc);
            boolean isCallbackInfo = method.owner.equals(CALLBACK_INFO.getInternalName());
            boolean isCallbackInfoReturnable = method.owner.equals(CALLBACK_INFO_RETURNABLE.getInternalName());

            if (!isCallbackInfo && !isCallbackInfoReturnable) continue;
            method.owner = INJECTION_CALLBACK.getInternalName();

            if (method.name.equals("cancel") && method.desc.equals("()V")) {
                method.name = "setCancelled";
                method.desc = "(Z)V";
                methodNode.instructions.insertBefore(method, new InsnNode(Opcodes.ICONST_1));
            }
        }
    }

    private static void visitInstructions(final MethodNode methodNode) {
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                field.desc = replaceCallbackTypes(field.desc);
            } else if (insn instanceof TypeInsnNode) {
                TypeInsnNode type = (TypeInsnNode) insn;
                if (type.desc.equals(CALLBACK_INFO.getInternalName()) || type.desc.equals(CALLBACK_INFO_RETURNABLE.getInternalName())) {
                    type.desc = INJECTION_CALLBACK.getInternalName();
                }
            } else if (insn instanceof LdcInsnNode) {
                LdcInsnNode ldc = (LdcInsnNode) insn;
                if (ldc.cst instanceof Type) {
                    Type type = (Type) ldc.cst;
                    if (type.equals(CALLBACK_INFO) || type.equals(CALLBACK_INFO_RETURNABLE)) ldc.cst = INJECTION_CALLBACK;
                }
            }
        }
    }

    private static void visitLocalVariables(final MethodNode methodNode) {
        if (methodNode.localVariables == null) return;
        for (LocalVariableNode localVariable : methodNode.localVariables) {
            Type type = Type.getType(localVariable.desc);
            if (type.equals(CALLBACK_INFO) || type.equals(CALLBACK_INFO_RETURNABLE)) localVariable.desc = INJECTION_CALLBACK.getDescriptor();
        }
    }

    private static String replaceCallbackTypes(final String descriptor) {
        return descriptor
                .replace(CALLBACK_INFO.getDescriptor(), INJECTION_CALLBACK.getDescriptor())
                .replace(CALLBACK_INFO_RETURNABLE.getDescriptor(), INJECTION_CALLBACK.getDescriptor());
    }

}
