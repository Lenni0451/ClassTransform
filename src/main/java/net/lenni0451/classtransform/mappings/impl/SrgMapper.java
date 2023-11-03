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

/**
 * A remapper that uses srg mappings for remapping.
 */
@ParametersAreNonnullByDefault
public class SrgMapper extends AMapper {

    private static final String CLASS_LINE = "^CL: (\\S+) (\\S+)$";
    private static final String FIELD_LINE = "^FD: (\\S+)/(\\S+) (\\S+)/(\\S+)$";
    private static final String METHOD_LINE = "^MD: (\\S+)/(\\S+) (\\(\\S*\\)\\S+) (\\S+)/(\\S+) (\\(\\S*\\)\\S+)$";

    private final IOSupplier<InputStream> mappingsSupplier;

    public SrgMapper(final MapperConfig config, @WillClose final InputStream mappingsStream) {
        super(config);
        this.mappingsSupplier = () -> mappingsStream;
    }

    public SrgMapper(final MapperConfig config, final File mappingsFile) {
        super(config);
        this.mappingsSupplier = () -> new FileInputStream(mappingsFile);
    }

    @Override
    protected void init() throws Throwable {
        for (String line : this.readLines(this.mappingsSupplier.get())) {
            if (line.trim().isEmpty()) continue;

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
