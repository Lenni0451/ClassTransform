package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.utils.IOSupplier;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A remapper that uses tiny v1 mappings for remapping.<br>
 * The from and to namespaces are also required.
 */
@ParametersAreNonnullByDefault
public class TinyV1Mapper extends AMapper {

    private final IOSupplier<InputStream> mappingsSupplier;
    private final String from;
    private final String to;

    public TinyV1Mapper(final MapperConfig config, @WillClose final InputStream mappingsStream, final String from, final String to) {
        super(config);
        this.mappingsSupplier = () -> mappingsStream;
        this.from = from;
        this.to = to;
    }

    public TinyV1Mapper(final MapperConfig config, final File mappingsFile, final String from, final String to) {
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
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.trim().split("\t+");
            if (fromIndex == -1) {
                this.verifyHeader(parts);

                String[] mappingNames = Arrays.copyOfRange(parts, 1, parts.length);
                List<String> mappingNamesList = Arrays.asList(mappingNames);
                fromIndex = mappingNamesList.indexOf(this.from);
                toIndex = mappingNamesList.indexOf(this.to);
                if (fromIndex == -1) throw new IllegalArgumentException("Unable to find from mapping '" + this.from + "'");
                if (toIndex == -1) throw new IllegalArgumentException("Unable to find to mapping '" + this.to + "'");
            } else if (line.startsWith("CLASS\t")) {
                String baseName = parts[1];
                String fromName = parts[1 + fromIndex];
                String toName = parts[1 + toIndex];

                descriptorRemapper.addClassMapping(baseName, fromName);
                this.remapper.addClassMapping(fromName, toName);
            } else if (line.startsWith("FIELD")) {
                String descriptor = parts[2];
                String fromName = parts[3 + fromIndex];
                String toName = parts[3 + toIndex];

                tempMappings.add(new TempMapping(false, parts[1], fromName, descriptor, toName));
            } else if (line.startsWith("METHOD")) {
                String descriptor = parts[2];
                String fromName = parts[3 + fromIndex];
                String toName = parts[3 + toIndex];

                tempMappings.add(new TempMapping(true, parts[1], fromName, descriptor, toName));
            } else {
                throw new IllegalStateException("Unknown line: " + line);
            }
        }
        //Temp mappings are required because the descriptor needs to be remapped which is only possible after all class mappings are loaded
        for (TempMapping tempMapping : tempMappings) {
            if (tempMapping.method) {
                this.remapper.addMethodMapping(descriptorRemapper.mapSafe(tempMapping.owner), tempMapping.name, descriptorRemapper.mapMethodDesc(tempMapping.descriptor), tempMapping.newName);
            } else {
                this.remapper.addFieldMapping(descriptorRemapper.mapSafe(tempMapping.owner), tempMapping.name, descriptorRemapper.mapDesc(tempMapping.descriptor), tempMapping.newName);
            }
        }
    }

    private void verifyHeader(final String[] parts) {
        if (!parts[0].equals("v1")) throw new IllegalStateException("Invalid tiny header (magic)");
        if (parts.length < 3) throw new IllegalStateException("Invalid tiny header (missing columns)");
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
