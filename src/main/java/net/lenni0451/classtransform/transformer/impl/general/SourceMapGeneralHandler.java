package net.lenni0451.classtransform.transformer.impl.general;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.smap.SourceMap;
import net.lenni0451.classtransform.utils.smap.SourceMapParser;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SourceMapGeneralHandler extends AnnotationHandler {

    private static final String DEFAULT_STRATUM = "ClassTransform";
    private static final int OFFSET = 50;

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        int maxTargetLine = ASMUtils.getMaxLineNumber(transformedClass);
        int maxTransformerLine = ASMUtils.getMaxLineNumber(transformer);
        ASMUtils.offsetLineNumbers(transformer, maxTargetLine + OFFSET);

        SourceMap sourceMap = this.getSourceMap(transformedClass);
        SourceMap.Section section = this.addSection(sourceMap, transformer);
        section.addSourceFile(new SourceMap.SourceFile(1, this.getSourceFile(transformedClass), transformedClass.name + ".java"));
        section.addSourceFile(new SourceMap.SourceFile(2, this.getSourceFile(transformer), transformer.name + ".java"));
        section.addLineNumber(new SourceMap.LineNumber(1, 1, maxTargetLine, 1, null));
        section.addLineNumber(new SourceMap.LineNumber(1, 2, maxTransformerLine, maxTargetLine + OFFSET, null));
        transformedClass.sourceDebug = sourceMap.toString();
    }

    private String getSourceFile(final ClassNode classNode) {
        String fileName = classNode.sourceFile;
        if (fileName == null) {
            fileName = classNode.name;
            if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            fileName = fileName + ".java";
        }
        return fileName;
    }

    private SourceMap getSourceMap(final ClassNode transformedClass) {
        SourceMap sourceMap = null;
        try {
            sourceMap = SourceMapParser.parse(transformedClass.sourceDebug);
        } catch (Throwable ignored) {
            //If a source map can not be parsed, overwrite it with a new one
        }
        if (sourceMap == null) sourceMap = new SourceMap(this.getSourceFile(transformedClass), DEFAULT_STRATUM);
        return sourceMap;
    }

    private SourceMap.Section addSection(final SourceMap sourceMap, final ClassNode transformer) {
        String sectionName = transformer.name;
        if (sectionName.contains("/")) sectionName = sectionName.substring(sectionName.lastIndexOf("/") + 1);
        sectionName = DEFAULT_STRATUM + "_" + sectionName;

        int i = 2;
        while (sourceMap.getSection(sectionName) != null) sectionName = sectionName + i++;

        SourceMap.Section section = new SourceMap.Section(sectionName);
        sourceMap.addSection(section);
        return section;
    }

}
