package net.lenni0451.classtransform.transformer.impl.general.membercopy;

import net.lenni0451.classtransform.utils.ASMComparator;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

import static net.lenni0451.classtransform.utils.Types.*;

public class InitializerMerger {

    public static void mergeInitializers(ClassNode transformedClass, ClassNode transformer) {
        Map<String, InsnList> fieldInitializers = null;
        for (MethodNode method : transformer.methods) {
            if (!method.name.equals(MN_Init)) continue;

            Map<String, InsnList> initializers = getFieldInitializers(transformer, method);
            if (fieldInitializers == null) {
                fieldInitializers = initializers;
            } else {
                //Check for any differences in the field initializers
                Set<String> diff = diff(fieldInitializers.keySet(), initializers.keySet());
                if (!diff.isEmpty()) {
                    //Check if there are more/less field initializers
                    throw new IllegalStateException("Class " + transformer.name + " has different field initializers in multiple constructor methods. Field differences: " + diff);
                }
                //Compare the initializer instructions
                for (String fieldName : fieldInitializers.keySet()) {
                    InsnList instructions1 = fieldInitializers.get(fieldName);
                    InsnList instructions2 = initializers.get(fieldName);
                    if (!ASMComparator.equals(instructions1, instructions2)) {
                        throw new IllegalStateException("Class " + transformer.name + " has different field initializers in multiple constructor methods. Field instruction difference: " + fieldName);
                    }
                }
            }
        }
        if (fieldInitializers != null && !fieldInitializers.isEmpty()) {
            for (MethodNode method : transformedClass.methods) {
                if (!method.name.equals(MN_Init)) continue;

                mergeFieldInitializers(transformer, transformedClass, method, fieldInitializers);
            }
        }

        MethodNode staticBlock = ASMUtils.getMethod(transformer, MN_Clinit, MD_Void);
        if (staticBlock != null) mergeFieldInitializers(transformer, transformedClass, getOrCreateClinit(transformedClass), getFieldInitializers(transformer, staticBlock));
    }

    private static Map<String, InsnList> getFieldInitializers(final ClassNode owner, final MethodNode method) {
        AbstractInsnNode firstInstruction;
        if (method.name.equals(MN_Init)) firstInstruction = ASMUtils.getFirstConstructorInstruction(owner.superName, method);
        else firstInstruction = method.instructions.getFirst();
        if (firstInstruction == null) return Collections.emptyMap();

        Map<String, InsnList> fieldInitializers = new LinkedHashMap<>();
        InsnList lastInstructions = new InsnList();
        Map<LabelNode, LabelNode> labels = ASMUtils.cloneLabels(method.instructions);
        for (int i = method.instructions.indexOf(firstInstruction); i < method.instructions.size(); i++) {
            AbstractInsnNode instruction = method.instructions.get(i);
            if (instruction instanceof LineNumberNode || instruction instanceof FrameNode) continue;
            lastInstructions.add(instruction.clone(labels));

            if (instruction instanceof FieldInsnNode && (instruction.getOpcode() == Opcodes.PUTFIELD || instruction.getOpcode() == Opcodes.PUTSTATIC)) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                if (!fieldInsnNode.owner.equals(owner.name)) {
                    throw new IllegalStateException("Can't copy field initializer from " + owner.nestHostClass + " of field belonging to " + fieldInsnNode.owner + " to another class. Please use @CInject into the constructor instead.");
                }
                fieldInitializers.put(fieldInsnNode.name + ":" + fieldInsnNode.desc, lastInstructions);
                lastInstructions = new InsnList();
            }
        }
        return fieldInitializers;
    }

    private static void mergeFieldInitializers(final ClassNode source, final ClassNode target, final MethodNode initializer, final Map<String, InsnList> fieldInitializers) {
        if (fieldInitializers.isEmpty()) return;

        AbstractInsnNode firstInstruction;
        if (initializer.name.equals(MN_Init)) firstInstruction = ASMUtils.getFirstConstructorInstruction(target.superName, initializer);
        else firstInstruction = initializer.instructions.getFirst();
        if (firstInstruction == null) return;

        for (AbstractInsnNode instruction : initializer.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode && (instruction.getOpcode() == Opcodes.PUTFIELD || instruction.getOpcode() == Opcodes.PUTSTATIC)) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                if (!fieldInsnNode.owner.equals(target.name)) continue;

                InsnList instructions = fieldInitializers.remove(fieldInsnNode.name + ":" + fieldInsnNode.desc);
                if (instructions == null) continue;

                //Copy all replaced field initializers after the original field initializer
                initializer.instructions.insert(instruction, remapInstructions(instructions, source.name, target.name));
            }
        }
        //Copy all remaining field initializers after the first instruction
        for (InsnList instructions : fieldInitializers.values()) {
            initializer.instructions.insert(firstInstruction, remapInstructions(instructions, source.name, target.name));
        }
    }

    private static InsnList remapInstructions(final InsnList instructions, final String fromName, final String toName) {
        ClassNode tempClassHolder = new ClassNode();
        tempClassHolder.visit(0, 0, "temp", null, IN_Object, null);
        MethodNode tempMethodHolder = new MethodNode(0, "temp", MD_Void, null, null);
        tempMethodHolder.instructions = instructions;
        Remapper.remapAndAdd(fromName, toName, tempClassHolder, tempMethodHolder);
        return tempClassHolder.methods.get(0).instructions;
    }

    private static MethodNode getOrCreateClinit(final ClassNode transformedClass) {
        for (MethodNode method : transformedClass.methods) {
            if (method.name.equals(MN_Clinit)) return method;
        }

        MethodVisitor staticBlock = transformedClass.visitMethod(Opcodes.ACC_STATIC, MN_Clinit, MD_Void, null, null);
        staticBlock.visitCode();
        staticBlock.visitInsn(Opcodes.RETURN);
        staticBlock.visitEnd();

        return (MethodNode) staticBlock;
    }

    private static Set<String> diff(final Set<String> set1, final Set<String> set2) {
        Set<String> diff = new HashSet<>();
        for (String s : set1) {
            if (!set2.contains(s)) diff.add(s);
        }
        for (String s : set2) {
            if (!set1.contains(s)) diff.add(s);
        }
        return diff;
    }

}
