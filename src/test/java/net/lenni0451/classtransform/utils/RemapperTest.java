package net.lenni0451.classtransform.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemapperTest {

    @Test
    @DisplayName("Remap method")
    public void remapMethod() {
        String oldName = "net.lenni0451.classtransform.utils.Remapper";
        String newName = "net.lenni0451.classtransform.utils.RemapperTest";
        ClassNode holder = new ClassNode();
        MethodNode methodNode = new MethodNode(0, "test", "()V", null, null);
        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, oldName, "test", "()V"));
        methodNode.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, oldName, "test", "B"));

        Remapper.remapAndAdd(oldName, newName, holder, methodNode);

        assertEquals(1, holder.methods.size());
        MethodNode newMethodNode = holder.methods.get(0);
        assertEquals(2, newMethodNode.instructions.size());
        assertEquals(Opcodes.INVOKESTATIC, newMethodNode.instructions.get(0).getOpcode());
        assertEquals(Opcodes.GETSTATIC, newMethodNode.instructions.get(1).getOpcode());

        assertEquals(newName, ((MethodInsnNode) newMethodNode.instructions.get(0)).owner);
        assertEquals(newName, ((FieldInsnNode) newMethodNode.instructions.get(1)).owner);
    }

    @Test
    @DisplayName("Remap field")
    public void remapField() {
        String oldName = "net.lenni0451.classtransform.utils.Remapper";
        String oldDescriptor = "L" + oldName + ";";
        String newName = "net.lenni0451.classtransform.utils.RemapperTest";
        String newDescriptor = "L" + newName + ";";
        String descriptor = "LTest;";
        ClassNode holder = new ClassNode();
        FieldNode fieldNode = new FieldNode(0, "test", "Z", null, null);
        {
            AnnotationNode annotation = new AnnotationNode(descriptor);
            annotation.values = new ArrayList<>();
            annotation.values.add("test");
            annotation.values.add(Type.getType(oldDescriptor));
            fieldNode.visibleAnnotations = Collections.singletonList(annotation);
        }
        {
            AnnotationNode annotation = new AnnotationNode(descriptor);
            annotation.values = new ArrayList<>();
            annotation.values.add("test");
            annotation.values.add(Type.getType(oldDescriptor));
            fieldNode.invisibleAnnotations = Collections.singletonList(annotation);
        }

        Remapper.remapAndAdd(oldName, newName, holder, fieldNode);

        assertEquals(1, holder.fields.size());
        FieldNode newFieldNode = holder.fields.get(0);
        { //visible
            assertEquals(1, newFieldNode.visibleAnnotations.size());
            AnnotationNode annotation = newFieldNode.visibleAnnotations.get(0);
            assertEquals(descriptor, annotation.desc);
            assertEquals(2, annotation.values.size());
            assertEquals("test", annotation.values.get(0));
            assertEquals(Type.getType(newDescriptor), annotation.values.get(1));
        }
        { //invisible
            assertEquals(1, newFieldNode.invisibleAnnotations.size());
            AnnotationNode annotation = newFieldNode.invisibleAnnotations.get(0);
            assertEquals(descriptor, annotation.desc);
            assertEquals(2, annotation.values.size());
            assertEquals("test", annotation.values.get(0));
            assertEquals(Type.getType(newDescriptor), annotation.values.get(1));
        }
    }

    @Test
    @DisplayName("Remap class")
    public void remapClass() {
        String oldName = "net/lenni0451/classtransform/utils/Remapper";
        String newName = "net/lenni0451/classtransform/utils/RemapperTest";
        ClassNode node = new ClassNode();
        node.visit(0, 0, oldName, null, "java/lang/Object", null);

        MethodNode method = new MethodNode(0, "test", "()V", null, null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, oldName, "test", "Z"));
        node.methods.add(method);

        node = Remapper.remap(oldName, newName, node);

        assertEquals(1, node.methods.size());
        MethodNode newMethod = node.methods.get(0);
        assertEquals(1, newMethod.instructions.size());
        assertEquals(Opcodes.GETSTATIC, newMethod.instructions.get(0).getOpcode());
        assertEquals(newName, ((FieldInsnNode) newMethod.instructions.get(0)).owner);
    }

    @Test
    @DisplayName("Merge classes")
    public void mergeClasses() {
        ClassNode node1 = new ClassNode();
        node1.visit(12, 34, "net/lenni0451/classtransform/utils/Remapper", null, "java/lang/Object", new String[]{"Test"});
        ClassNode node2 = new ClassNode();

        Remapper.merge(node1, node2);

        assertEquals(node1.version, node2.version);
        assertEquals(node1.access, node2.access);
        assertEquals(node1.name, node2.name);
        assertEquals(node1.signature, node2.signature);
        assertEquals(node1.superName, node2.superName);
        assertEquals(node1.interfaces, node2.interfaces);
    }

}