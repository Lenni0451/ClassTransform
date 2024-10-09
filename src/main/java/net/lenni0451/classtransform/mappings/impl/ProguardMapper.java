package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.utils.IOSupplier;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;

/**
 * A remapper that uses a proguard mapping file for remapping.
 */
@ParametersAreNonnullByDefault
public class ProguardMapper extends AMapper {

    private static final String CLASS_LINE = "^([^ ]+) ?-> ?([^ ]+):$";
    private static final String METHOD_LINE = "^ {4}(\\d+:|)+([^ ]+) ([^ ()]+)(\\([^ ()]*\\))(:\\d+|)+ ?-> ?(.+)$";
    private static final String FIELD_LINE = "^ {4}([^ ]+) ([^ (]+) ?-> ?(.+)$";

    private final IOSupplier<InputStream> mappingsSupplier;

    public ProguardMapper(final MapperConfig config, @WillClose final InputStream mappingsStream) {
        super(config);
        this.mappingsSupplier = () -> mappingsStream;
    }

    public ProguardMapper(final MapperConfig config, final File mappingsFile) {
        super(config);
        this.mappingsSupplier = () -> new FileInputStream(mappingsFile);
    }

    @Override
    protected void init() throws Throwable {
        String currentClass = null;
        for (String line : this.readLines(this.mappingsSupplier.get())) {
            if (line.trim().isEmpty() || line.startsWith("#")) continue;

            String error = null;
            if (line.matches(CLASS_LINE)) {
                Matcher m = Pattern.compile(CLASS_LINE).matcher(line);
                if (m.find()) {
                    currentClass = slash(m.group(1));
                    String newName = slash(m.group(2));

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
                        String descriptor = this.descriptorToInternal(m.group(4));
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
            case "void":
                return arrayCount + "V";
            case "boolean":
                return arrayCount + "Z";
            case "byte":
                return arrayCount + "B";
            case "short":
                return arrayCount + "S";
            case "char":
                return arrayCount + "C";
            case "int":
                return arrayCount + "I";
            case "long":
                return arrayCount + "J";
            case "float":
                return arrayCount + "F";
            case "double":
                return arrayCount + "D";
            default:
                return arrayCount + "L" + slash(type) + ";";
        }
    }

    private String descriptorToInternal(String descriptor) {
        descriptor = descriptor.substring(1, descriptor.length() - 1);
        if (descriptor.isEmpty()) return "()";

        String[] parts = descriptor.split(",");
        String out = "";
        for (String part : parts) out += this.typeToInternal(part);
        return "(" + out + ")";
    }

}
