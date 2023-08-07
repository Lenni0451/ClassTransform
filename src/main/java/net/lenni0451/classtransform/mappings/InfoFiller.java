package net.lenni0451.classtransform.mappings;

import net.lenni0451.classtransform.exceptions.FieldNotFoundException;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.FillType;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class remaps short members in transformer annotation.<br>
 * This also is responsible for resolving wildcards in short members.
 */
@ParametersAreNonnullByDefault
class InfoFiller {

    static void fillInfo(final MapRemapper remapper, final Object holder, final AnnotationRemap remap, final Method method, final Map<String, Object> values, final ClassNode target, final ClassNode transformer) {
        if (!remap.value().equals(RemapType.SHORT_MEMBER)) return;
        if (remap.fill().equals(FillType.SKIP)) return;

        Object value = values.get(method.getName());
        if (method.getReturnType().equals(String.class)) {
            String current = (String) value;
            if (!remap.fill().equals(FillType.KEEP_EMPTY)) {
                List<String> names = getNames(remapper, holder, current, target, transformer);
                if (names.size() != 1) throw new MethodNotFoundException(target, transformer, current);
                values.put(method.getName(), names.get(0));
            }
        } else if (method.getReturnType().equals(String[].class)) {
            List<String> current = (List<String>) value;
            if (current == null) current = new ArrayList<>();
            if (current.isEmpty()) {
                if (!remap.fill().equals(FillType.KEEP_EMPTY)) current.addAll(getMethodNames(remapper, holder, null, target, transformer));
            } else {
                List<String> newValues = new ArrayList<>();
                for (String name : current) newValues.addAll(getNames(remapper, holder, name, target, transformer));
                current = newValues;
            }
            values.put(method.getName(), current);
        }
    }

    private static List<String> getNames(final MapRemapper remapper, final Object holder, final String current, final ClassNode target, final ClassNode transformer) {
        if (holder instanceof MethodNode) return getMethodNames(remapper, holder, current, target, transformer);
        else if (holder instanceof FieldNode) return getFieldNames(remapper, holder, current, target, transformer);
        else throw new IllegalArgumentException("Unknown holder type '" + holder.getClass().getName() + "' from transformer '" + transformer.name + "'");
    }

    private static List<String> getMethodNames(final MapRemapper remapper, final Object holder, @Nullable String current, final ClassNode target, final ClassNode transformer) {
        List<String> names = new ArrayList<>();
        if (current == null) { //Copy the name and descriptor of the transformer method
            MethodNode methodNode = (MethodNode) holder;
            current = methodNode.name + methodNode.desc;
        }
        if (!remapper.isEmpty()) { //Remap the current name if mappings are available
            String originalTarget = remapper.reverse().map(target.name);
            if (originalTarget == null) originalTarget = target.name;

            MemberDeclaration fullDeclaration = ASMUtils.splitMemberDeclaration(current);
            if (fullDeclaration != null) { //TODO: Add support for only targeting specific members when specifying multiple target classes
                if (!originalTarget.equals(fullDeclaration.getOwner())) {
                    throw new IllegalArgumentException("The owner of the method '" + current + "' does not match the target class '" + originalTarget + "'");
                }
                current = fullDeclaration.getName() + fullDeclaration.getDesc();
            }
            if (current.contains("(")) { //If a descriptor is available, remap the method name and descriptor
                String unmappedMethodName = current.substring(0, current.indexOf('('));
                String unmappedMethodDesc = current.substring(current.indexOf('('));
                String mappedMethodName = remapper.mapMethodName(originalTarget, unmappedMethodName, unmappedMethodDesc);
                String mappedMethodDesc = remapper.mapMethodDesc(unmappedMethodDesc);

                //Verify that the remapped method actually exists
                MethodNode methodNode = ASMUtils.getMethod(target, mappedMethodName, mappedMethodDesc);
                if (methodNode == null) throw new MethodNotFoundException(target, transformer, mappedMethodName + mappedMethodDesc);
                names.add(methodNode.name + methodNode.desc);
            } else { //If no descriptor is available, remap the method name and get all available mappings for that name
                String partialMapping = originalTarget + "." + current + "("; //The start of a method mapping
                List<String> partialMappings = remapper.getStartingMappings(partialMapping);
                for (String mapping : partialMappings) {
                    String unmappedMethodDesc = mapping.substring(partialMapping.length() - 1);
                    String mappedMethodName = remapper.map(mapping);
                    String mappedMethodDesc = remapper.mapMethodDesc(unmappedMethodDesc);

                    //Verify that the remapped method actually exists
                    MethodNode methodNode = ASMUtils.getMethod(target, mappedMethodName, mappedMethodDesc);
                    if (methodNode == null) throw new MethodNotFoundException(target, transformer, mappedMethodName + mappedMethodDesc);
                    names.add(methodNode.name + methodNode.desc);
                }
            }
        }
        if (names.isEmpty()) { //If no names have been found, get all methods from the current name and descriptor and add those
            List<MethodNode> methods = ASMUtils.getMethodsFromCombi(target, current);
            if (methods.isEmpty()) throw new MethodNotFoundException(target, transformer, current);
            for (MethodNode method : methods) names.add(method.name + method.desc);
        }
        return names;
    }

