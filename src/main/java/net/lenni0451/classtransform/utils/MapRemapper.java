package net.lenni0451.classtransform.utils;

import org.objectweb.asm.commons.Remapper;

import java.util.*;

public class MapRemapper extends Remapper {

    private final Map<String, String> mappings;

    public MapRemapper() {
        this(new HashMap<>());
    }

    public MapRemapper(final String oldName, final String newName) {
        this(Collections.singletonMap(oldName, newName));
    }

    public MapRemapper(final Map<String, String> mappings) {
        this.mappings = mappings;
    }

    public void addClassMapping(final String from, final String to) {
        this.mappings.put(from, to);
    }

    public void addMethodMapping(final String owner, final String name, final String desc, final String target) {
        this.mappings.put(owner + "." + name + desc, target);
    }

    public void addFieldMapping(final String owner, final String name, final String target) {
        this.addFieldMapping(owner, name, "", target);
    }

    public void addFieldMapping(final String owner, final String name, final String desc, final String target) {
        this.mappings.put(owner + "." + name + ":" + desc, target);
    }

    public List<String> getStartingMappings(final String... starts) {
        List<String> mappings = new ArrayList<>();
        for (String mapping : this.mappings.keySet()) {
            for (String start : starts) {
                if (mapping.startsWith(start)) mappings.add(mapping);
            }
        }
        return mappings;
    }

    public boolean isEmpty() {
        return this.mappings.isEmpty();
    }


    @Override
    public String mapMethodName(final String owner, final String name, final String descriptor) {
        String remappedName = map(owner + '.' + name + descriptor);
        return remappedName == null ? name : remappedName;
    }

    @Override
    public String mapInvokeDynamicMethodName(final String name, final String descriptor) {
        String remappedName = map('.' + name + descriptor);
        return remappedName == null ? name : remappedName;
    }

    @Override
    public String mapAnnotationAttributeName(final String descriptor, final String name) {
        String remappedName = map(descriptor + '.' + name);
        return remappedName == null ? name : remappedName;
    }

    @Override
    public String mapFieldName(final String owner, final String name, final String descriptor) {
        String remappedName = map(owner + '.' + name + ':' + descriptor);
        if (remappedName == null) remappedName = map(owner + '.' + name + ":");
        return remappedName == null ? name : remappedName;
    }

    @Override
    public String map(final String key) {
        return this.mappings.get(key);
    }

}
