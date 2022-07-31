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

    public Map<String, String> getMappings() {
        return Collections.unmodifiableMap(this.mappings);
    }

    public void addClassMapping(final String from, final String to) {
        this.addClassMapping(from, to, false);
    }

    public void addClassMapping(final String from, final String to, final boolean skipIfExists) {
        if (skipIfExists && this.mappings.containsKey(from)) return;
        this.mappings.put(from, to);
    }

    public void addMethodMapping(final String owner, final String name, final String desc, final String target) {
        this.addMethodMapping(owner, name, desc, target, false);
    }

    public void addMethodMapping(final String owner, final String name, final String desc, final String target, final boolean skipIfExists) {
        String key = owner + "." + name + desc;
        if (skipIfExists && this.mappings.containsKey(key)) return;
        this.mappings.put(key, target);
    }

    public void addFieldMapping(final String owner, final String name, final String target) {
        this.addFieldMapping(owner, name, "", target, false);
    }

    public void addFieldMapping(final String owner, final String name, final String target, final boolean skipIfExists) {
        this.addFieldMapping(owner, name, "", target, skipIfExists);
    }

    public void addFieldMapping(final String owner, final String name, final String desc, final String target) {
        this.addFieldMapping(owner, name, desc, target, false);
    }

    public void addFieldMapping(final String owner, final String name, final String desc, final String target, final boolean skipIfExists) {
        String key = owner + "." + name + ":" + desc;
        if (skipIfExists && this.mappings.containsKey(key)) return;
        this.mappings.put(key, target);
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

    public String mapSafe(final String key) {
        return this.mappings.getOrDefault(key, key);
    }

    public String mapReverse(final String mapping) {
        return this.mappings.entrySet().stream().filter(e -> e.getValue().equals(mapping)).map(Map.Entry::getKey).findFirst().orElse(null);
    }


    public MapRemapper reverse() {
        MapRemapper reverseRemapper = new MapRemapper();
        for (Map.Entry<String, String> entry : this.mappings.entrySet()) {
            if (entry.getKey().contains(".")) continue;
            reverseRemapper.addClassMapping(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : this.mappings.entrySet()) {
            if (!entry.getKey().contains(".")) continue;
            if (entry.getKey().contains(":")) {
                String fieldMapping = entry.getKey();
                String owner = fieldMapping.substring(0, fieldMapping.indexOf("."));
                String name = fieldMapping.substring(fieldMapping.indexOf(".") + 1, fieldMapping.indexOf(":"));
                String desc = fieldMapping.substring(fieldMapping.indexOf(":") + 1);
                String mappedName = entry.getValue();

                if (desc.isEmpty()) reverseRemapper.addFieldMapping(this.mapSafe(owner), mappedName, name);
                else reverseRemapper.addFieldMapping(this.mapSafe(owner), mappedName, this.mapDesc(desc), name);
            } else {
                String methodMapping = entry.getKey();
                String owner = methodMapping.substring(0, methodMapping.indexOf("."));
                String name = methodMapping.substring(methodMapping.indexOf(".") + 1, methodMapping.indexOf("("));
                String desc = methodMapping.substring(methodMapping.indexOf("("));
                String mappedName = entry.getValue();

                reverseRemapper.addMethodMapping(this.mapSafe(owner), mappedName, this.mapMethodDesc(desc), name);
            }
        }
        return reverseRemapper;
    }

}
