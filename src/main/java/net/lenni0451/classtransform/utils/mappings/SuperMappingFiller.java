package net.lenni0451.classtransform.utils.mappings;

import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

/**
 * Util to complete mappings which are missing some information about overridden methods.
 */
@ParametersAreNonnullByDefault
public class SuperMappingFiller {

    /**
     * Fill all super mappings for the given transformer targets.<br>
     * Missing mappings are added to the given remapper.
     *
     * @param transformer   The class node of the transformer
     * @param remapper      The remapper to use
     * @param classTree     The class tree to use
     * @param classProvider The class provider to use
     * @throws ClassNotFoundException If a class could not be found
     */
    public static void fillTransformerSuperMembers(final ClassNode transformer, final MapRemapper remapper, final ClassTree classTree, final IClassProvider classProvider) throws ClassNotFoundException {
        List<Object> annotation = AnnotationUtils.findAnnotation(transformer, CTransformer.class).map(a -> a.values).orElseThrow(() -> new IllegalStateException("Transformer does not have CTransformer annotation"));
        for (int i = 0; i < annotation.size(); i += 2) {
            String key = (String) annotation.get(i);
            Object value = annotation.get(i + 1);

            if (key.equals("value")) {
                List<Type> classesList = (List<Type>) value;
                for (Type type : classesList) fillSuperMembers(type.getInternalName(), remapper, classTree, classProvider);
            } else if (key.equals("name")) {
                List<String> classesList = (List<String>) value;
                for (String className : classesList) fillSuperMembers(slash(className), remapper, classTree, classProvider);
            }
        }
    }

    /**
     * Fill all super mappings for the given class by name.<br>
     * The class name <b>must</b> be separated by slashes.<br>
     * Missing mappings are added to the given remapper.
     *
     * @param className     The name of the class (not descriptor)
     * @param remapper      The remapper to use
     * @param classTree     The class tree to use
     * @param classProvider The class provider to use
     * @throws ClassNotFoundException If a class could not be found
     */
    public static void fillSuperMembers(final String className, final MapRemapper remapper, final ClassTree classTree, final IClassProvider classProvider) throws ClassNotFoundException {
        ClassTree.TreePart treePart = classTree.getTreePart(classProvider, remapper.mapSafe(className));
        Set<ClassNode> superClasses = treePart.getParsedSuperClasses(classProvider, false).stream().map(ClassTree.TreePart::getNode).collect(Collectors.toSet());
        fillSuperMembers(treePart.getNode(), superClasses, remapper);
    }

    /**
     * Fill all super mappings for the given class.<br>
     * All input classes <b>must</b> be in the target format of the remapper.<br>
     * Missing mappings are added to the given remapper.
     *
     * @param node         The class node
     * @param superClasses The super classes of the given class
     * @param remapper     The remapper to use
     */
    public static void fillSuperMembers(final ClassNode node, final Set<ClassNode> superClasses, final MapRemapper remapper) {
        MapRemapper reverseRemapper = remapper.reverse();
        Set<String> mappedFields = new HashSet<>();
        Set<String> mappedMethods = new HashSet<>();
        for (ClassNode superClass : superClasses) {
            for (FieldNode field : superClass.fields) {
                if (Modifier.isPrivate(field.access)) continue;
                String mappedName = reverseRemapper.mapFieldName(superClass.name, field.name, field.desc);
                if (field.name.equals(mappedName)) continue;

                String mappedOwner = reverseRemapper.mapSafe(node.name);
                String mappedDesc = reverseRemapper.mapDesc(field.desc);
                if (mappedFields.add(field.name + field.desc)) {
                    remapper.addFieldMapping(mappedOwner, mappedName, mappedDesc, field.name, true);
                }
            }
            for (MethodNode method : superClass.methods) {
                if (Modifier.isPrivate(method.access)) continue;
                if (method.name.startsWith("<")) continue;
                String mappedName = reverseRemapper.mapMethodName(superClass.name, method.name, method.desc);
                if (method.name.equals(mappedName)) continue;

                String mappedOwner = reverseRemapper.mapSafe(node.name);
                String mappedDesc = reverseRemapper.mapMethodDesc(method.desc);
                if (mappedMethods.add(method.name + method.desc)) {
                    remapper.addMethodMapping(mappedOwner, mappedName, mappedDesc, method.name, true);
                }
            }
        }
    }

    /**
     * Fill all super mappings for all classes found in the given remapper.<br>
     * If a class could not be found the mappings for it will be skipped.<br>
     * Missing mappings are added to the given remapper.<br>
     * The remapper <b>must</b> map from named to obfuscated. If you need to map from obfuscated to named use {@link MapRemapper#reverse()}.
     *
     * @param remapper      The remapper to use
     * @param classTree     The class tree to use
     * @param classProvider The class provider to use
     */
    public static void fillAllSuperMembers(final MapRemapper remapper, final ClassTree classTree, final IClassProvider classProvider) {
        CachedMapRemapper cachedRemapper = new CachedMapRemapper(remapper);
        for (String clazz : remapper.getMentionedClasses()) {
            String obfClass = remapper.mapSafe(clazz);
            try {
                ClassTree.TreePart treePart = classTree.getTreePart(classProvider, obfClass);
                Set<ClassTree.TreePart> superClassParts = treePart.getParsedSuperClasses(classProvider, false);
                Set<ClassNode> superClasses = new LinkedHashSet<>();
                for (ClassTree.TreePart part : superClassParts) superClasses.add(part.getNode());
                SuperMappingFiller.fillSuperMembers(treePart.getNode(), superClasses, cachedRemapper);
            } catch (Throwable ignored) {
            }
        }
    }


    private static class CachedMapRemapper extends MapRemapper {
        private final MapRemapper remapper;
        private final MapRemapper reverse;

        public CachedMapRemapper(final MapRemapper remapper) {
            super(Collections.emptyMap());
            this.remapper = remapper;
            this.reverse = new MapRemapper(remapper.reverse().getMappings());
        }

        @Override
        public void addFieldMapping(String owner, String name, String desc, String target, boolean skipIfExists) {
            this.remapper.addFieldMapping(owner, name, desc, target, skipIfExists);
        }

        @Override
        public void addMethodMapping(String owner, String name, String desc, String target, boolean skipIfExists) {
            this.remapper.addMethodMapping(owner, name, desc, target, skipIfExists);
        }

        @Nonnull
        @Override
        public MapRemapper reverse() {
            //Reversing the mapper causes a lot of overhead
            //For filling the super mappings it is sufficient to just cache the reverse mapper
            return this.reverse;
        }
    }

}
