package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CRecordComponent;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Types;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ParametersAreNonnullByDefault
public class CRecordComponentAnnotationHandler extends AnnotationHandler {

    private static final String OBJECTMETHODS_BOOTSTRAP_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;";

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        List<FieldNode> recordComponents = new ArrayList<>();
        List<FieldNode> constructor = new ArrayList<>();
        List<FieldNode> getter = new ArrayList<>();
        List<FieldNode> toString = new ArrayList<>();
        List<FieldNode> equals = new ArrayList<>();
        List<FieldNode> hashCode = new ArrayList<>();
        Iterator<FieldNode> it = transformer.fields.iterator();
        while (it.hasNext()) {
            FieldNode field = it.next();
            CRecordComponent recordComponent = this.getAnnotation(CRecordComponent.class, field, transformerManager);
            if (recordComponent == null) continue;
            this.copyField(transformer, transformedClass, field);
            it.remove();

            if (recordComponent.addRecordComponent()) recordComponents.add(field);
            if (recordComponent.addConstructor()) constructor.add(field);
            if (recordComponent.addGetter()) getter.add(field);
            if (recordComponent.addToString()) toString.add(field);
            if (recordComponent.addEquals()) equals.add(field);
            if (recordComponent.addHashCode()) hashCode.add(field);
        }
        if (!recordComponents.isEmpty()) this.addRecordComponents(transformedClass, recordComponents);
        if (!constructor.isEmpty()) this.addConstructor(transformer, transformedClass, constructor);
        if (!getter.isEmpty()) this.addGetter(transformer, transformedClass, getter);
        if (!toString.isEmpty()) this.addToString(transformedClass, toString);
        if (!equals.isEmpty()) this.addEquals(transformedClass, equals);
        if (!hashCode.isEmpty()) this.addHashCode(transformedClass, hashCode);
    }

    private void copyField(final ClassNode transformer, final ClassNode transformedClass, final FieldNode field) {
        if (ASMUtils.getField(transformedClass, field.name, field.desc) != null) throw TransformerException.alreadyExists(field, transformer, transformedClass);
        this.prepareForCopy(transformer, field);
        Remapper.remapAndAdd(transformer, transformedClass, field);
    }

    private void addRecordComponents(final ClassNode transformedClass, final List<FieldNode> fields) {
        if (transformedClass.recordComponents == null) transformedClass.recordComponents = new ArrayList<>();
        for (FieldNode field : fields) transformedClass.recordComponents.add(new RecordComponentNode(field.name, field.desc, field.signature));
    }

    private void addConstructor(final ClassNode transformer, final ClassNode transformedClass, final List<FieldNode> fields) {
        List<RecordComponentNode> oldRecordComponents = transformedClass.recordComponents.subList(0, transformedClass.recordComponents.size() - fields.size());
        List<RecordComponentNode> newRecordComponents = transformedClass.recordComponents;
        String mainDescriptor = Types.methodDescriptor(void.class, oldRecordComponents.stream().map(comp -> comp.descriptor).toArray(Object[]::new));
        String newDescriptor = Types.methodDescriptor(void.class, newRecordComponents.stream().map(comp -> comp.descriptor).toArray(Object[]::new));

        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", newDescriptor, null, null);
        if (ASMUtils.getMethod(transformedClass, constructor.name, constructor.desc) != null) throw TransformerException.alreadyExists(constructor, transformer, transformedClass);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        int varIndex = 1;
        for (RecordComponentNode component : oldRecordComponents) {
            Type type = Types.type(component.descriptor);
            constructor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), varIndex);
            varIndex += type.getSize();
        }
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, transformedClass.name, "<init>", mainDescriptor, false);
        for (FieldNode field : fields) {
            Type type = Types.type(field.desc);
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), varIndex);
            constructor.visitFieldInsn(Opcodes.PUTFIELD, transformedClass.name, field.name, field.desc);
            varIndex += type.getSize();
        }
        constructor.visitInsn(Opcodes.RETURN);

        transformedClass.methods.add(constructor);
    }

    private void addGetter(final ClassNode transformer, final ClassNode transformedClass, final List<FieldNode> fields) {
        for (FieldNode field : fields) {
            Type type = Types.type(field.desc);

            MethodNode getter = new MethodNode(Opcodes.ACC_PUBLIC, field.name, "()" + field.desc, null, null);
            if (ASMUtils.getMethod(transformedClass, field.name, "()" + field.desc) != null) throw TransformerException.alreadyExists(getter, transformer, transformedClass);
            getter.visitVarInsn(Opcodes.ALOAD, 0);
            getter.visitFieldInsn(Opcodes.GETFIELD, transformedClass.name, field.name, field.desc);
            getter.visitInsn(type.getOpcode(Opcodes.IRETURN));

            transformedClass.methods.add(getter);
        }
    }

    private void addToString(final ClassNode transformedClass, final List<FieldNode> fields) {
        this.addFieldsToDefaults(transformedClass, fields, "toString", "()Ljava/lang/String;");
    }

    private void addHashCode(final ClassNode transformedClass, final List<FieldNode> fields) {
        this.addFieldsToDefaults(transformedClass, fields, "hashCode", "()I");
    }

    private void addEquals(final ClassNode transformedClass, final List<FieldNode> fields) {
        this.addFieldsToDefaults(transformedClass, fields, "equals", "(Ljava/lang/Object;)Z");
    }


    private InvokeDynamicInsnNode getDefaultMethod(final ClassNode node, final String name, final String descriptor) {
        for (MethodNode method : node.methods) {
            if (!method.name.equals(name)) continue;
            if (!method.desc.equals(descriptor)) continue;

            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof InvokeDynamicInsnNode)) continue;
                InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) instruction;
                if (!invokeDynamic.bsm.getOwner().equals("java/lang/runtime/ObjectMethods")) continue;
                if (!invokeDynamic.bsm.getName().equals("bootstrap")) continue;
                if (!invokeDynamic.bsm.getDesc().equals(OBJECTMETHODS_BOOTSTRAP_DESC)) continue;
                return invokeDynamic;
            }
        }
        return null;
    }

    private void addFieldsToDefaults(final ClassNode transformedClass, final List<FieldNode> fields, final String name, final String descriptor) {
        InvokeDynamicInsnNode invokeDynamic = this.getDefaultMethod(transformedClass, name, descriptor);
        if (invokeDynamic == null) return;

        Object[] newArgs = new Object[invokeDynamic.bsmArgs.length + fields.size()];
        System.arraycopy(invokeDynamic.bsmArgs, 0, newArgs, 0, invokeDynamic.bsmArgs.length);
        for (int i = 0; i < fields.size(); i++) {
            FieldNode field = fields.get(i);
            Handle handle = new Handle(Opcodes.H_GETFIELD, transformedClass.name, field.name, field.desc, Modifier.isInterface(transformedClass.access));
            newArgs[invokeDynamic.bsmArgs.length + i] = handle;

            String fieldNames = (String) newArgs[1];
            newArgs[1] = fieldNames + (fieldNames.isEmpty() ? "" : ";") + field.name;
        }
        invokeDynamic.bsmArgs = newArgs;
    }

}
