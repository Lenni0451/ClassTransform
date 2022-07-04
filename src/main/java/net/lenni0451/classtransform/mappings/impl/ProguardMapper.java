package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProguardMapper extends AMapper {

    private static final String CLASS_LINE = "^([^ ]+) ?-> ?([^ ]+):$";
    private static final String METHOD_LINE = "^ {4}(\\d+:|)+([^ ]+) ([^ ()]+)(\\([^ ()]*\\))(:\\d+|)+ ?-> ?(.+)$";
    private static final String FIELD_LINE = "^ {4}([^ ]+) ([^ (]+) ?-> ?(.+)$";

    private final File mappingFile;

    public ProguardMapper(final MapperConfig config, final File mappingFile) {
        super(config);
        this.mappingFile = mappingFile;
    }

    @Override
    protected void init() throws Throwable {
        String currentClass = null;
        for (String line : this.readLines(this.mappingFile)) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;

            String error = null;
            if (line.matches(CLASS_LINE)) {
                Matcher m = Pattern.compile(CLASS_LINE).matcher(line);
                if (m.find()) {
                    currentClass = this.slash(m.group(1));
                    String newName = this.slash(m.group(2));

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
                        String returnType = this.typeToInternal(m.group(2));
                        String name = m.group(3);
                        String descriptor = "(" + this.descriptorToInternal(m.group(4)) + ")";
                        String newName = m.group(6);

                        if (name.equals(newName)) continue;
                        this.remapper.addMethodMapping(currentClass, name, descriptor + returnType, newName);
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
                        String descriptor = this.typeToInternal(m.group(1));
                        String name = m.group(2);
                        String newName = m.group(3);

                        if (name.equals(newName)) continue;
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

    private String typeToInternal(String type) {
        String arrayCount = "";
        while (type.endsWith("[]")) {
            arrayCount += "[";
            type = type.substring(0, type.length() - 2);
        }

        switch (type) {
            case "int":
                return arrayCount + "I";

            case "float":
                return arrayCount + "F";

            case "double":
                return arrayCount + "D";

            case "long":
                return arrayCount + "J";

            case "boolean":
                return arrayCount + "Z";

            case "short":
                return arrayCount + "S";

            case "byte":
                return arrayCount + "B";

            case "void":
                return arrayCount + "V";

            default:
                return arrayCount + "L" + this.slash(type) + ";";
        }
    }

    private String descriptorToInternal(String descriptor) {
        descriptor = descriptor.substring(1, descriptor.length() - 1);
        String[] parts = descriptor.split(",");
        String out = "";
        for (String part : parts) out += this.typeToInternal(part);
        return out;
    }

}
