package net.lenni0451.classtransform.mappings.impl.special;

import lombok.Data;
import lombok.With;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.mappings.impl.TinyV2Mapper;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillClose;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the {@link TinyV2Mapper} which also parses metadata (comments and parameter names) from the mappings.
 */
@ParametersAreNonnullByDefault
public class MetaTinyV2Mapper extends TinyV2Mapper {

    private final MapRemapper descriptorMapper = new MapRemapper();
    private final List<ClassMetadata> metadata = new ArrayList<>();
    private ClassMetadata currentClassMetadata = null;
    private FieldMetadata currentFieldMetadata = null;
    private MethodMetadata currentMethodMetadata = null;
    private ParameterMetadata currentParameterMetadata = null;

    /**
     * Create a mapper that reads the mapping from a stream.
     *
     * @param config         The mapper config
     * @param mappingsStream The stream to read, closed after loading
     * @param from           The namespace to map from
     * @param to             The namespace to map to
     */
    public MetaTinyV2Mapper(final MapperConfig config, @WillClose final InputStream mappingsStream, final String from, final String to) {
        super(config, mappingsStream, from, to);
    }

    /**
     * Create a mapper that reads the mapping from a file.
     *
     * @param config       The mapper config
     * @param mappingsFile The mapping file to read
     * @param from         The namespace to map from
     * @param to           The namespace to map to
     */
    public MetaTinyV2Mapper(final MapperConfig config, final File mappingsFile, final String from, final String to) {
        super(config, mappingsFile, from, to);
    }

    /**
     * Get the parsed metadata.<br>
     * All class/field/method names and descriptors are in the 'to' namespace.
     * <p>
     * The returned list is the internal mutable list in mapping file order.
     *
     * @return The parsed metadata
     */
    public List<ClassMetadata> getMetadata() {
        return this.metadata;
    }

    @Override
    protected void classParsed(String baseName, String fromName, String toName) {
        this.descriptorMapper.addClassMapping(baseName, toName);
        this.updateMetadata(UpdateLevel.CLASS);
        this.currentClassMetadata = new ClassMetadata(toName, null, new ArrayList<>(), new ArrayList<>());
    }

    @Override
    protected void fieldParsed(@Nullable String currentClass, String fromName, String toName, String descriptor) {
        if (this.currentClassMetadata == null) throw new IllegalStateException("Field mapping without class mapping");
        this.updateMetadata(UpdateLevel.FIELD);
        this.currentFieldMetadata = new FieldMetadata(toName, descriptor, null);
    }

    @Override
    protected void methodParsed(@Nullable String currentClass, String fromName, String toName, String descriptor) {
        if (this.currentClassMetadata == null) throw new IllegalStateException("Method mapping without class mapping");
        this.updateMetadata(UpdateLevel.METHOD);
        this.currentMethodMetadata = new MethodMetadata(toName, descriptor, null, new ArrayList<>());
    }

    @Override
    protected void parseParameter(@Nullable final String currentClass, final String[] parts) {
        if (this.currentMethodMetadata == null) throw new IllegalStateException("Parameter mapping without method mapping");
        int index = Integer.parseInt(parts[1]);
        String name = parts[2];

        this.updateMetadata(UpdateLevel.PARAMETER);
        this.currentParameterMetadata = new ParameterMetadata(index, name, null);
    }

    @Override
    protected void parseComment(@Nullable String currentClass, String line, String[] parts) {
        if (line.startsWith("\t\t\t")) { //Parameter comment
            if (this.currentParameterMetadata == null) throw new IllegalStateException("Comment without parameter mapping");
            this.currentParameterMetadata = this.currentParameterMetadata.withComment(String.join("\t", Arrays.copyOfRange(parts, 1, parts.length)));
        } else if (line.startsWith("\t\t")) { //Method/Field comment
            if (this.currentFieldMetadata != null && this.currentMethodMetadata != null) throw new IllegalStateException("Field and method metadata at the same time");
            if (this.currentFieldMetadata != null) {
                this.currentFieldMetadata = this.currentFieldMetadata.withComment(String.join("\t", Arrays.copyOfRange(parts, 1, parts.length)));
            } else if (this.currentMethodMetadata != null) {
                this.currentMethodMetadata = this.currentMethodMetadata.withComment(String.join("\t", Arrays.copyOfRange(parts, 1, parts.length)));
            } else {
                throw new IllegalStateException("Comment without field or method mapping");
            }
        } else if (line.startsWith("\t")) { //Class comment
            if (this.currentClassMetadata == null) throw new IllegalStateException("Comment without class mapping");
            this.currentClassMetadata = this.currentClassMetadata.withComment(String.join("\t", Arrays.copyOfRange(parts, 1, parts.length)));
        } else {
            //Lines starting with 'c\t' are already handled by the class mappings
            throw new IllegalStateException("Unknown line: " + line);
        }
    }

