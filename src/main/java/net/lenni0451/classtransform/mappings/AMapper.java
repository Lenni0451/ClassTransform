package net.lenni0451.classtransform.mappings;

import lombok.SneakyThrows;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.mappings.dynamic.IDynamicRemapper;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.FailStrategy;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.log.Logger;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import net.lenni0451.classtransform.utils.mappings.SuperMappingFiller;
import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;
import static net.lenni0451.classtransform.utils.ASMUtils.slash;
import static net.lenni0451.classtransform.utils.Types.type;

/**
 * The abstract remapper class responsible for remapping class transform annotations.
 */
@ParametersAreNonnullByDefault
public abstract class AMapper {

    private final MapperConfig config;
    protected final MapRemapper remapper;
    private boolean initialized = false;
    private ClassTree superMappingsTree = null;

    public AMapper(final MapperConfig config) {
        this.config = config;
        this.remapper = new MapRemapper();
    }

    /**
     * Load all mappings from the overridden {@link #init()} method.<br>
     * Will do nothing if the mappings are already loaded.
     *
     * @throws RuntimeException If the {@link #init()} method throws an exception
     */
    public synchronized final void load() {
        if (this.initialized) return;
        try {
            this.init();
            this.initialized = true;
        } catch (Throwable t) {
            throw new RuntimeException("Unable to initialize mappings", t);
        }
    }

    /**
     * Remap the given class name separated by dots.<br>
     * If no mapping is found the original name will be returned.
     *
     * @param className The class name
     * @return The remapped class name
     */
    public final String mapClassName(final String className) {
        return dot(this.remapper.mapType(slash(className)));
    }

    /**
     * <b>Use {@link AMapper#mapClass(TransformerManager, ClassNode, ClassNode)}.</b>
     */
    @Deprecated
    @SneakyThrows
    public final ClassNode mapClass(final ClassTree classTree, final IClassProvider classProvider, final ClassNode target, final ClassNode transformer) {
        //Some horrible code for backwards compatibility
        //Remove as soon as possible
        Field field = ClassTree.class.getDeclaredField("transformerManager");
        field.setAccessible(true);
        TransformerManager transformerManager = (TransformerManager) field.get(classTree);
        if (transformerManager == null) {
            //The class tree has no transformer manager, so create a new one and replace the class tree
            transformerManager = new TransformerManager(classProvider);
            field = TransformerManager.class.getDeclaredField("classTree");
            field.setAccessible(true);
            field.set(transformerManager, classTree);
        }
        return this.mapClass(transformerManager, target, transformer);
    }

    /**
     * Remap the given transformer for the given target class.
     *
     * @param transformerManager The transformer manager
     * @param target             The target class node
     * @param transformer        The transformer class node
     * @return The remapped transformer class node
     */
    public final ClassNode mapClass(final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer) {
        this.fillTransformerSuperMappings(transformerManager, transformer);
        List<AnnotationHolder> annotationsToRemap = new ArrayList<>();
        this.checkAnnotations(transformer, transformer.visibleAnnotations, annotationsToRemap);
        this.checkAnnotations(transformer, transformer.invisibleAnnotations, annotationsToRemap);
        for (FieldNode field : transformer.fields) {
            this.checkAnnotations(field, field.visibleAnnotations, annotationsToRemap);
            this.checkAnnotations(field, field.invisibleAnnotations, annotationsToRemap);
        }
        for (MethodNode method : transformer.methods) {
            this.checkAnnotations(method, method.visibleAnnotations, annotationsToRemap);
            this.checkAnnotations(method, method.invisibleAnnotations, annotationsToRemap);
        }
        for (AnnotationHolder annotation : annotationsToRemap) {
            Class<?> annotationClass;
            try {
                annotationClass = Class.forName(type(annotation.annotation.desc).getClassName());
            } catch (ClassNotFoundException e) {
                //In bytecode it is possible to add annotations that are not in the classpath
                //If this is the case it can't be a ClassTransform annotation, so we can ignore it
                continue;
            }
            try {
                Map<String, Object> annotationMap = AnnotationUtils.listToMap(annotation.annotation.values);
                this.mapAnnotation(annotation.holder, annotationClass, annotationMap, transformerManager, target, transformer);
                annotation.annotation.values = AnnotationUtils.mapToList(annotationMap);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to remap annotation '" + annotation.annotation.desc + "' from transformer '" + transformer.name + "'", e);
            }
        }
        if (this.config.remapTransformer) return Remapper.remap(transformer, this.remapper);
        else return transformer;
    }

