package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SrgMapper extends AMapper {

    private static final String CLASS_LINE = "^CL: (\\S+) (\\S+)$";
    private static final String FIELD_LINE = "^FD: (\\S+)/(\\S+) (\\S+)/(\\S+)$";
    private static final String METHOD_LINE = "^MD: (\\S+)/(\\S+) (\\(\\S*\\)\\S+) (\\S+)/(\\S+) (\\(\\S*\\)\\S+)$";

    private final File mappingFile;

    public SrgMapper(final MapperConfig config, final File mappingFile) {
        super(config);

        this.mappingFile = mappingFile;
    }

    @Override
    protected void init() throws Throwable {
        for (String line : this.readLines(this.mappingFile)) {
            String error = null;
            if (line.matches(CLASS_LINE)) {
                Matcher m = Pattern.compile(CLASS_LINE).matcher(line);
                if (m.find()) {
                    String obfName = m.group(1);
                    String deobfName = m.group(2);

                    this.remapper.addClassMapping(obfName, deobfName);
                } else {
                    error = "Could not parse class line: " + line;
                }
            } else if (line.matches(FIELD_LINE)) {
                Matcher m = Pattern.compile(FIELD_LINE).matcher(line);
                if (m.find()) {
                    String obfOwner = m.group(1);
                    String obfName = m.group(2);
                    String deobfName = m.group(4);

                    this.remapper.addFieldMapping(obfOwner, obfName, deobfName);
                } else {
                    error = "Could not parse field line: " + line;
                }
            } else if (line.matches(METHOD_LINE)) {
                Matcher m = Pattern.compile(METHOD_LINE).matcher(line);
                if (m.find()) {
                    String obfOwner = m.group(1);
                    String obfName = m.group(2);
                    String obfDesc = m.group(3);
                    String deobfName = m.group(5);

                    this.remapper.addMethodMapping(obfOwner, obfName, obfDesc, deobfName);
                } else {
                    error = "Could not parse method line: " + line;
                }
            } else {
                error = "Unknown line: " + line;
            }

            if (error != null) throw new IllegalStateException(error);
        }
    }

}
