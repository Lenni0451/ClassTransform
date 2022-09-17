package net.lenni0451.classtransform.mixinstranslator;

import net.lenni0451.classtransform.mixinstranslator.classtranslator.*;
import net.lenni0451.classtransform.transformer.ITransformerPreprocessor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MixinsTranslator implements ITransformerPreprocessor {

    /*
     * TODO:
     *  - Inject shift is not the right one (At.Shift.BEFORE)
     *  - Callback infos
     */

    private final Map<String, IAnnotationTranslator> annotationTranslator = new HashMap<>();

    public MixinsTranslator() {
        this.annotationTranslator.put(Mixin.class.getName(), new MixinTranslator());
        this.annotationTranslator.put(Inject.class.getName(), new InjectTranslator());
        this.annotationTranslator.put(Redirect.class.getName(), new RedirectTranslator());
        this.annotationTranslator.put(ModifyConstant.class.getName(), new ModifyConstantTranslator());
        this.annotationTranslator.put(Overwrite.class.getName(), new OverwriteTranslator());
        this.annotationTranslator.put(At.class.getName(), new AtTranslator());
        this.annotationTranslator.put(Shadow.class.getName(), new ShadowTranslator());
        this.annotationTranslator.put(Slice.class.getName(), new SliceTranslator());
    }

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
            IAnnotationTranslator translator = this.annotationTranslator.get(Type.getType(annotation.desc).getClassName());
            if (translator != null) translator.translate(this.annotationTranslator, annotation);
        }
    }

}
