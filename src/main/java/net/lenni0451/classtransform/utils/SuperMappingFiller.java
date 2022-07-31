package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SuperMappingFiller {

    /**
     * Fill all super mappings for the given transformer targets
     *
     * @param transformer   The {@link ClassNode} of the transformer
     * @param remapper      The {@link MapRemapper} to use
     * @param classProvider The {@link IClassProvider} to use
     */
    public static void fillTransformerSuperMembers(final ClassNode transformer, final MapRemapper remapper, final IClassProvider classProvider) {
        List<Object> annotation;
        if (transformer.invisibleAnnotations == null || (annotation = transformer.invisibleAnnotations.stream().filter(a -> a.desc.equals(Type.getDescriptor(CTransformer.class))).map(a -> a.values).findFirst().orElse(null)) == null) {
            throw new IllegalStateException("Transformer does not have CTransformer annotation");
        }
        for (int i = 0; i < annotation.size(); i += 2) {
            String key = (String) annotation.get(i);
            Object value = annotation.get(i + 1);

            if (key.equals("value")) {
                List<Type> classesList = (List<Type>) value;
                for (Type type : classesList) {
                    ClassTree treePart = ClassTree.getTreePart(classProvider, remapper.mapSafe(type.getInternalName()));
                    Set<ClassNode> superClasses = treePart.walkSuperClasses(new HashSet<>(), classProvider, false).stream().map(ClassTree::getNode).collect(Collectors.toSet());
                    fillSuperMembers(treePart.getNode(), superClasses, remapper);
                }
            } else if (key.equals("name")) {
                List<String> classesList = (List<String>) value;
                for (String className : classesList) {
                    ClassTree treePart = ClassTree.getTreePart(classProvider, remapper.mapSafe(className.replace(".", "/")));
                    Set<ClassNode> superClasses = treePart.walkSuperClasses(new HashSet<>(), classProvider, false).stream().map(ClassTree::getNode).collect(Collectors.toSet());
                    fillSuperMembers(treePart.getNode(), superClasses, remapper);
                }
            }
        }
    }

    /**
     * Fill all super mappings for the given class<br>
     * All input classes <b>must</b> be in the target format of the remapper
     *
     * @param node         The {@link ClassNode} of the target class
     * @param superClasses The {@link Set} of super classes to use
     * @param remapper     The {@link MapRemapper} to use
     */
    public static void fillSuperMembers(final ClassNode node, final Set<ClassNode> superClasses, final MapRemapper remapper) {
        MapRemapper reverseRemapper = remapper.reverse();
        for (ClassNode superClass : superClasses) {
            for (FieldNode field : superClass.fields) {
                if (Modifier.isStatic(field.access)) continue;
                if (Modifier.isPrivate(field.access)) continue;
                String mappedName = reverseRemapper.mapFieldName(superClass.name, field.name, field.desc);
                if (field.name.equals(mappedName)) continue;
                remapper.addFieldMapping(reverseRemapper.mapSafe(node.name), mappedName, reverseRemapper.mapDesc(field.desc), field.name, true);
            }
            for (MethodNode method : superClass.methods) {
                if (Modifier.isStatic(method.access)) continue;
                if (Modifier.isPrivate(method.access)) continue;
                String mappedName = reverseRemapper.mapMethodName(superClass.name, method.name, method.desc);
                if (method.name.equals(mappedName)) continue;
                remapper.addMethodMapping(reverseRemapper.mapSafe(node.name), mappedName, reverseRemapper.mapMethodDesc(method.desc), method.name, true);
            }
        }
    }

}
