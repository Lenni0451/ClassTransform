package net.lenni0451.classtransform.mappings;

import net.lenni0451.classtransform.exceptions.FieldNotFoundException;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class InfoFiller {

    static void fillInfo(final Object holder, final AnnotationRemap remap, final Method method, final Map<String, Object> values, final ClassNode target, final ClassNode transformer) {
        if (!remap.value().equals(RemapType.SHORT_MEMBER)) return;
        if (remap.fill().equals(FillType.SKIP)) return;

        Object value = values.get(method.getName());
        if (method.getReturnType().equals(String.class)) {
            String current = (String) value;
            if (!remap.fill().equals(FillType.KEEP_EMPTY)) {
                List<String> names = getNames(holder, current, target, transformer);
                if (names.size() != 1) throw new MethodNotFoundException(target, transformer, current);
                values.put(method.getName(), names.get(0));
            }
        } else if (method.getReturnType().equals(String[].class)) {
            List<String> current = (List<String>) value;
            if (current == null) current = new ArrayList<>();
            if (current.isEmpty()) {
                if (!remap.fill().equals(FillType.KEEP_EMPTY)) current.addAll(getMethodNames(holder, null, target, transformer));
            } else {
                List<String> newValues = new ArrayList<>();
                for (String name : current) newValues.addAll(getNames(holder, name, target, transformer));
                current = newValues;
            }
            values.put(method.getName(), current);
        }
    }

    private static List<String> getNames(final Object holder, final String current, final ClassNode target, final ClassNode transformer) {
        if (holder instanceof MethodNode) return getMethodNames(holder, current, target, transformer);
        else if (holder instanceof FieldNode) return getFieldNames(holder, current, target, transformer);
        else throw new IllegalArgumentException("Unknown holder type '" + holder.getClass().getName() + "' from transformer '" + transformer.name + "'");
    }

    private static List<String> getMethodNames(final Object holder, final String current, final ClassNode target, final ClassNode transformer) {
        List<String> names = new ArrayList<>();
        if (current == null) {
            MethodNode methodNode = (MethodNode) holder;
            names.add(methodNode.name + methodNode.desc);
        } else {
            List<MethodNode> methods = ASMUtils.getMethodsFromCombi(target, current);
            if (methods.isEmpty()) throw new MethodNotFoundException(target, transformer, current);
            for (MethodNode method : methods) names.add(method.name + method.desc);
        }
        return names;
    }

    private static List<String> getFieldNames(final Object holder, final String current, final ClassNode target, final ClassNode transformer) {
        List<String> names = new ArrayList<>();
        if (current == null) {
            FieldNode fieldNode = (FieldNode) holder;
            names.add(fieldNode.name + ":" + fieldNode.desc);
        } else {
            List<FieldNode> fields = ASMUtils.getFieldsFromCombi(target, current);
            if (fields.isEmpty()) throw new FieldNotFoundException(target, transformer, current);
            for (FieldNode field : fields) names.add(field.name + ":" + field.desc);
        }
        return names;
    }

}
