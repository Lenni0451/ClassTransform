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

import static net.lenni0451.classtransform.utils.Types.*;

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

    public static boolean hasAnnotation(final List<AnnotationNode> nodes, final String desc) {
        if (nodes == null) return false;
        for (AnnotationNode annotation : nodes) {
            if (annotation.desc.equals(desc)) return true;
        }
        return false;
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
            return ClassDefiner.
                    <T>defineAnonymousClass(ASMUtils.toBytes(this.node, this.classProvider))
                    .newInstance(new Class[]{IClassProvider.class, Map.class}, new Object[]{this.classProvider, this.values});
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
        this.node.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, ClassDefiner.generateClassName("AnnotationWrapper"), null, IN_Object, new String[]{internalName(this.type), internalName(IParsedAnnotation.class)});

        { //fields
            this.node.visitField(Opcodes.ACC_PRIVATE, "classProvider", typeDescriptor(IClassProvider.class), null, null).visitEnd();
            this.node.visitField(Opcodes.ACC_PRIVATE, "values", typeDescriptor(Map.class), null, null).visitEnd();
        }

        { //<init>
            MethodVisitor constructor = this.node.visitMethod(Opcodes.ACC_PUBLIC, MN_Init, methodDescriptor(void.class, IClassProvider.class, Map.class), null, null);
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, IN_Object, MN_Init, MD_Void, false);

            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(Opcodes.ALOAD, 1);
            constructor.visitFieldInsn(Opcodes.PUTFIELD, this.node.name, "classProvider", typeDescriptor(IClassProvider.class));

            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(Opcodes.ALOAD, 2);
            constructor.visitFieldInsn(Opcodes.PUTFIELD, this.node.name, "values", typeDescriptor(Map.class));

            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitEnd();
        }
        { //equals
            MethodVisitor equals = this.node.visitMethod(Opcodes.ACC_PUBLIC, "equals", methodDescriptor(boolean.class, Object.class), null, null);
            equals.visitInsn(Opcodes.ICONST_0);
            equals.visitInsn(Opcodes.IRETURN);
            equals.visitEnd();
        }
        { //hashCode
            MethodVisitor hashCode = this.node.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", methodDescriptor(int.class), null, null);
            hashCode.visitInsn(Opcodes.ICONST_0);
            hashCode.visitInsn(Opcodes.IRETURN);
            hashCode.visitEnd();
        }
        { //toString
            MethodVisitor toString = this.node.visitMethod(Opcodes.ACC_PUBLIC, "toString", methodDescriptor(String.class), null, null);
            toString.visitLdcInsn("AnnotationWrapper");
            toString.visitInsn(Opcodes.ARETURN);
            toString.visitEnd();
        }
        { //annotationType
            MethodVisitor annotationType = this.node.visitMethod(Opcodes.ACC_PUBLIC, "annotationType", methodDescriptor(Class.class), null, null);
            annotationType.visitLdcInsn(type(this.type));
            annotationType.visitInsn(Opcodes.ARETURN);
            annotationType.visitEnd();
        }
        { //getValues
            MethodVisitor getValues = this.node.visitMethod(Opcodes.ACC_PUBLIC, "getValues", methodDescriptor(Map.class), null, null);
            getValues.visitVarInsn(Opcodes.ALOAD, 0);
            getValues.visitFieldInsn(Opcodes.GETFIELD, this.node.name, "values", typeDescriptor(Map.class));
            getValues.visitInsn(Opcodes.ARETURN);
            getValues.visitEnd();
        }
        { //wasSet
            MethodVisitor wasSet = this.node.visitMethod(Opcodes.ACC_PUBLIC, "wasSet", methodDescriptor(boolean.class, String.class), null, null);
            for (String value : this.values.keySet()) {
                if (this.initializedDefaultValues.contains(value)) continue;

                Label jumpAfter = new Label();
                wasSet.visitVarInsn(Opcodes.ALOAD, 1);
                wasSet.visitLdcInsn(value);
                wasSet.visitMethodInsn(Opcodes.INVOKEVIRTUAL, IN_String, "equals", methodDescriptor(boolean.class, Object.class), false);
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
            MethodVisitor methodVisitor = this.node.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), methodDescriptor(method), null, null);
            Object value = this.values.get(method.getName());
            this.visit(methodVisitor, method.getReturnType(), value);
            methodVisitor.visitInsn(ASMUtils.getReturnOpcode(returnType(method)));
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
        else if (type.equals(Class.class) || type.equals(Type.class)) this.visitClass(methodVisitor, value);
        else if (type.isEnum()) this.visitEnum(methodVisitor, value);
        else if (type.isAnnotation() || type.equals(AnnotationNode.class)) this.visitAnnotation(methodVisitor, value);
        else if (type.isArray() || List.class.isAssignableFrom(type)) this.visitArray(methodVisitor, type.getComponentType(), value);
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
        if (value instanceof Class<?> || value instanceof Type) visitType(methodVisitor, value);
        else throw new IllegalArgumentException("Unexpected value class for type 'Class': " + value.getClass());
    }

    private void visitEnum(final MethodVisitor methodVisitor, final Object value) {
        if (value instanceof Enum<?>) {
            Enum<?> e = (Enum<?>) value;
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalName(e.getDeclaringClass()), e.name(), typeDescriptor(e.getDeclaringClass()));
        } else if (value instanceof String[]) {
            String[] enumValue = (String[]) value;
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalName(enumValue[0]), enumValue[1], enumValue[0]);
        } else {
            throw new IllegalArgumentException("Unexpected value class for type 'Enum': " + value.getClass());
        }
    }

    private void visitAnnotation(final MethodVisitor methodVisitor, final Object value) {
        Type annotationType;
        if (value instanceof Annotation) annotationType = type(((Annotation) value).annotationType());
        else if (value instanceof AnnotationNode) annotationType = type(((AnnotationNode) value).desc);
        else throw new IllegalArgumentException("Unexpected value class for type 'Annotation': " + value.getClass());

        methodVisitor.visitLdcInsn(annotationType);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, this.node.name, "classProvider", typeDescriptor(IClassProvider.class));
        methodVisitor.visitTypeInsn(Opcodes.NEW, internalName(HashMap.class));
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName(HashMap.class), MN_Init, MD_Void, false);
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
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, internalName(Map.class), "put", methodDescriptor(Object.class, Object.class, Object.class), true);
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
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, internalName(Map.class), "put", methodDescriptor(Object.class, Object.class, Object.class), true);
                methodVisitor.visitInsn(Opcodes.POP);
            }
        }
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, internalName(AnnotationParser.class), "parse", methodDescriptor(Annotation.class, Class.class, IClassProvider.class, Map.class), false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, internalName(annotationType));
    }

    private void visitArray(final MethodVisitor methodVisitor, final Class<?> arrayType, final Object value) {
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, array.length);
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalName(arrayType));
            for (int i = 0; i < array.length; i++) {
                methodVisitor.visitInsn(Opcodes.DUP);
                methodVisitor.visitLdcInsn(i);
                this.visit(methodVisitor, arrayType, array[i]);
                methodVisitor.visitInsn(Opcodes.AASTORE);
            }
        } else if (value instanceof List) {
            List<?> array = (List<?>) value;
            methodVisitor.visitIntInsn(Opcodes.BIPUSH, array.size());
            methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, internalName(arrayType));
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
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Boolean, "valueOf", methodDescriptor(Boolean.class, boolean.class), false);
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Byte, "valueOf", methodDescriptor(Byte.class, byte.class), false);
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Short, "valueOf", methodDescriptor(Short.class, short.class), false);
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Character, "valueOf", methodDescriptor(Character.class, char.class), false);
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Integer, "valueOf", methodDescriptor(Integer.class, int.class), false);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Long, "valueOf", methodDescriptor(Long.class, long.class), false);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Float, "valueOf", methodDescriptor(Float.class, float.class), false);
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_Double, "valueOf", methodDescriptor(Double.class, double.class), false);
        } else if (type.equals(String.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, IN_String, "valueOf", methodDescriptor(String.class, Object.class), false);
        }
    }

    private void visitType(final MethodVisitor methodVisitor, final Object typeOrClass) {
        if (Type.VOID_TYPE.equals(typeOrClass) || void.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Void, "TYPE", "Ljava/lang/Class;");
        } else if (Type.BOOLEAN_TYPE.equals(typeOrClass) || boolean.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Boolean, "TYPE", "Ljava/lang/Class;");
        } else if (Type.BYTE_TYPE.equals(typeOrClass) || byte.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Byte, "TYPE", "Ljava/lang/Class;");
        } else if (Type.SHORT_TYPE.equals(typeOrClass) || short.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Short, "TYPE", "Ljava/lang/Class;");
        } else if (Type.CHAR_TYPE.equals(typeOrClass) || char.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Character, "TYPE", "Ljava/lang/Class;");
        } else if (Type.INT_TYPE.equals(typeOrClass) || int.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Integer, "TYPE", "Ljava/lang/Class;");
        } else if (Type.LONG_TYPE.equals(typeOrClass) || long.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Long, "TYPE", "Ljava/lang/Class;");
        } else if (Type.FLOAT_TYPE.equals(typeOrClass) || float.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Float, "TYPE", "Ljava/lang/Class;");
        } else if (Type.DOUBLE_TYPE.equals(typeOrClass) || double.class.equals(typeOrClass)) {
            methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, IN_Double, "TYPE", "Ljava/lang/Class;");
        } else if (typeOrClass instanceof Class<?>) {
            methodVisitor.visitLdcInsn(type(typeOrClass));
        } else if (typeOrClass instanceof Type) {
            methodVisitor.visitLdcInsn(typeOrClass);
        } else {
            throw new IllegalArgumentException("Unexpected type or class: " + typeOrClass);
        }
    }

}
