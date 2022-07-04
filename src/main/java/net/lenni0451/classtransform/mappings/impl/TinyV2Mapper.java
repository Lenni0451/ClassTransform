package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.utils.MapRemapper;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TinyV2Mapper extends AMapper {

    private static final String HEADER_LINE = "tiny\\t(2)\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)$";
    private static final String CLASS_LINE = "^c\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)$";
    private static final String METHOD_LINE = "^\\tm\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)$";
    private static final String FIELD_LINE = "^\\tf\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)$";
    private static final String PARAMETER_LINE = "^\\t{2}p\\t\\d+\\t+[^\\t]+$";
    private static final String COMMENT_LINE = "^\\t+c.*$";

    private final File mappingFile;
    private final Type type;

    public TinyV2Mapper(final MapperConfig config, final File mappingFile, final Type type) {
        super(config);
        this.mappingFile = mappingFile;
        this.type = type;
    }

    @Override
    protected void init() throws Throwable {
        MapRemapper descriptorRemapper = new MapRemapper();
        List<String> lines = this.readLines(this.mappingFile);
        for (String line : lines) {
            if (this.skipLine(line)) continue;

            if (line.matches(CLASS_LINE)) {
                Matcher m = Pattern.compile(CLASS_LINE).matcher(line);
                if (m.find()) {
                    String obfuscated = m.group(1);
                    String targetName = m.group(1 + this.type.getFromIndex());

                    descriptorRemapper.addClassMapping(obfuscated, targetName);
                } else {
                    throw new IllegalStateException("Could not parse class line: " + line);
                }
            }
        }

        String currentClass = null;
        for (String line : lines) {
            if (this.skipLine(line)) continue;

            String error = null;
            if (line.matches(CLASS_LINE)) {
                Matcher m = Pattern.compile(CLASS_LINE).matcher(line);
                if (m.find()) {
                    currentClass = m.group(1 + this.type.getFromIndex());
                    String newName = m.group(1 + this.type.getToIndex());

                    if (currentClass.equals(newName)) continue;
                    this.remapper.addClassMapping(currentClass, newName);
                } else {
                    error = "Could not parse class line: " + line;
                }
            } else if (line.matches(METHOD_LINE)) {
                if (currentClass == null) {
                    error = "Method line without class: " + line;
                } else {
                    Matcher m = Pattern.compile(METHOD_LINE).matcher(line);
                    if (m.find()) {
                        String descriptor = m.group(1);
                        String name = m.group(2 + this.type.getFromIndex());
                        String newName = m.group(2 + this.type.getToIndex());

                        if (name.equals(newName)) continue;
                        if (!descriptor.isEmpty()) descriptor = descriptorRemapper.mapMethodDesc(descriptor);
                        this.remapper.addMethodMapping(currentClass, name, descriptor, newName);
                    } else {
                        error = "Could not parse method line: " + line;
                    }
                }
            } else if (line.matches(FIELD_LINE)) {
                if (currentClass == null) {
                    error = "Field line without class: " + line;
                } else {
                    Matcher m = Pattern.compile(FIELD_LINE).matcher(line);
                    if (m.find()) {
                        String descriptor = m.group(1);
                        String name = m.group(2 + this.type.getFromIndex());
                        String newName = m.group(2 + this.type.getToIndex());

                        if (name.equals(newName)) continue;
                        if (!descriptor.isEmpty()) descriptor = descriptorRemapper.mapDesc(descriptor);
                        this.remapper.addFieldMapping(currentClass, name, descriptor, newName);
                    } else {
                        error = "Could not parse field line: " + line;
                    }
                }
            } else {
                error = "Unknown line: " + line;
            }

            if (error != null) throw new IllegalStateException(error);
        }
    }

    private boolean skipLine(final String line) {
        return line.trim().isEmpty() || line.matches(HEADER_LINE) || line.matches(COMMENT_LINE) || line.matches(PARAMETER_LINE);
    }


    public enum Type {
        OBFUSCATED_TO_INTERMEDIARY(0, 1),
        OBFUSCATED_TO_CLEAN(0, 2),

        INTERMEDIARY_TO_OBFUSCATED(1, 0),
        INTERMEDIARY_TO_CLEAN(1, 2),

        CLEAN_TO_OBFUSCATED(2, 0),
        CLEAN_TO_INTERMEDIARY(2, 1);


        private final int fromIndex;
        private final int toIndex;

        Type(final int fromIndex, final int toIndex) {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        public int getFromIndex() {
            return this.fromIndex;
        }

        public int getToIndex() {
            return this.toIndex;
        }
    }

}
