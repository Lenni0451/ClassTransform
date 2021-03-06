package net.lenni0451.classtransform.utils.annotations;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationParser<T extends Annotation> {

    public static <T extends Annotation> T parse(final Class<T> type, final IClassProvider classProvider, final Map<String, Object> values) {
        return new AnnotationParser<>(type, classProvider).parse(values);
    }

    public static Map<String, Object> listToMap(final List<Object> list) {
        Map<String, Object> map = new HashMap<>();
        if (list != null) for (int i = 0; i < list.size(); i += 2) map.put((String) list.get(i), list.get(i + 1));
        return map;
    }

    public static List<Object> mapToList(final Map<String, Object> map) {
        List<Object> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            list.add(entry.getKey());
            list.add(entry.getValue());
        }
        return list;
    }


    private final Class<T> type;
    private final IClassProvider classProvider;

    private Map<String, Object> values;
    private List<String> initializedDefaultValues;
    private ClassNode node;

    public AnnotationParser(final Class<T> type, final IClassProvider classProvider) {
        this.type = type;
        this.classProvider = classProvider;
    }

    public T parse(final Map<String, Object> values) {
        this.initDefaults(values);
        this.defineBase();
        this.declareMethods();

        try {
            return ClassDefiner.<T>defineAnonymousClass(ASMUtils.toBytes(this.node, this.classProvider)).newInstance(new Class[]{IClassProvider.class}, new Object[]{this.classProvider});
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to create instance of '" + this.type.getName() + "'", t);
        }
    }


    private void initDefaults(final Map<String, Object> values) {
        this.initializedDefaultValues = new ArrayList<>();
        for (Method method : this.type.getDeclaredMethods()) {
            if (values.containsKey(method.getName())) continue;
            Object defaultValue = method.getDefaultValue();
            if (defaultValue == null) continue;
            values.put(method.getName(), defaultValue);
            this.initializedDefaultValues.add(method.getName());
        }

        this.values = values;
    }

    private void defineBase() {
        this.node = new ClassNode();
        this.node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, ClassDefiner.generateClassName("AnnotationWrapper"), null, "java/lang/Object", new String[]{Type.getInternalName(this.type), Type.getInternalName(IParsedAnnotation.class)});

        { //IClassProvider classProvider
            this.node.visitField(Opcodes.ACC_PRIVATE, "classProvider", Type.getDescriptor(IClassProvider.class), null, null).visitEnd();
        }

        { //<init>
            MethodVisitor constructor = this.node.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + Type.getDescriptor(IClassProvider.class) + ")V", null, null);
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(Opcodes.ALOAD, 1);
            constructor.visitFieldInsn(Opcodes.PUTFIELD, this.node.name, "classProvider", Type.getDescriptor(IClassProvider.class));
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitEnd();
        }
        { //equals
            MethodVisitor equals = this.node.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
            equals.visitInsn(Opcodes.ICONST_0);
            equals.visitInsn(Opcodes.IRETURN);
            equals.visitEnd();
        }
        { //hashCode
            MethodVisitor hashCode = this.node.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);
            hashCode.visitInsn(Opcodes.ICONST_0);
            hashCode.visitInsn(Opcodes.IRETURN);
            hashCode.visitEnd();
        }
        { //toString
            MethodVisitor toString = this.node.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
            toString.visitLdcInsn("AnnotationWrapper");
            toString.visitInsn(Opcodes.ARETURN);
            toString.visitEnd();
        }
        { //annotationType
            MethodVisitor annotationType = this.node.visitMethod(Opcodes.ACC_PUBLIC, "annotationType", "()Ljava/lang/Class;", null, null);
            annotationType.visitLdcInsn(Type.getType(this.type));
            annotationType.visitInsn(Opcodes.ARETURN);
            annotationType.visitEnd();
        }
        { //wasSet
            MethodVisitor wasSet = this.node.visitMethod(Opcodes.ACC_PUBLIC, "wasSet", "(Ljava/lang/String;)Z", null, null);
            for (String value : this.values.keySet()) {
                if (this.initializedDefaultValues.contains(value)) continue;

                Label jumpAfter = new Label();
                wasSet.visitVarInsn(Opcodes.ALOAD, 1);
                wasSet.visitLdcInsn(value);
                wasSet.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                wasSet.visitJumpInsn(Opcodes.IFEQ, jumpAfter);
                wasSet.visitInsn(Opcodes.ICONST_1);
                wasSet.visitInsn(Opcodes.IRETURN);
                wasSet.visitLabel(jumpAfter);
            }
            wasSet.visitInsn(Opcodes.ICONST_0);
            wasSet.visitInsn(Opcodes.IRETURN);
            wasSet.visitEnd();
        }
    }

    private void declareMethods() {
        for (Method method : this.type.getDeclaredMethods()) {
            MethodVisitor methodVisitor = this.node.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
            Object value = this.values.get(method.getName());
            this.visit(methodVisitor, method.getReturnType(), value);
            methodVisitor.visitInsn(ASMUtils.getReturnOpcode(Type.getReturnType(method)));
            methodVisitor.visitEnd();
        }
    }

    private void visit(final MethodVisitor methodVisitor, final Class<?> type, final Object value) {
        if (type.equals(boolean.class) || type.equals(Boolean.class)) this.visitBoolean(methodVisitor, value);
        else if (type.equals(byte.class) || type.equals(Byte.class)) this.visitByte(methodVisitor, value);
        else if (type.equals(short.class) || type.equals(Short.class)) this.visitShort(methodVisitor, value);
        else if (type.equals(char.class) || type.equals(Character.class)) this.visitChar(methodVisitor, value);
        else if (type.equals(int.class) || type.equals(Integer.class)) this.visitInt(methodVisitor, value);
        else if (type.equals(long.class) || type.equals(Long.class)) this.visitLong(methodVisitor, value);
        else if (type.equals(float.class) || type.equals(Float.class)) this.visitFloat(methodVisitor, value);
        else if (type.equals(double.class) || type.equals(Double.class)) this.visitDouble(methodVisitor, value);
        else if (type.equals(String.class)) this.visitString(methodVisitor, value);
        else if (type.equals(Class.class)) this.visitClass(methodVisitor, value);
        else if (type.isEnum()) this.visitEnum(methodVisitor, value);
        else if (type.isAnnotation() || type.equals(AnnotationNode.class)) this.visitAnnotation(methodVisitor, value);
        else if (type.isArray()) this.visitArray(methodVisitor, type.getComponentType(), value);
        else throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private void visitBoolean(final MethodVisitor methodVisitor, final Object value) {
        boolean b = (boolean) value;
        methodVisitor.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
    }

    private void visitByte(final MethodVisitor methodVisitor, final Object value) {
        byte b = (byte) value;
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, b);
    }

    private void visitShort(final MethodVisitor methodVisitor, final Object value) {
        short s = (short) value;
        methodVisitor.visitIntInsn(Opcodes.SIPUSH, s);
    }

    private void visitChar(final MethodVisitor methodVisitor, final Object value) {
        char c = (char) value;
        methodVisitor.visitIntInsn(Opcodes.SIPUSH, c);
    }

    private void visitInt(final MethodVisitor methodVisitor, final Object value) {
        int i = (int) value;
        methodVisitor.visitLdcInsn(i);
    }

    private void visitLong(final MethodVisitor methodVisitor, final Object value) {
        long l = (long) value;
        methodVisitor.visitLdcInsn(l);
    }

    private void visitFloat(final MethodVisitor methodVisitor, final Object value) {
        float f = (float) value;
        methodVisitor.visitLdcInsn(f);
    }

    private void visitDouble(final MethodVisitor methodVisitor, final Object value) {
        double d = (double) value;
        methodVisitor.visitLdcInsn(d);
    }

    private void visitString(final MethodVisitor methodVisitor, final Object value) {
        String s = (String) value;
        methodVisitor.visitLdcInsn(s);
    }

    private void visitClass(final MethodVisitor methodVisitor, final Object value) {
        if (value instanceof Class<?>) {
            Class<?> c = (Class<?>) value;
            methodVisitor.visitLdcInsn(Type.getType(c));
        } else if (value instanceof Type) {
            Type type = (Type) value;
            methodVisitor.visitLdcInsn(type);
        } else {
            throw new IllegalArgumentException("Unexpected value class for type 'Class': " + value.getClass());
        }
    }

    private void visitEnum(final MethodVisitor methodVisitor, final Object value) {
        if (value instanceof Enum<?>) {
            Enum<?> e = (Enum<?>) value;
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(e.getDeclaringClass()), e.name(), Type.getDescriptor(e.getDeclaringClass()));
        } else if (value instanceof String[]) {
            String[] enumValue = (String[]) value;
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getType(enumValue[0]).getInternalName(), enumValue[1], enumValue[0]);
        } else {
            throw new IllegalArgumentException("Unexpected value class for type 'Enum': " + value.getClass());
        }
    }

    private void visitAnnotation(final MethodVisitor methodVisitor, final Object value) {
        Type annotationType;
        if (value instanceof Annotation) annotationType = Type.getType(((Annotation) value).annotationType());
        else if (value instanceof AnnotationNode) annotationType = Type.getType(((AnnotationNode) value).desc);
        else throw new IllegalArgumentException("Unexpected value class for type 'Annotation': " + value.getClass());

        methodVisitor.visitLdcInsn(annotationType);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, this.node.name, "classProvider", Type.getDescriptor(IClassProvider.class));
        methodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(HashMap.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(HashMap.class), "<init>", "()V", false);
        if (value instanceof Annotation) {
            Annotation a = (Annotation) value;
            for (Method method : a.annotationType().getDeclaredMethods()) {
                if (!Modifier.isAbstract(method.getModifiers())) continue;

                Object returnValue;
                try {
                    returnValue = method.invoke(value);
                    if (returnValue == null) throw new IllegalArgumentException("Null return value for annotation member: " + method.getName());
                } catch (Throwable t) {
                    throw new IllegalStateException("Failed to invoke method '" + method.getName() + "' on annotation '" + a.annotationType().getName() + "'", t);
                }
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(method.getName());
                this.visit(methodVisitor, method.getReturnType(), returnValue);
                this.visitPrimitiveWrap(methodVisitor, method.getReturnType());
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                methodVisitor.visitInsn(Opcodes.POP);
            }
        } else {
            AnnotationNode a = (AnnotationNode) value;
            Map<String, Object> map = listToMap(a.values);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(entry.getKey());
                this.visit(methodVisitor, entry.getValue().getClass(), entry.getValue());
                this.visitPrimitiveWrap(methodVisitor, entry.getValue().getClass());
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                methodVisitor.visitInsn(Opcodes.POP);
            }
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(AnnotationParser.class), "parse", "(Ljava/lang/Class;Lnet/lenni0451/classtransform/utils/tree/IClassProvider;Ljava/util/Map;)Ljava/lang/annotation/Annotation;", false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, annotationType.getInternalName());
    }

    private void visitArray(final MethodVisitor methodVisitor, final Class<?> arrayType, final Object value) {
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, array.length);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(arrayType));
            for (int i = 0; i < array.length; i++) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(i);
                this.visit(methodVisitor, arrayType, array[i]);
                methodVisitor.visitInsn(Opcodes.AASTORE);
            }
        } else if (value instanceof List) {
            List<?> array = (List<?>) value;
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, array.size());
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(arrayType));
            for (int i = 0; i < array.size(); i++) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(i);
                this.visit(methodVisitor, arrayType, array.get(i));
                methodVisitor.visitInsn(Opcodes.AASTORE);
            }
        } else {
            throw new IllegalArgumentException("Unexpected value class for type 'Array': " + value.getClass());
        }
    }

    private void visitPrimitiveWrap(final MethodVisitor methodVisitor, final Class<?> type) {
        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Boolean.class), "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Byte.class), "valueOf", "(B)Ljava/lang/Byte;", false);
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Short.class), "valueOf", "(S)Ljava/lang/Short;", false);
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Character.class), "valueOf", "(C)Ljava/lang/Character;", false);
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class), "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class), "valueOf", "(F)Ljava/lang/Float;", false);
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Double.class), "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (type.equals(String.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(String.class), "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;", false);
        }
    }

}
