package net.lenni0451.classtransform.utils.smap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser for the source map format.<br>
 * A download for the specification can be obtained <a href="https://download.oracle.com/otndocs/jcp/dsol-1.0-fr-spec-oth-JSpec/">here</a>.
 */
public class SourceMapParser {

    private static final String SOURCE_FILE_PATTERN = "^(\\+\\s)?(\\d+)\\s(.+)$";
    private static final String LINE_NUMBER_PATTERN = "^(\\d+)(?>#(\\d+))?(?>,(\\d+))?:(\\d+)(?>,(\\d+))?$";

    @Nullable
    public static SourceMap parse(@Nullable final String smap) {
        if (smap == null || smap.trim().isEmpty()) return null;
        LineReader reader = new LineReader(smap.trim().split("\n"));
        SourceMap parsed = parse(reader);
        if (reader.hasNext()) throw new IllegalStateException("Unexpected extra lines in source map");
        return parsed;
    }

    @Nullable
    private static SourceMap parse(final LineReader reader) {
        if (!reader.hasNext()) return null;
        if (!reader.next().equals(SourceMap.HEADER)) throw new IllegalArgumentException("Unsupported source map format");

        SourceMap sourceMap = readHeader(reader);
        while (reader.hasNext()) {
            if (reader.peek().startsWith("*O ")) {
                sourceMap.addEmbeddedSection(readEmbeddedSection(reader));
            } else if (reader.peek().startsWith("*S ")) {
                sourceMap.addSection(readSection(reader));
            } else {
                break;
            }
        }
        if (!reader.hasNext() || !reader.next().equals("*E")) throw new IllegalArgumentException("Expected end of source map");
        return sourceMap;
    }

    private static SourceMap readHeader(final LineReader reader) {
        if (!reader.hasNext()) throw new IllegalArgumentException("Expected source map source");
        String source = reader.next();
        if (!reader.hasNext()) throw new IllegalArgumentException("Expected source map stratum");
        String stratum = reader.next();
        return new SourceMap(source, stratum);
    }

    private static SourceMap.EmbeddedSection readEmbeddedSection(final LineReader reader) {
        if (!reader.hasNext() || !reader.peek().startsWith("*O ")) throw new IllegalArgumentException("Expected embedded section start");
        SourceMap.EmbeddedSection embeddedSection = new SourceMap.EmbeddedSection(reader.next().substring(3));
        String close = "*C " + embeddedSection.getName();
        while (reader.hasNext() && !reader.peek().equals(close)) {
            SourceMap sourceMap = parse(reader);
            if (sourceMap == null) throw new IllegalArgumentException("Invalid embedded source map");
            embeddedSection.addSourceMap(sourceMap);
        }
        if (!reader.hasNext() || !reader.next().equals(close)) throw new IllegalArgumentException("Expected embedded section end");
        return embeddedSection;
    }

    private static SourceMap.Section readSection(final LineReader reader) {
        if (!reader.hasNext() || !reader.peek().startsWith("*S ")) throw new IllegalArgumentException("Expected section start");
        SourceMap.Section section = new SourceMap.Section(reader.next().substring(3));
        sections:
        while (reader.hasNext() && reader.peek().startsWith("*")) {
            switch (reader.peek()) {
                case "*F":
                    for (SourceMap.SourceFile sourceFile : readSourceFiles(reader)) section.addSourceFile(sourceFile);
                    break;
                case "*L":
                    for (SourceMap.LineNumber lineNumber : readLineNumbers(reader)) section.addLineNumber(lineNumber);
                    break;
                case "*V":
                    for (String vendorLine : readVendorLines(reader)) section.addVendorLine(vendorLine);
                    break;
                default:
                    break sections;
            }
        }
        return section;
    }

    private static List<SourceMap.SourceFile> readSourceFiles(final LineReader reader) {
        if (!reader.hasNext() || !reader.next().equals("*F")) throw new IllegalArgumentException("Expected source file start");
        List<SourceMap.SourceFile> sourceFiles = new ArrayList<>();
        while (reader.hasNext() && !reader.peek().startsWith("*")) {
            String line = reader.next();
            Matcher matcher = Pattern.compile(SOURCE_FILE_PATTERN).matcher(line);
            if (!matcher.matches()) throw new IllegalArgumentException("Invalid source file line: " + line);
            sourceFiles.add(new SourceMap.SourceFile(Integer.parseInt(matcher.group(2)), matcher.group(3), matcher.group(1) == null ? null : reader.next()));
        }
        return sourceFiles;
    }

    private static List<SourceMap.LineNumber> readLineNumbers(final LineReader reader) {
        if (!reader.hasNext() || !reader.next().equals("*L")) throw new IllegalArgumentException("Expected line number start");
        List<SourceMap.LineNumber> lineNumbers = new ArrayList<>();
        while (reader.hasNext() && !reader.peek().startsWith("*")) {
            String line = reader.next();
            Matcher matcher = Pattern.compile(LINE_NUMBER_PATTERN).matcher(line);
            if (!matcher.matches()) throw new IllegalArgumentException("Invalid line number line: " + line);
            lineNumbers.add(new SourceMap.LineNumber(Integer.parseInt(matcher.group(1)), matcher.group(2) == null ? null : Integer.valueOf(matcher.group(2)), matcher.group(3) == null ? null : Integer.valueOf(matcher.group(3)), Integer.parseInt(matcher.group(4)), matcher.group(5) == null ? null : Integer.valueOf(matcher.group(5))));
        }
        return lineNumbers;
    }

    private static List<String> readVendorLines(final LineReader reader) {
        if (!reader.hasNext() || !reader.next().equals("*V")) throw new IllegalArgumentException("Expected vendor line start");
        List<String> vendorLines = new ArrayList<>();
        while (reader.hasNext() && !reader.peek().startsWith("*")) vendorLines.add(reader.next());
        return vendorLines;
    }


    private static class LineReader {
        private final String[] lines;
        private int index = 0;

        private LineReader(final String[] lines) {
            this.lines = lines;
        }

        public String next() {
            return this.lines[this.index++];
        }

        public String peek() {
            return this.lines[this.index];
        }

        public boolean hasNext() {
            return this.index < this.lines.length;
        }
    }

}
