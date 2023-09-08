package net.lenni0451.classtransform.transformer;

import lombok.SneakyThrows;
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.mappings.impl.VoidMapper;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.annotations.AnnotationUtils;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.classtransform.utils.tree.ClassTree;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.junit.jupiter.api.BeforeEach;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public abstract class AnnotationHandlerTest {

    protected final VoidMapper voidMapper = new VoidMapper();
    protected ClassTree classTree = new ClassTree();
    protected IClassProvider classProvider = new BasicClassProvider();
    protected TransformerManager transformerManager = new TransformerManager(this.classProvider);
    protected ClassNode staticCalculatorClass;
    protected ClassNode virtualCalculatorClass;

    @BeforeEach
    public void setUp() throws ClassNotFoundException {
        this.staticCalculatorClass = ASMUtils.fromBytes(this.classProvider.getClass(SCalculator.class.getName()));
        this.virtualCalculatorClass = ASMUtils.fromBytes(this.classProvider.getClass(VCalculator.class.getName()));
    }

    @SneakyThrows
    protected ClassNode getTransformerClass(final String name) {
        ClassNode transformer = ASMUtils.fromBytes(this.classProvider.getClass(name));
        List<Object> annotation = AnnotationUtils.findInvisibleAnnotation(transformer, CTransformer.class).map(a -> a.values).orElse(null);
        if (annotation != null) {
            CTransformer cTransformer = AnnotationParser.parse(CTransformer.class, this.classTree, this.classProvider, AnnotationParser.listToMap(annotation));
            Class<?> targetClass = cTransformer.value()[0];
            ClassNode targetNode = ASMUtils.fromBytes(this.classProvider.getClass(targetClass.getName()));
            this.voidMapper.mapClass(this.classTree, this.classProvider, targetNode, transformer);
        }
        return transformer;
    }

}
