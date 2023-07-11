package net.lenni0451.classtransform.transformer.impl.general;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CASM;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

import static net.lenni0451.classtransform.utils.Types.*;

/**
 * Copy members from the transformer class to the transformed class.<br>
 * This also merges initializers.
 */
public class MemberCopyGeneralHandler extends AnnotationHandler {

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        this.mergeInitializers(transformedClass, transformer);
        this.mergeMethods(transformedClass, transformer);
        this.mergeFields(transformedClass, transformer);
        this.mergeInterfaces(transformedClass, transformer);
    }

    private void mergeInitializers(ClassNode transformedClass, ClassNode transformer) {
        Map<MethodNode, MethodNode> initializers = new HashMap<>();
        List<MethodNode> unresolvedInitializers = new ArrayList<>();
        for (MethodNode method : transformedClass.methods) {
            if (!method.name.equals(MN_Init)) continue;

            MethodNode targetMethod = ASMUtils.getMethod(transformer, method.name, method.desc);
            if (targetMethod != null) initializers.put(targetMethod, method);
            else unresolvedInitializers.add(method);
        }
        if (!unresolvedInitializers.isEmpty()) {
            MethodNode emptyConstructor = ASMUtils.getMethod(transformer, MN_Init, MD_Void);
            if (emptyConstructor == null) throw new IllegalStateException("Unable to merge all constructors in target class '" + transformedClass.name + "'");
            for (MethodNode unresolvedInitializer : unresolvedInitializers) initializers.put(emptyConstructor, unresolvedInitializer);
        }
        MethodNode staticBlock = ASMUtils.getMethod(transformer, MN_Clinit, MD_Void);
        if (staticBlock != null) initializers.put(staticBlock, this.createStaticBlock(transformedClass));
        for (Map.Entry<MethodNode, MethodNode> entry : initializers.entrySet()) this.copyInitializers(transformer, entry.getKey(), transformedClass, entry.getValue());
    }

    private void mergeMethods(final ClassNode transformedClass, final ClassNode transformer) {
        for (MethodNode method : transformer.methods) {
            if (method.name.startsWith("<")) continue;
            if (AnnotationUtils.hasInvisibleAnnotation(method, CASM.class)) continue; //Special case for CASM bottom handler
            if (ASMUtils.getMethod(transformedClass, method.name, method.desc) != null) {
                throw new IllegalStateException("Method '" + method.name + method.desc + "' from transformer '" + transformer.name + "' already exists in class '" + transformedClass.name + "' and does not override it");
            }
            Remapper.remapAndAdd(transformer, transformedClass, method);
        }
    }

    private void mergeFields(final ClassNode transformedClass, final ClassNode transformer) {
        for (FieldNode field : transformer.fields) {
            if (ASMUtils.getField(transformedClass, field.name, field.desc) != null) {
                throw new IllegalStateException("Field '" + field.name + field.desc + "' from transformer '" + transformer.name + "' already exists in class '" + transformedClass.name + "'");
            }
            Remapper.remapAndAdd(transformer, transformedClass, field);
        }
    }

    private void mergeInterfaces(final ClassNode transformedClass, final ClassNode transformer) {
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
            if (from.name.equals(MN_Init)) first = ASMUtils.getFirstConstructorInstruction(fromClass.superName, from);
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
        tempClassHolder.visit(0, 0, "temp", null, IN_Object, null);
        MethodNode tempMethodHolder = new MethodNode(0, "temp", MD_Void, null, null);
        tempMethodHolder.instructions = instructions;
        Remapper.remapAndAdd(fromName, toName, tempClassHolder, tempMethodHolder);
        return tempClassHolder.methods.get(0).instructions;
    }

    private MethodNode createStaticBlock(final ClassNode transformedClass) {
        for (MethodNode method : transformedClass.methods) {
            if (method.name.equals("<clinit>")) return method;
        }

        MethodVisitor staticBlock = transformedClass.visitMethod(Opcodes.ACC_STATIC, MN_Clinit, MD_Void, null, null);
        staticBlock.visitCode();
        staticBlock.visitInsn(Opcodes.RETURN);
        staticBlock.visitEnd();

        return (MethodNode) staticBlock;
    }

}