    @Override
    protected void postInit(MapRemapper descriptorRemapper) {
        this.updateMetadata(UpdateLevel.CLASS); //Add the last metadata

        List<ClassMetadata> remappedMetadata = new ArrayList<>();
        for (ClassMetadata classMetadata : this.metadata) {
            classMetadata = classMetadata.withName(this.remapper.mapSafe(classMetadata.getName()));
            remappedMetadata.add(classMetadata);

            List<FieldMetadata> remappedFields = new ArrayList<>();
            for (FieldMetadata field : classMetadata.fields) {
                remappedFields.add(field.withDescriptor(this.descriptorMapper.mapDesc(field.getDescriptor())));
            }
            classMetadata.fields.clear();
            classMetadata.fields.addAll(remappedFields);

            List<MethodMetadata> remappedMethods = new ArrayList<>();
            for (MethodMetadata method : classMetadata.methods) {
                remappedMethods.add(method.withDescriptor(this.descriptorMapper.mapMethodDesc(method.getDescriptor())));
            }
            classMetadata.methods.clear();
            classMetadata.methods.addAll(remappedMethods);
        }
        this.metadata.clear();
        this.metadata.addAll(remappedMetadata);
    }

    private void updateMetadata(final UpdateLevel level) {
        UpdateLevel[] updates;
        switch (level) {
            case CLASS:
                updates = new UpdateLevel[]{UpdateLevel.PARAMETER, UpdateLevel.METHOD, UpdateLevel.FIELD, UpdateLevel.CLASS};
                break;
            case FIELD:
            case METHOD:
                updates = new UpdateLevel[]{UpdateLevel.PARAMETER, UpdateLevel.METHOD, UpdateLevel.FIELD};
                break;
            case PARAMETER:
                updates = new UpdateLevel[]{UpdateLevel.PARAMETER};
                break;
            default:
                throw new IllegalArgumentException("Unknown update level: " + level);
        }
        for (UpdateLevel update : updates) {
            switch (update) {
                case CLASS:
                    if (this.currentClassMetadata != null && !this.currentClassMetadata.isEmpty()) {
                        this.metadata.add(this.currentClassMetadata);
                    }
                    this.currentClassMetadata = null;
                    break;
                case FIELD:
                    if (this.currentFieldMetadata != null && !this.currentFieldMetadata.isEmpty()) {
                        this.currentClassMetadata.getFields().add(this.currentFieldMetadata);
                    }
                    this.currentFieldMetadata = null;
                    break;
                case METHOD:
                    if (this.currentMethodMetadata != null && !this.currentMethodMetadata.isEmpty()) {
                        this.currentClassMetadata.getMethods().add(this.currentMethodMetadata);
                    }
                    this.currentMethodMetadata = null;
                    break;
                case PARAMETER:
                    if (this.currentParameterMetadata != null) {
                        this.currentMethodMetadata.getParameters().add(this.currentParameterMetadata);
                    }
                    this.currentParameterMetadata = null;
                    break;
            }
        }
    }


    /**
     * Metadata of a class parsed from the mappings.
     */
    @Data
    @With
    public static class ClassMetadata {
        /**
         * The class name in the to namespace.
         */
        private final String name;
        /**
         * The class comment, or null if the mapping has none.
         */
        @Nullable
        private final String comment;
        /**
         * The metadata of the class fields; mutable and owned by this instance.
         */
        private final List<FieldMetadata> fields;
        /**
         * The metadata of the class methods; mutable and owned by this instance.
         */
        private final List<MethodMetadata> methods;

        /**
         * Check if this class has no comment and no non-empty member metadata.
         *
         * @return If this metadata contains no information
         */
        public boolean isEmpty() {
            return this.comment == null && this.fields.stream().allMatch(FieldMetadata::isEmpty) && this.methods.stream().allMatch(MethodMetadata::isEmpty);
        }
    }

    /**
     * Metadata of a field parsed from the mappings.
     */
    @Data
    @With
    public static class FieldMetadata {
        /**
         * The field name in the to namespace.
         */
        private final String name;
        /**
         * The field descriptor in the to namespace.
         */
        private final String descriptor;
        /**
         * The field comment, or null if the mapping has none.
         */
        @Nullable
        private final String comment;

        /**
         * Check if this field has no comment.
         *
         * @return If this metadata contains no information
         */
        public boolean isEmpty() {
            return this.comment == null;
        }
    }

    /**
     * Metadata of a method parsed from the mappings.
     */
    @Data
    @With
    public static class MethodMetadata {
        /**
         * The method name in the to namespace.
         */
        private final String name;
        /**
         * The method descriptor in the to namespace.
         */
        private final String descriptor;
        /**
         * The method comment, or null if the mapping has none.
         */
        @Nullable
        private final String comment;
        /**
         * The metadata of the method parameters; mutable and owned by this instance.
         */
        private final List<ParameterMetadata> parameters;

        /**
         * Check if this method has no comment and no parameters.
         *
         * @return If this metadata contains no information
         */
        public boolean isEmpty() {
            return this.comment == null && this.parameters.isEmpty();
        }
    }

    /**
     * Metadata of a method parameter parsed from the mappings.
     */
    @Data
    @With
    public static class ParameterMetadata {
        /**
         * The parameter index as written in the mapping file.
         */
        private final int index;
        /**
         * The parameter name.
         */
        private final String name;
        /**
         * The parameter comment, or null if the mapping has none.
         */
        @Nullable
        private final String comment;
    }

    private enum UpdateLevel {
        CLASS,
        FIELD,
        METHOD,
        PARAMETER
    }

}