    /**
     * Get the remapper used by this mapper.
     *
     * @return The remapper
     */
    public MapRemapper getRemapper() {
        return this.remapper;
    }


    protected abstract void init() throws Throwable;


    protected List<String> readLines(final File f) throws IOException {
        return this.readLines(new FileInputStream(f));
    }

    protected List<String> readLines(final InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.toList());
        }
    }


    private ClassTree getSuperMappingsTree(final TransformerManager transformerManager) {
        if (!transformerManager.getClassTree().canTransform()) return transformerManager.getClassTree();
        if (this.superMappingsTree == null) this.superMappingsTree = new ClassTree();
        return this.superMappingsTree;
    }

    private void fillTransformerSuperMappings(final TransformerManager transformerManager, final ClassNode transformer) {
        if (!this.config.fillSuperMappings) return;
        try {
            SuperMappingFiller.fillTransformerSuperMembers(transformer, this.remapper, this.getSuperMappingsTree(transformerManager), transformerManager.getClassProvider());
        } catch (Throwable t) {
            if (FailStrategy.CONTINUE.equals(this.config.superMappingsFailStrategy)) {
                Logger.warn("Unable to fill super mappings for class '{}'. Trying without", transformer.name, t);
            } else if (FailStrategy.CANCEL.equals(this.config.superMappingsFailStrategy)) {
                throw new RuntimeException("Unable to fill super mappings for class '" + transformer.name + "'", t);
            } else if (FailStrategy.EXIT.equals(this.config.superMappingsFailStrategy)) {
                System.exit(-1);
            }
        }
    }

    private void fillSuperMembers(final String className, final TransformerManager transformerManager) {
        if (!this.config.fillSuperMappings) return;
        try {
            SuperMappingFiller.fillSuperMembers(className, this.remapper, this.getSuperMappingsTree(transformerManager), transformerManager.getClassProvider());
        } catch (Throwable t) {
            if (FailStrategy.CONTINUE.equals(this.config.superMappingsFailStrategy)) {
                Logger.warn("Unable to fill super mappings for class '{}'. Trying without", className, t);
            } else if (FailStrategy.CANCEL.equals(this.config.superMappingsFailStrategy)) {
                throw new RuntimeException("Unable to fill super mappings for class '" + className + "'", t);
            } else if (FailStrategy.EXIT.equals(this.config.superMappingsFailStrategy)) {
                System.exit(-1);
            }
        }
    }

    private void mapAnnotation(final Object holder, final Class<?> annotation, final Map<String, Object> values, final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer) throws ClassNotFoundException {
        for (Method method : annotation.getDeclaredMethods()) {
            AnnotationRemap remap = method.getDeclaredAnnotation(AnnotationRemap.class);
            if (remap == null) continue;
            RemapType remapType = remap.value();
            if (remapType.equals(RemapType.DYNAMIC)) {
                try {
                    IDynamicRemapper dynamicRemapper = remap.dynamicRemapper().getDeclaredConstructor().newInstance();
                    remapType = dynamicRemapper.dynamicRemap(this, annotation, values, method, transformerManager, target, transformer);
                    if (remapType == null || remapType.equals(RemapType.DYNAMIC)) continue;
                } catch (Throwable t) {
                    throw new RuntimeException("Unable to create instance of dynamic remapper '" + remap.dynamicRemapper().getName() + "'", t);
                }
            }
            if (remapType.equals(RemapType.SHORT_MEMBER)) InfoFiller.fillInfo(this.remapper, holder, remap, method, values, target, transformer);
            if (this.remapper.isEmpty()) continue;

            Object value = values.get(method.getName());
            if (value == null) continue;

            if (remapType.equals(RemapType.ANNOTATION)) {
                if (value instanceof AnnotationNode) {
                    AnnotationNode node = (AnnotationNode) value;
                    Type type = Type.getType(node.desc);
                    Map<String, Object> nodeMap = AnnotationUtils.listToMap(node.values);
                    this.mapAnnotation(holder, Class.forName(type.getClassName()), nodeMap, transformerManager, target, transformer);
                    node.values = AnnotationUtils.mapToList(nodeMap);
                } else if (value instanceof AnnotationNode[]) {
                    AnnotationNode[] nodes = (AnnotationNode[]) value;
                    for (AnnotationNode node : nodes) {
                        Type type = Type.getType(node.desc);
                        Map<String, Object> nodeMap = AnnotationUtils.listToMap(node.values);
                        this.mapAnnotation(holder, Class.forName(type.getClassName()), nodeMap, transformerManager, target, transformer);
                        node.values = AnnotationUtils.mapToList(nodeMap);
                    }
                } else if (value instanceof List) {
                    List<AnnotationNode> nodes = (List<AnnotationNode>) value;
                    for (AnnotationNode node : nodes) {
                        Type type = Type.getType(node.desc);
                        Map<String, Object> nodeMap = AnnotationUtils.listToMap(node.values);
                        this.mapAnnotation(holder, Class.forName(type.getClassName()), nodeMap, transformerManager, target, transformer);
                        node.values = AnnotationUtils.mapToList(nodeMap);
                    }
                } else {
                    throw new IllegalStateException("Unexpected value type '" + value.getClass().getName() + "' for annotation '" + annotation.getName() + "' value '" + annotation.getName() + "'");
                }
            } else {
                if (value instanceof String) {
                    String s = (String) value;
                    values.put(method.getName(), this.remap(remapType, s, transformerManager));
                } else if (value instanceof String[]) {
                    String[] strings = (String[]) value;
                    for (int i = 0; i < strings.length; i++) strings[i] = this.remap(remapType, strings[i], transformerManager);
                } else if (value instanceof List) {
                    final RemapType finalRemapType = remapType;
                    List<String> list = (List<String>) value;
                    list.replaceAll(s -> this.remap(finalRemapType, s, transformerManager));
                } else {
                    throw new IllegalStateException("Unexpected value type '" + value.getClass().getName() + "' for annotation '" + annotation.getName() + "' value '" + annotation.getName() + "'");
                }
            }
        }
    }

    private String remap(final RemapType type, String s, final TransformerManager transformerManager) {
        switch (type) {
            case SHORT_MEMBER:
                //See InfoFiller for remapping of short members
                return s;

            case MEMBER:
                MemberDeclaration member = ASMUtils.splitMemberDeclaration(s);
                if (member == null) throw new IllegalStateException("Invalid member declaration '" + s + "'");
                this.fillSuperMembers(member.getOwner(), transformerManager);
                String owner = this.remapper.mapType(member.getOwner());
                String name;
                String desc;
                if (member.isFieldMapping()) {
                    name = this.remapper.mapFieldName(member.getOwner(), member.getName(), member.getDesc());
                    desc = this.remapper.mapDesc(member.getDesc());
                } else {
                    name = this.remapper.mapMethodName(member.getOwner(), member.getName(), member.getDesc());
                    desc = this.remapper.mapMethodDesc(member.getDesc());
                }
                return Type.getObjectType(owner).getDescriptor() + name + (member.isFieldMapping() ? ":" : "") + desc;

            case DESCRIPTOR:
                if (s.startsWith("(")) return this.remapper.mapMethodDesc(s);
                else return this.remapper.mapDesc(s);

            case CLASS:
                return dot(this.remapper.mapType(slash(s)));

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    private void checkAnnotations(final Object holder, @Nullable final List<AnnotationNode> annotations, final List<AnnotationHolder> out) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) out.add(new AnnotationHolder(holder, annotation));
    }


    private static class AnnotationHolder {
        private final Object holder;
        private final AnnotationNode annotation;

        private AnnotationHolder(final Object holder, final AnnotationNode annotation) {
            this.holder = holder;
            this.annotation = annotation;
        }
    }

}
