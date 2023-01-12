package net.lenni0451.classtransform.mappings;

import net.lenni0451.classtransform.mappings.annotation.AnnotationRemap;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.log.ILogger;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import net.lenni0451.classtransform.utils.mappings.SuperMappingFiller;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static net.lenni0451.classtransform.utils.ASMUtils.dot;
import static net.lenni0451.classtransform.utils.ASMUtils.slash;

public abstract class AMapper {

    private final MapperConfig config;
    protected final MapRemapper remapper;

    public AMapper(final MapperConfig config) {
        this.config = config;
        this.remapper = new MapRemapper();
    }

    public final void load() {
        try {
            this.init();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to initialize mappings", t);
        }
    }

    public final String mapClassName(final String className) {
        return dot(this.remapper.mapType(slash(className)));
    }

    public final ClassNode mapClass(final IClassProvider classProvider, final ILogger logger, final ClassNode target, final ClassNode transformer) {
        if (this.config.fillSuperMappings) {
            try {
                SuperMappingFiller.fillTransformerSuperMembers(transformer, this.remapper, classProvider);
            } catch (Throwable t) {
                logger.warn("Unable to fill super mappings for class '%s'. Trying without", transformer.name, t);
            }
        }
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
            try {
                Class<?> annotationClass = Class.forName(Type.getType(annotation.annotation.desc).getClassName());
                Map<String, Object> annotationMap = AnnotationParser.listToMap(annotation.annotation.values);
                this.mapAnnotation(annotation.holder, annotationClass, annotationMap, target, transformer);
                annotation.annotation.values = AnnotationParser.mapToList(annotationMap);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to remap annotation '" + annotation.annotation.desc + "' from transformer '" + transformer.name + "'", e);
            }
        }
        if (this.config.remapTransformer) return Remapper.remap(transformer, this.remapper);
        else return transformer;
    }

    public MapRemapper getRemapper() {
        return this.remapper;
    }


    protected abstract void init() throws Throwable;


    protected List<String> readLines(final File f) throws FileNotFoundException {
        List<String> out = new ArrayList<>();
        try (Scanner s = new Scanner(f)) {
            while (s.hasNextLine()) out.add(s.nextLine());
        }
        return out;
    }


    private void mapAnnotation(final Object holder, final Class<?> annotation, final Map<String, Object> values, final ClassNode target, final ClassNode transformer) throws ClassNotFoundException {
        for (Method method : annotation.getDeclaredMethods()) {
            AnnotationRemap remap = method.getDeclaredAnnotation(AnnotationRemap.class);
            if (remap == null) continue;
            InfoFiller.fillInfo(this.remapper, holder, remap, method, values, target, transformer);
            if (this.remapper.isEmpty()) continue;

            Object value = values.get(method.getName());

            if (remap.value().equals(RemapType.ANNOTATION)) {
                if (value instanceof AnnotationNode) {
                    AnnotationNode node = (AnnotationNode) value;
                    Type type = Type.getType(node.desc);
                    Map<String, Object> nodeMap = AnnotationParser.listToMap(node.values);
                    this.mapAnnotation(holder, Class.forName(type.getClassName()), nodeMap, target, transformer);
                    node.values = AnnotationParser.mapToList(nodeMap);
                } else if (value instanceof AnnotationNode[]) {
                    AnnotationNode[] nodes = (AnnotationNode[]) value;
                    for (AnnotationNode node : nodes) {
                        Type type = Type.getType(node.desc);
                        Map<String, Object> nodeMap = AnnotationParser.listToMap(node.values);
                        this.mapAnnotation(holder, Class.forName(type.getClassName()), nodeMap, target, transformer);
                        node.values = AnnotationParser.mapToList(nodeMap);
                    }
                } else if (value instanceof List) {
                    List<AnnotationNode> nodes = (List<AnnotationNode>) value;
                    for (AnnotationNode node : nodes) {
                        Type type = Type.getType(node.desc);
                        Map<String, Object> nodeMap = AnnotationParser.listToMap(node.values);
                        this.mapAnnotation(holder, Class.forName(type.getClassName()), nodeMap, target, transformer);
                        node.values = AnnotationParser.mapToList(nodeMap);
                    }
                }
            } else {
                if (value instanceof String) {
                    String s = (String) value;
                    values.put(method.getName(), remap(remap.value(), s));
                } else if (value instanceof String[]) {
                    String[] strings = (String[]) value;
                    for (int i = 0; i < strings.length; i++) strings[i] = remap(remap.value(), strings[i]);
                } else if (value instanceof List) {
                    List<String> list = (List<String>) value;
                    list.replaceAll(s -> remap(remap.value(), s));
                }
            }
        }
    }

    private String remap(final RemapType type, String s) {
        switch (type) {
            case SHORT_MEMBER:
                //See InfoFiller for remapping of short members
                return s;

            case MEMBER:
                MemberDeclaration member = ASMUtils.splitMemberDeclaration(s);
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

            case CLASS:
                return dot(this.remapper.mapType(slash(s)));

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    private void checkAnnotations(final Object holder, final List<AnnotationNode> annotations, final List<AnnotationHolder> out) {
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
