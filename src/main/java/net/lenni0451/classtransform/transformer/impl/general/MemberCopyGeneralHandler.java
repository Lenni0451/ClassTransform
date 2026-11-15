package net.lenni0451.classtransform.transformer.impl.general;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.AnnotationHandler;
import net.lenni0451.classtransform.transformer.impl.general.membercopy.FieldMerger;
import net.lenni0451.classtransform.transformer.impl.general.membercopy.InitializerMerger;
import net.lenni0451.classtransform.transformer.impl.general.membercopy.InterfaceMerger;
import net.lenni0451.classtransform.transformer.impl.general.membercopy.MethodMerger;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Copy members from the transformer class to the transformed class.<br>
 * This also merges initializers.
 * <p>
 * The handler is registered twice: before the injection handlers it merges interfaces, fields and initializers, after them it merges the remaining methods.
 */
@ParametersAreNonnullByDefault
public class MemberCopyGeneralHandler extends AnnotationHandler {

    private final boolean pre;

    /**
     * Creates a new member copy handler.
     *
     * @param pre Whether to merge interfaces, fields and initializers instead of methods
     */
    public MemberCopyGeneralHandler(final boolean pre) {
        this.pre = pre;
    }

    @Override
    public void transform(TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer) {
        if (this.pre) {
            InterfaceMerger.mergeInterfaces(transformedClass, transformer);
            FieldMerger.mergeFields(transformedClass, transformer);
            InitializerMerger.mergeInitializers(transformedClass, transformer);
        } else {
            MethodMerger.mergeMethods(transformedClass, transformer);
        }
    }

}