    private static List<String> getFieldNames(final MapRemapper remapper, final Object holder, @Nullable String current, final ClassNode target, final ClassNode transformer) {
        List<String> names = new ArrayList<>();
        if (current == null) { //Copy the name of the transformer field
            FieldNode fieldNode = (FieldNode) holder;
            current = fieldNode.name + ":" + fieldNode.desc;
        }
        if (!remapper.isEmpty()) { //Remap the current name if mappings are available
            String originalTarget = remapper.reverse().map(target.name);
            if (originalTarget == null) originalTarget = target.name;

            MemberDeclaration fullDeclaration = ASMUtils.splitMemberDeclaration(current);
            if (fullDeclaration != null) { //TODO: Add support for only targeting specific members when specifying multiple target classes
                if (!originalTarget.equals(fullDeclaration.getOwner())) {
                    throw new IllegalArgumentException("The owner of the field '" + current + "' does not match the target class '" + originalTarget + "'");
                }
                current = fullDeclaration.getName() + fullDeclaration.getDesc();
            }
            if (current.contains(":")) { //If a descriptor is available, remap the field name and descriptor
                String unmappedName = current.substring(0, current.indexOf(':'));
                String unmappedDescriptor = current.substring(current.indexOf(":") + 1);
                String mappedFieldName = remapper.mapFieldName(originalTarget, unmappedName, unmappedDescriptor);
                String mappedDescriptor = remapper.mapDesc(unmappedDescriptor);

                //Verify that the remapped field actually exists
                FieldNode fieldNode = ASMUtils.getField(target, mappedFieldName, mappedDescriptor);
                if (fieldNode == null) throw new FieldNotFoundException(target, transformer, mappedFieldName + ":" + mappedDescriptor);
                names.add(fieldNode.name + ":" + fieldNode.desc);
            } else { //If no descriptor is available, remap the field name and get all available mappings for that name
                String partialMapping = originalTarget + "." + current + ":";
                List<String> partialMappings = remapper.getStartingMappings(partialMapping);
                for (String mapping : partialMappings) {
                    String unmappedDescriptor = mapping.substring(partialMapping.length());
                    String mappedFieldName = remapper.map(mapping);
                    String mappedDescriptor = null;
                    if (!unmappedDescriptor.isEmpty()) mappedDescriptor = remapper.mapDesc(unmappedDescriptor);

                    //Verify that the remapped field actually exists
                    FieldNode fieldNode = ASMUtils.getField(target, mappedFieldName, mappedDescriptor);
                    if (fieldNode == null) throw new FieldNotFoundException(target, transformer, mappedFieldName + ":" + mappedDescriptor);
                    names.add(fieldNode.name + ":" + fieldNode.desc);
                }
            }
        }
        if (names.isEmpty()) { //If no names have been found, get all fields from the current name and descriptor and add those
            List<FieldNode> fields = ASMUtils.getFieldsFromCombi(target, current);
            if (fields.isEmpty()) throw new FieldNotFoundException(target, transformer, current);
            for (FieldNode field : fields) names.add(field.name + ":" + field.desc);
        }
        return names;
    }

}
