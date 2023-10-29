package net.lenni0451.classtransform.utils.smap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a source map class file attribute.<br>
 * It is used by debuggers to map the bytecode line numbers to the source file line numbers.
 */
public class SourceMap {

    public static final String HEADER = "SMAP";

    private final String source;
    private final String stratum;
    private final List<EmbeddedSection> embeddedSections = new ArrayList<>();
    private final List<Section> sections = new ArrayList<>();

    public SourceMap(final String source, final String stratum) {
        this.source = source;
        this.stratum = stratum;
    }

    public String getSource() {
        return this.source;
    }

    public String getStratum() {
        return this.stratum;
    }

    public List<EmbeddedSection> getEmbeddedSections() {
        return this.embeddedSections;
    }

    public void addEmbeddedSection(final EmbeddedSection embeddedSection) {
        this.embeddedSections.add(embeddedSection);
    }

    public List<Section> getSections() {
        return this.sections;
    }

    public Section getSection(final String name) {
        for (Section section : this.sections) {
            if (section.getName().equals(name)) return section;
        }
        return null;
    }

    public void addSection(final Section section) {
        this.sections.add(section);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(HEADER + "\n" + this.source + "\n" + this.stratum + "\n");
        for (EmbeddedSection embeddedSection : this.embeddedSections) out.append(embeddedSection);
        for (Section section : this.sections) out.append(section);
        return out + "*E\n";
    }


    public static class EmbeddedSection {
        private final String name;
        private final List<SourceMap> sourceMaps = new ArrayList<>();

        public EmbeddedSection(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public List<SourceMap> getSourceMaps() {
            return this.sourceMaps;
        }

        public void addSourceMap(final SourceMap sourceMap) {
            this.sourceMaps.add(sourceMap);
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder("*O ").append(this.name).append("\n");
            for (SourceMap sourceMap : this.sourceMaps) out.append(sourceMap);
            return out.append("*C ").append(this.name).append("\n").toString();
        }
    }

    public static class Section {
        private final String name;
        private final List<SourceFile> sourceFiles = new ArrayList<>();
        private final List<LineNumber> lineNumbers = new ArrayList<>();
        private final List<String> vendorLines = new ArrayList<>();

        public Section(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public List<SourceFile> getSourceFiles() {
            return this.sourceFiles;
        }

        public void addSourceFile(final SourceFile sourceFile) {
            this.sourceFiles.add(sourceFile);
        }

        public List<LineNumber> getLineNumbers() {
            return this.lineNumbers;
        }

        public void addLineNumber(final LineNumber lineNumber) {
            this.lineNumbers.add(lineNumber);
        }

        public List<String> getVendorLines() {
            return this.vendorLines;
        }

        public void addVendorLine(final String vendorLine) {
            this.vendorLines.add(vendorLine);
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder("*S ").append(this.name).append("\n");
            if (!this.sourceFiles.isEmpty()) {
                out.append("*F\n");
                for (SourceFile sourceFile : this.sourceFiles) out.append(sourceFile);
            }
            if (!this.lineNumbers.isEmpty()) {
                out.append("*L\n");
                for (LineNumber lineNumber : this.lineNumbers) out.append(lineNumber);
            }
            if (!this.vendorLines.isEmpty()) {
                out.append("*V\n");
                for (String vendorLine : this.vendorLines) out.append(vendorLine).append("\n");
            }
            return out.toString();
        }
    }

    public static class SourceFile {
        private final int id;
        private final String name;
        @Nullable
        private final String path;

        public SourceFile(final int id, final String name) {
            this(id, name, null);
        }

        public SourceFile(final int id, final String name, @Nullable final String path) {
            this.id = id;
            this.name = name;
            this.path = path;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        @Nullable
        public String getPath() {
            return this.path;
        }

        @Override
        public String toString() {
            String out = "";
            if (this.path != null) out += "+ ";
            out += this.id + " " + this.name;
            if (this.path != null) out += "\n" + this.path;
            return out + "\n";
        }
    }

    public static class LineNumber {
        private final int inputStart;
        @Nullable
        private final Integer fileId;
        @Nullable
        private final Integer repeatCount;
        private final int outputStart;
        @Nullable
        private final Integer outputIncrement;

        public LineNumber(final int inputStart, @Nullable final Integer fileId, @Nullable final Integer repeatCount, final int outputStart, @Nullable final Integer outputIncrement) {
            this.inputStart = inputStart;
            this.fileId = fileId;
            this.repeatCount = repeatCount;
            this.outputStart = outputStart;
            this.outputIncrement = outputIncrement;
        }

        public int getInputStart() {
            return this.inputStart;
        }

        @Nullable
        public Integer getFileId() {
            return this.fileId;
        }

        @Nullable
        public Integer getRepeatCount() {
            return this.repeatCount;
        }

        public int getOutputStart() {
            return this.outputStart;
        }

        @Nullable
        public Integer getOutputIncrement() {
            return this.outputIncrement;
        }

        @Override
        public String toString() {
            String out = String.valueOf(this.inputStart);
            if (this.fileId != null) out += "#" + this.fileId;
            if (this.repeatCount != null) out += "," + this.repeatCount;
            out += ":" + this.outputStart;
            if (this.outputIncrement != null) out += "," + this.outputIncrement;
            return out + "\n";
        }
    }

}
