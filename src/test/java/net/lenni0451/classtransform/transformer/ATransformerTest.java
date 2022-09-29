package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CTransformer;
import net.lenni0451.classtransform.mappings.impl.VoidMapper;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.test.SCalculator;
import net.lenni0451.classtransform.test.VCalculator;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.annotations.AnnotationParser;
import net.lenni0451.classtransform.utils.log.DefaultLogger;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.junit.jupiter.api.BeforeEach;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public abstract class ATransformerTest {

    protected final VoidMapper voidMapper = new VoidMapper();
    protected IClassProvider classProvider = new BasicClassProvider();
    protected TransformerManager transformerManager = new TransformerManager(this.classProvider);
    protected Map<String, IInjectionTarget> injectionTargets;
    protected ClassNode staticCalculatorClass;
    protected ClassNode virtualCalculatorClass;

    @BeforeEach
    public void setUp() {
        try {
            Field f = TransformerManager.class.getDeclaredField("injectionTargets");
            f.setAccessible(true);
            this.injectionTargets = (Map<String, IInjectionTarget>) f.get(this.transformerManager);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to get injection targets", t);
        }
        this.staticCalculatorClass = ASMUtils.fromBytes(this.classProvider.getClass(SCalculator.class.getName()));
        this.virtualCalculatorClass = ASMUtils.fromBytes(this.classProvider.getClass(VCalculator.class.getName()));
    }

    protected ClassNode getTransformerClass(final String name) {
        ClassNode transformer = ASMUtils.fromBytes(this.classProvider.getClass(name));
        List<Object> annotation = transformer.invisibleAnnotations.stream().filter(a -> a.desc.equals(Type.getDescriptor(CTransformer.class))).map(a -> a.values).findFirst().orElse(null);
        if (annotation != null) {
            CTransformer cTransformer = AnnotationParser.parse(CTransformer.class, this.classProvider, AnnotationParser.listToMap(annotation));
            Class<?> targetClass = cTransformer.value()[0];
            ClassNode targetNode = ASMUtils.fromBytes(this.classProvider.getClass(targetClass.getName()));
            this.voidMapper.mapClass(this.classProvider, new DefaultLogger(), targetNode, transformer);
        }
        return transformer;
    }

}
