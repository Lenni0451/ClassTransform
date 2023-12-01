package net.lenni0451.classtransform.mixinstranslator;

import net.lenni0451.classtransform.mixinstranslator.impl.AnnotationTranslatorManager;
import net.lenni0451.classtransform.mixinstranslator.impl.IAnnotationTranslator;
import net.lenni0451.classtransform.transformer.IAnnotationHandlerPreprocessor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Translate annotations from Mixins to ClassTransform.<br>
 * Since ClassTransform has some differences to Mixins this is not a 100% perfect translation.<br>
 * Some features may not be supported.<br>
 * Some fields which are not supported still got copied over to simplify the copy-paste action.<br>
 * You can recognize them by the @Deprecated annotation.
 */
@ParametersAreNonnullByDefault
public class MixinsTranslator implements IAnnotationHandlerPreprocessor {

    @Override
    public void process(ClassNode node) {
        this.translate(node.visibleAnnotations);
        this.translate(node.invisibleAnnotations);
        for (FieldNode field : node.fields) {
            this.translate(field.visibleAnnotations);
            this.translate(field.invisibleAnnotations);
        }
        for (MethodNode method : node.methods) {
            this.translate(method.visibleAnnotations);
            this.translate(method.invisibleAnnotations);
            this.translate(method.visibleParameterAnnotations);
            this.translate(method.invisibleParameterAnnotations);

            CallbackRewriter.rewrite(method);
        }
    }

    private void translate(@Nullable final List<AnnotationNode>[] annotations) {
        if (annotations == null) return;
        for (List<AnnotationNode> annotationList : annotations) this.translate(annotationList);
    }

    private void translate(@Nullable final List<AnnotationNode> annotations) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            IAnnotationTranslator translator = AnnotationTranslatorManager.getTranslator(Type.getType(annotation.desc));
            if (translator != null) translator.translate(annotation);
        }
    }

}
