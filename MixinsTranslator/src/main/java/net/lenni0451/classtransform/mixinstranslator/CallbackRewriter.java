package net.lenni0451.classtransform.mixinstranslator;

import net.lenni0451.classtransform.InjectionCallback;
import net.lenni0451.classtransform.utils.parser.StringReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class CallbackRewriter {

    private static final Type CALLBACK_INFO = Type.getType(CallbackInfo.class);
    private static final Type CALLBACK_INFO_RETURNABLE = Type.getType(CallbackInfoReturnable.class);
    private static final Type INJECTION_CALLBACK = Type.getType(InjectionCallback.class);

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
                    StringReader reader = new StringReader(rest);
                    int open = 0;
                    char c;
                    while ((c = reader.read()) != ';' || open > 0) {
                        if (c == '<') open++;
                        else if (c == '>') open--;
                    }
                    methodNode.signature = methodNode.signature.substring(0, start) + INJECTION_CALLBACK.getDescriptor() + methodNode.signature.substring(start + cirStart.length() + reader.getCursor());
                }
            }
        }

        visitMethodInsn(methodNode);

    }

    private static void visitMethodInsn(final MethodNode methodNode) {
        for (AbstractInsnNode insn : methodNode.instructions.toArray()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode method = (MethodInsnNode) insn;
                if (method.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    boolean isCallbackInfo = method.owner.equals(CALLBACK_INFO.getInternalName());
                    boolean isCallbackInfoReturnable = method.owner.equals(CALLBACK_INFO_RETURNABLE.getInternalName());

                    if (isCallbackInfo || isCallbackInfoReturnable) {
                        method.owner = INJECTION_CALLBACK.getInternalName();

                        if (method.name.equals("cancel") && method.desc.equals("()V")) { //cancel -> setCancelled(true)
                            method.name = "setCancelled";
                            method.desc = "(Z)V";
                            methodNode.instructions.insertBefore(method, new InsnNode(Opcodes.ICONST_1));
                        }
                    }
                    if (isCallbackInfoReturnable) {
                        if (method.name.equals("getReturnValueB") && method.desc.equals("()B")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B"));
                        } else if (method.name.equals("getReturnValueC") && method.desc.equals("()C")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false));
                        } else if (method.name.equals("getReturnValueD") && method.desc.equals("()D")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
                        } else if (method.name.equals("getReturnValueF") && method.desc.equals("()F")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
                        } else if (method.name.equals("getReturnValueI") && method.desc.equals("()I")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                        } else if (method.name.equals("getReturnValueJ") && method.desc.equals("()J")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false));
                        } else if (method.name.equals("getReturnValueS") && method.desc.equals("()S")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false));
                        } else if (method.name.equals("getReturnValueZ") && method.desc.equals("()Z")) {
                            methodNode.instructions.insert(method, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
                        }
                    }
                }
            }
        }
    }

}
