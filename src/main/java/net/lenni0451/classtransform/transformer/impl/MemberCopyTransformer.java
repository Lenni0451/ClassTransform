package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.ATransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Remapper;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

public class MemberCopyTransformer extends ATransformer {

    @Override
    public void transform(TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer) {
        for (MethodNode method : transformer.methods) {
            boolean isStaticBlock = method.name.equals("<clinit>");
            if (isStaticBlock) this.createStaticBlock(transformedClass);
            if (method.name.equals("<init>") || isStaticBlock) {
                for (MethodNode targetMethod : transformedClass.methods) {
                    if (targetMethod.name.equals(method.name) && targetMethod.desc.equals(method.desc)) this.copyInitializers(transformer, method, transformedClass, targetMethod);
                }
            }
            if (method.name.startsWith("<")) continue;
            if (ASMUtils.getMethod(transformedClass, method.name, method.desc) != null) {
                throw new IllegalStateException("Method '" + method.name + method.desc + "' from transformer '" + transformer.name + "' already exists in class '" + transformedClass.name + "' and does not override it");
            }
            Remapper.remapAndAdd(transformer, transformedClass, method);
        }
        for (FieldNode field : transformer.fields) {
            if (ASMUtils.getField(transformedClass, field.name, field.desc) != null) {
                throw new IllegalStateException("Field '" + field.name + field.desc + "' from transformer '" + transformer.name + "' already exists in class '" + transformedClass.name + "'");
            }
            Remapper.remapAndAdd(transformer, transformedClass, field);
        }
        if (transformer.interfaces != null) {
            List<String> interfaces = transformedClass.interfaces;
            if (interfaces == null) interfaces = transformedClass.interfaces = new ArrayList<>();
            for (String anInterface : transformer.interfaces) {
                if (!interfaces.contains(anInterface)) interfaces.add(anInterface);
            }
        }
    }

    private void copyInitializers(final ClassNode fromClass, final MethodNode from, final ClassNode toClass, final MethodNode to) {
        Map<String, InsnList> fieldInitializers = new LinkedHashMap<>();
        InsnList lastInstructions = new InsnList();
        Map<LabelNode, LabelNode> copiedLabels = new HashMap<>();
        for (AbstractInsnNode instruction : from.instructions) {
            if (instruction instanceof LabelNode) copiedLabels.put((LabelNode) instruction, new LabelNode());
        }

        {
            AbstractInsnNode first;
            if (from.name.equals("<init>")) first = ASMUtils.getFirstConstructorInstruction(fromClass.superName, from);
            else first = from.instructions.getFirst();
            if (first == null) return;

            for (int i = from.instructions.indexOf(first); i < from.instructions.size(); i++) {
                AbstractInsnNode instruction = from.instructions.get(i);
                if (instruction instanceof LineNumberNode || instruction instanceof FrameNode) continue;
                lastInstructions.add(instruction.clone(copiedLabels));

                if (instruction instanceof FieldInsnNode && (instruction.getOpcode() == Opcodes.PUTFIELD || instruction.getOpcode() == Opcodes.PUTSTATIC)) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                    fieldInitializers.put(fieldInsn.owner + ":" + fieldInsn.name + fieldInsn.desc, lastInstructions);
                    lastInstructions = new InsnList();
                }
            }
            if (lastInstructions.size() != 0) fieldInitializers.put(null, lastInstructions);
        }
        {
            for (AbstractInsnNode instruction : to.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode && (instruction.getOpcode() == Opcodes.PUTFIELD || instruction.getOpcode() == Opcodes.PUTSTATIC)) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                    InsnList insns = fieldInitializers.remove(fieldInsn.owner + ":" + fieldInsn.name + fieldInsn.desc);
                    if (insns == null) continue;

                    to.instructions.insert(instruction, this.remapInstructions(insns, fromClass.name, toClass.name));
                }
            }
            for (AbstractInsnNode instruction : to.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.RETURN) {
                    for (InsnList instructions : fieldInitializers.values()) {
                        to.instructions.insertBefore(instruction, this.remapInstructions(instructions, fromClass.name, toClass.name));
                    }
                }
            }
        }
    }

    private InsnList remapInstructions(final InsnList instructions, final String fromName, final String toName) {
        ClassNode tempClassHolder = new ClassNode();
        tempClassHolder.visit(0, 0, "temp", null, "java/lang/Object", null);
        MethodNode tempMethodHolder = new MethodNode(0, "temp", "()V", null, null);
        tempMethodHolder.instructions = instructions;
        Remapper.remapAndAdd(fromName, toName, tempClassHolder, tempMethodHolder);
        return tempClassHolder.methods.get(0).instructions;
    }

    private void createStaticBlock(final ClassNode transformedClass) {
        for (MethodNode method : transformedClass.methods) {
            if (method.name.equals("<clinit>")) return;
        }

        MethodVisitor staticBlock = transformedClass.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        staticBlock.visitCode();
        staticBlock.visitInsn(Opcodes.RETURN);
        staticBlock.visitEnd();
    }

}
