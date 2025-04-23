package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CStub;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.types.RemovingAnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class CStubAnnotationHandler extends RemovingAnnotationHandler<CStub> {

    public CStubAnnotationHandler() {
        super(CStub.class);
    }

    @Override
    public void transform(CStub annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod) {
        if (!Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, true);
        }
        Type[] stubArgs = Type.getArgumentTypes(transformerMethod.desc);
        Type stubReturn = Type.getReturnType(transformerMethod.desc);

        MemberDeclaration memberDeclaration = ASMUtils.splitMemberDeclaration(annotation.value());
        if (memberDeclaration == null) {
            throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, annotation.value());
        }
        ClassNode stubOwner = null;
        if (annotation.memberValidation()) {
            try {
                stubOwner = ASMUtils.fromBytes(transformerManager.getClassProvider().getClass(memberDeclaration.getOwner()));
            } catch (Throwable t) {
                throw new TransformerException(transformerMethod, transformer, "has unknown stub class '" + memberDeclaration.getOwner() + "'");
            }
        }

        BiConsumer<MethodNode, InsnList> instructionGenerator;
        if (memberDeclaration.isFieldMapping()) {
            boolean isStatic;
            if (annotation.memberValidation()) {
                FieldNode field = ASMUtils.getField(stubOwner, memberDeclaration.getName(), memberDeclaration.getDesc());
                if (field == null) {
                    throw new TransformerException(transformerMethod, transformer, "has unknown stub field '" + memberDeclaration.getName() + "'");
                }
                isStatic = Modifier.isStatic(field.access);
                if (annotation.access() == CStub.Access.STATIC && !isStatic) {
                    throw new TransformerException(transformerMethod, transformer, "has static stub field '" + memberDeclaration.getName() + "' but the field is not static");
                } else if (annotation.access() == CStub.Access.NON_STATIC && isStatic) {
                    throw new TransformerException(transformerMethod, transformer, "has non-static stub field '" + memberDeclaration.getName() + "' but the field is static");
                }
            } else {
                if (annotation.access() == CStub.Access.AUTO) {
                    throw new TransformerException(transformerMethod, transformer, "has invalid access type AUTO");
                }
                isStatic = annotation.access() == CStub.Access.STATIC;
            }

            Type memberType = Type.getType(memberDeclaration.getDesc());
            if (isStatic) {
                if (stubArgs.length == 0 && ASMUtils.compareType(memberType, stubReturn)) {
                    //Option 1: return field type, no args -> getstatic
                    instructionGenerator = (method, instructions) -> {
                        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, memberDeclaration.getOwner(), memberDeclaration.getName(), memberDeclaration.getDesc()));
                        if (!stubReturn.equals(memberType)) {
                            instructions.add(ASMUtils.getCast(stubReturn));
                        }
                    };
                } else if (stubArgs.length == 1 && ASMUtils.compareType(memberType, stubArgs[0]) && stubReturn.equals(Type.VOID_TYPE)) {
                    //Option 2: void, 1 arg -> putstatic
                    instructionGenerator = (method, instructions) -> {
                        if (!stubArgs[0].equals(memberType)) {
                            instructions.add(ASMUtils.getCast(memberType));
                        }
                        instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, memberDeclaration.getOwner(), memberDeclaration.getName(), memberDeclaration.getDesc()));
                    };
                } else {
                    String getHelp = Codifier.get().access(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE).returnType(memberType).name(transformerMethod.name).build();
                    String setHelp = Codifier.get().access(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE).returnType(Type.VOID_TYPE).param(memberType).name(transformerMethod.name).build();
                    throw new TransformerException(transformerMethod, transformer, "has invalid signature")
                            .help("'" + getHelp + "' or '" + setHelp + "'");
                }
            } else {
                Type memberOwnerType = Type.getObjectType(memberDeclaration.getOwner());
                if (stubArgs.length == 1 && ASMUtils.compareType(memberOwnerType, stubArgs[0]) && ASMUtils.compareType(memberType, stubReturn)) {
                    //Option 1: return field type, 1 arg -> getfield
                    instructionGenerator = (method, instructions) -> {
                        if (!memberOwnerType.equals(stubArgs[0])) {
                            instructions.add(ASMUtils.getCast(memberOwnerType));
                        }
                        instructions.add(new FieldInsnNode(Opcodes.GETFIELD, memberDeclaration.getOwner(), memberDeclaration.getName(), memberDeclaration.getDesc()));
                        if (!stubReturn.equals(memberType)) {
                            instructions.add(ASMUtils.getCast(stubReturn));
                        }
                    };
                } else if (stubArgs.length == 2 && ASMUtils.compareType(memberOwnerType, stubArgs[0]) && ASMUtils.compareType(memberType, stubArgs[1]) && stubReturn.equals(Type.VOID_TYPE)) {
                    //Option 2: void, 2 arg -> setfield
                    instructionGenerator = (method, instructions) -> {
                        if (!memberOwnerType.equals(stubArgs[0])) {
                            instructions.add(ASMUtils.swap(stubArgs[0], stubArgs[1]));
                            instructions.add(ASMUtils.getCast(memberOwnerType));
                            instructions.add(ASMUtils.swap(stubArgs[1], memberOwnerType));
                        }
                        if (!memberType.equals(stubArgs[1])) {
                            instructions.add(ASMUtils.getCast(memberType));
                        }
                        instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, memberDeclaration.getOwner(), memberDeclaration.getName(), memberDeclaration.getDesc()));
                    };
                } else {
                    String getHelp = Codifier.get().access(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE).returnType(memberType).param(memberOwnerType).name(transformerMethod.name).build();
                    String setHelp = Codifier.get().access(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE).returnType(Type.VOID_TYPE).params(memberOwnerType, memberType).name(transformerMethod.name).build();
                    throw new TransformerException(transformerMethod, transformer, "has invalid parameters")
                            .help("'" + getHelp + "' or '" + setHelp + "'");
                }
            }
        } else {
            boolean isStatic;
            if (annotation.memberValidation()) {
                MethodNode method = ASMUtils.getMethod(stubOwner, memberDeclaration.getName(), memberDeclaration.getDesc());
                if (method == null) {
                    throw new TransformerException(transformerMethod, transformer, "has unknown stub method '" + memberDeclaration.getName() + "'");
                }
                isStatic = Modifier.isStatic(method.access);
                if (annotation.access() == CStub.Access.STATIC && !isStatic) {
                    throw new TransformerException(transformerMethod, transformer, "has static stub method '" + memberDeclaration.getName() + "' but the method is not static");
                } else if (annotation.access() == CStub.Access.NON_STATIC && isStatic) {
                    throw new TransformerException(transformerMethod, transformer, "has non-static stub method '" + memberDeclaration.getName() + "' but the method is static");
                }
            } else {
                if (annotation.access() == CStub.Access.AUTO) {
                    throw new TransformerException(transformerMethod, transformer, "has invalid access type AUTO");
                }
                isStatic = annotation.access() == CStub.Access.STATIC;
            }

            Type[] memberArgs = Type.getArgumentTypes(memberDeclaration.getDesc());
            Type memberReturn = Type.getReturnType(memberDeclaration.getDesc());
            if (isStatic) {
                if (ASMUtils.compareTypes(stubArgs, memberArgs) && ASMUtils.compareType(stubReturn, memberReturn)) {
                    instructionGenerator = (method, instructions) -> {
                        int currentArgIndex = ASMUtils.getFreeVarIndex(method);
                        int[] argsIndices = new int[stubArgs.length];
                        for (int i = stubArgs.length - 1; i >= 0; i--) {
                            argsIndices[i] = currentArgIndex;
                            instructions.add(new VarInsnNode(stubArgs[i].getOpcode(Opcodes.ISTORE), currentArgIndex));
                            currentArgIndex += stubArgs[i].getSize();
                        }

                        for (int i = 0; i < memberArgs.length; i++) {
                            instructions.add(new VarInsnNode(memberArgs[i].getOpcode(Opcodes.ILOAD), argsIndices[i]));
                            if (!memberArgs[i].equals(stubArgs[i])) {
                                instructions.add(ASMUtils.getCast(memberArgs[i]));
                            }
                        }

                        instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, memberDeclaration.getOwner(), memberDeclaration.getName(), memberDeclaration.getDesc()));
                    };

                } else {
                    throw new TransformerException(transformerMethod, transformer, "has invalid parameters")
                            .help(Codifier.get().access(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE).returnType(memberReturn).params(memberArgs).name(transformerMethod.name));
                }
            } else {
                Type memberOwnerType = Type.getObjectType(memberDeclaration.getOwner());
                if (ASMUtils.compareTypes(memberArgs, stubArgs, true, memberOwnerType) && ASMUtils.compareType(stubReturn, memberReturn)) {
                    instructionGenerator = (method, instructions) -> {
                        int currentArgIndex = ASMUtils.getFreeVarIndex(method);
                        int[] argsIndices = new int[stubArgs.length];
                        for (int i = stubArgs.length - 1; i >= 0; i--) {
                            argsIndices[i] = currentArgIndex;
                            instructions.add(new VarInsnNode(stubArgs[i].getOpcode(Opcodes.ISTORE), currentArgIndex));
                            currentArgIndex += stubArgs[i].getSize();
                        }

                        instructions.add(new VarInsnNode(memberOwnerType.getOpcode(Opcodes.ILOAD), argsIndices[0]));
                        if (!memberOwnerType.equals(stubArgs[0])) {
                            instructions.add(ASMUtils.getCast(memberOwnerType));
                        }
                        for (int i = 0; i < memberArgs.length; i++) {
                            instructions.add(new VarInsnNode(memberArgs[i].getOpcode(Opcodes.ILOAD), argsIndices[i + 1]));
                            if (!memberArgs[i].equals(stubArgs[i + 1])) {
                                instructions.add(ASMUtils.getCast(memberArgs[i]));
                            }
                        }

                        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, memberDeclaration.getOwner(), memberDeclaration.getName(), memberDeclaration.getDesc()));
                    };
                } else {
                    throw new TransformerException(transformerMethod, transformer, "has invalid parameters")
                            .help(Codifier.get().access(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE).returnType(memberReturn).param(memberOwnerType).params(memberArgs).name(transformerMethod.name));
                }
            }
        }

        for (MethodNode method : transformer.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                if (methodInsn.getOpcode() != Opcodes.INVOKESTATIC) continue;
                if (!methodInsn.owner.equals(transformer.name)) continue;
                if (!methodInsn.name.equals(transformerMethod.name)) continue;
                if (!methodInsn.desc.equals(transformerMethod.desc)) continue;

                InsnList instructions = new InsnList();
                instructionGenerator.accept(method, instructions);
                method.instructions.insert(instruction, instructions);
                method.instructions.remove(instruction);
            }
        }
    }

}
