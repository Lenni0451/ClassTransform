package net.lenni0451.classtransform.mixinstranslator;

import net.lenni0451.classtransform.mixinstranslator.impl.AnnotationTranslatorManager;
import net.lenni0451.classtransform.mixinstranslator.impl.IAnnotationTranslator;
import net.lenni0451.classtransform.transformer.ITransformerPreprocessor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Translate annotation from Mixins to ClassTransform<br>
 * Since ClassTransform has some differences to Mixins this is not a 100% perfect translation<br>
 * Some features may not be supported.<br>
 * Some fields which are not supported but still got copied over to simplify the copy-paste action<br>
 * You can recognize them by the @Deprecated annotation
 */
public class MixinsTranslator implements ITransformerPreprocessor {

    @Override
    public void process(ClassNode node) {
        this.transform(node.visibleAnnotations);
        this.transform(node.invisibleAnnotations);
        for (FieldNode field : node.fields) {
            this.transform(field.visibleAnnotations);
            this.transform(field.invisibleAnnotations);
        }
        for (MethodNode method : node.methods) {
            this.transform(method.visibleAnnotations);
            this.transform(method.invisibleAnnotations);

            CallbackRewriter.rewrite(method);
        }
    }

    private void transform(final List<AnnotationNode> annotations) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            IAnnotationTranslator translator = AnnotationTranslatorManager.getTranslator(Type.getType(annotation.desc));
            if (translator != null) translator.translate(annotation);
        }
    }

}
