package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.utils.IOSupplier;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A remapper that reads tiny v2 mappings.
 * <p>
 * The file starts with a {@code tiny 2 0} header followed by the namespace names, with the first
 * namespace being the one used by member descriptors. The {@code from} and {@code to} namespaces
 * must both appear in the header. Class, field and method mappings are loaded from the {@code from}
 * namespace to the {@code to} namespace; parameter mappings and comments are passed to the
 * overridable hooks.
 */
@ParametersAreNonnullByDefault
public class TinyV2Mapper extends AMapper {

    private final IOSupplier<InputStream> mappingsSupplier;
    private final String from;
    private final String to;

    /**
     * Create a tiny v2 mapper that reads versioned {@code tiny 2 0} mapping data from the given stream.
     *
     * @param config         The mapper config
     * @param mappingsStream The stream to read, closed after loading
     * @param from           The namespace to map from
     * @param to             The namespace to map to
     */
    public TinyV2Mapper(final MapperConfig config, @WillClose final InputStream mappingsStream, final String from, final String to) {
        super(config);
        this.mappingsSupplier = () -> mappingsStream;
        this.from = from;
        this.to = to;
    }

    /**
     * Create a tiny v2 mapper that opens the versioned mapping file lazily when the mapper is first used.
     *
     * @param config       The mapper config
     * @param mappingsFile The {@code tiny 2 0} mapping file to read
     * @param from         The namespace to map from
     * @param to           The namespace to map to
     */
    public TinyV2Mapper(final MapperConfig config, final File mappingsFile, final String from, final String to) {
        super(config);
        this.mappingsSupplier = () -> new FileInputStream(mappingsFile);
        this.from = from;
        this.to = to;
    }

    @Override
    protected void init() throws Throwable {
        MapRemapper descriptorRemapper = new MapRemapper();
        List<TempMapping> tempMappings = new ArrayList<>();
        List<String> lines = this.readLines(this.mappingsSupplier.get());

        int fromIndex = -1;
        int toIndex = -1;

        String currentClass = null;
        for (String line : lines) {
            String trimmedLine = line.trim().replaceAll("\\s{2,}", "\t");
            if (trimmedLine.isEmpty()) continue;

            String[] parts = trimmedLine.split("\t");
            if (fromIndex == -1) {
                this.verifyHeader(parts);

                String[] mappingNames = Arrays.copyOfRange(parts, 3, parts.length);
                List<String> mappingNamesList = Arrays.asList(mappingNames);
                fromIndex = mappingNamesList.indexOf(this.from);
                toIndex = mappingNamesList.indexOf(this.to);
                if (fromIndex == -1) throw new IllegalArgumentException("Unable to find from mapping '" + this.from + "'");
                if (toIndex == -1) throw new IllegalArgumentException("Unable to find to mapping '" + this.to + "'");
            } else if (line.startsWith("c\t")) {
                String baseName = parts[1];
                currentClass = parts[1 + fromIndex];
                String toName = parts[1 + toIndex];

                descriptorRemapper.addClassMapping(baseName, currentClass);
                this.remapper.addClassMapping(currentClass, toName);
                this.classParsed(baseName, currentClass, toName);
            } else if (line.startsWith("\tf\t")) {
                if (currentClass == null) throw new IllegalStateException("Field mapping without class mapping");
                String descriptor = parts[1];
                String fromName = parts[2 + fromIndex];
                String toName = parts[2 + toIndex];

                tempMappings.add(new TempMapping(false, currentClass, fromName, descriptor, toName));
                this.fieldParsed(currentClass, fromName, toName, descriptor);
            } else if (line.startsWith("\tm\t")) {
                if (currentClass == null) throw new IllegalStateException("Method mapping without class mapping");
                String descriptor = parts[1];
                String fromName = parts[2 + fromIndex];
                String toName = parts[2 + toIndex];

                tempMappings.add(new TempMapping(true, currentClass, fromName, descriptor, toName));
                this.methodParsed(currentClass, fromName, toName, descriptor);
            } else if (line.startsWith("\t\tp")) {
                this.parseParameter(currentClass, parts);
            } else if (trimmedLine.startsWith("c")) {
                this.parseComment(currentClass, line, parts);
            } else {
                throw new IllegalStateException("Unknown line: " + line);
            }
        }

        //Temp mappings are required because the descriptor needs to be remapped which is only possible after all class mappings are loaded
        for (TempMapping tempMapping : tempMappings) {
            if (tempMapping.method) {
                this.remapper.addMethodMapping(tempMapping.owner, tempMapping.name, descriptorRemapper.mapMethodDesc(tempMapping.descriptor), tempMapping.newName);
            } else {
                this.remapper.addFieldMapping(tempMapping.owner, tempMapping.name, descriptorRemapper.mapDesc(tempMapping.descriptor), tempMapping.newName);
            }
        }
        this.postInit(descriptorRemapper);
    }

    private void verifyHeader(final String[] parts) {
        if (!parts[0].equals("tiny")) throw new IllegalStateException("Invalid tiny header (magic)");
        if (!parts[1].equals("2")) throw new IllegalStateException("Invalid tiny header (major version)");
        if (!parts[2].equals("0")) throw new IllegalStateException("Invalid tiny header (minor version)");
        if (parts.length < 5) throw new IllegalStateException("Invalid tiny header (missing columns)");
    }

    /**
     * Called for every parsed class mapping.
     *
     * @param baseName The class name in the first namespace
     * @param fromName The class name in the from namespace
     * @param toName   The class name in the to namespace
     */
    protected void classParsed(final String baseName, final String fromName, final String toName) {
    }

    /**
     * Called for every parsed field mapping.
     *
     * @param currentClass The owner class in the from namespace
     * @param fromName     The field name in the from namespace
     * @param toName       The field name in the to namespace
     * @param descriptor   The field descriptor in the first namespace
     */
    protected void fieldParsed(@Nullable final String currentClass, final String fromName, final String toName, final String descriptor) {
    }

    /**
     * Called for every parsed method mapping.
     *
     * @param currentClass The owner class in the from namespace
     * @param fromName     The method name in the from namespace
     * @param toName       The method name in the to namespace
     * @param descriptor   The method descriptor in the first namespace
     */
    protected void methodParsed(@Nullable final String currentClass, final String fromName, final String toName, final String descriptor) {
    }

    /**
     * Called for every parsed parameter mapping.
     *
     * @param currentClass The owner class in the from namespace
     * @param parts        The tab-separated columns of the trimmed line, starting with the {@code p} marker
     */
    protected void parseParameter(@Nullable final String currentClass, final String[] parts) {
    }

    /**
     * Called for every parsed comment line.
     *
     * @param currentClass The class the comment belongs to, in the from namespace
     * @param line         The raw line including indentation
     * @param parts        The tab-separated columns of the trimmed line, starting with the {@code c} marker
     */
    protected void parseComment(@Nullable final String currentClass, final String line, final String[] parts) {
    }

    /**
     * Called after all mappings have been parsed.
     *
     * @param descriptorRemapper The remapper translating descriptors from the first namespace to the from namespace
     */
    protected void postInit(final MapRemapper descriptorRemapper) {
    }


    private static class TempMapping {
        private final boolean method;
        private final String owner;
        private final String name;
        private final String descriptor;
        private final String newName;

        private TempMapping(final boolean method, final String owner, final String name, final String descriptor, final String newName) {
            this.method = method;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
            this.newName = newName;
        }
    }

}
