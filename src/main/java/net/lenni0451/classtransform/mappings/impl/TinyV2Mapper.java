package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A remapper that uses tiny v2 mappings for remapping.<br>
 * The from and to namespaces are also required.
 */
@ParametersAreNonnullByDefault
public class TinyV2Mapper extends AMapper {

    private final File mappingFile;
    private final String from;
    private final String to;

    public TinyV2Mapper(final MapperConfig config, final File mappingFile, final String from, final String to) {
        super(config);
        this.mappingFile = mappingFile;
        this.from = from;
        this.to = to;
    }

    @Override
    protected void init() throws Throwable {
        MapRemapper descriptorRemapper = new MapRemapper();
        List<TempMapping> tempMappings = new ArrayList<>();
        List<String> lines = this.readLines(this.mappingFile);

        int fromIndex = -1;
        int toIndex = -1;

        String currentClass = null;
        for (String line : lines) {
            String trimmedLine = line.trim();
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
            } else if (line.startsWith("\tf\t")) {
                if (currentClass == null) throw new IllegalStateException("Field mapping without class mapping");
                String descriptor = parts[1];
                String fromName = parts[2 + fromIndex];
                String toName = parts[2 + toIndex];

                tempMappings.add(new TempMapping(false, currentClass, fromName, descriptor, toName));
            } else if (line.startsWith("\tm\t")) {
                if (currentClass == null) throw new IllegalStateException("Method mapping without class mapping");
                String descriptor = parts[1];
                String fromName = parts[2 + fromIndex];
                String toName = parts[2 + toIndex];

                tempMappings.add(new TempMapping(true, currentClass, fromName, descriptor, toName));
            } else if ((!line.startsWith("\t") || !trimmedLine.startsWith("c")) && !line.startsWith("\t\tp")) {
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
    }

    private void verifyHeader(final String[] parts) {
        if (!parts[0].equals("tiny")) throw new IllegalStateException("Invalid tiny header (magic)");
        if (!parts[1].equals("2")) throw new IllegalStateException("Invalid tiny header (major version)");
        if (!parts[2].equals("0")) throw new IllegalStateException("Invalid tiny header (minor version)");
        if (parts.length < 5) throw new IllegalStateException("Invalid tiny header (missing columns)");
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
