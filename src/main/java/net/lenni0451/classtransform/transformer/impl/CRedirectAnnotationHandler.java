package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.impl.credirect.CRedirectField;
import net.lenni0451.classtransform.transformer.impl.credirect.CRedirectInvoke;
import net.lenni0451.classtransform.transformer.impl.credirect.CRedirectNew;
import net.lenni0451.classtransform.transformer.impl.credirect.IRedirectTarget;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The annotation handler for the {@link CRedirect} annotation.
 */
@ParametersAreNonnullByDefault
public class CRedirectAnnotationHandler extends RemovingTargetAnnotationHandler<CRedirect> {

    private final Map<String, IRedirectTarget> redirectTargets = new HashMap<>();

    public CRedirectAnnotationHandler() {
        super(CRedirect.class, CRedirect::method);

        this.redirectTargets.put("INVOKE", new CRedirectInvoke());
        this.redirectTargets.put("FIELD", new CRedirectField());
        this.redirectTargets.put("GETFIELD", new CRedirectField());
        this.redirectTargets.put("PUTFIELD", new CRedirectField());
        this.redirectTargets.put("NEW", new CRedirectNew());
    }

    @Override
    public void transform(CRedirect annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        IInjectionTarget iInjectionTarget = injectionTargets.get(annotation.target().value().toUpperCase(Locale.ROOT));
        IRedirectTarget iRedirectTarget = this.redirectTargets.get(annotation.target().value().toUpperCase(Locale.ROOT));
        if (iInjectionTarget == null || iRedirectTarget == null) {
            throw new InvalidTargetException(transformerMethod, transformer, annotation.target().value(), this.redirectTargets.keySet());
        }

        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            boolean isStatic = Modifier.isStatic(target.access);
            throw new TransformerException(transformerMethod, transformer, "must " + (isStatic ? "" : "not ") + "be static")
                    .help(Codifier.of(transformerMethod).access(isStatic ? transformerMethod.access | Modifier.STATIC : transformerMethod.access & ~Modifier.STATIC));
        }

        List<AbstractInsnNode> injectionInstructions = iInjectionTarget.getTargets(injectionTargets, target, annotation.target(), annotation.slice());
        if (injectionInstructions == null) {
            throw new TransformerException(transformerMethod, transformer, "has invalid member declaration '" + annotation.target().target() + "'")
                    .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
        }
        if (injectionInstructions.isEmpty() && !annotation.target().optional()) {
            throw new TransformerException(transformerMethod, transformer, "target '" + annotation.target().target() + "' could not be found")
                    .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
        }

        this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CRedirect");
        iRedirectTarget.inject(transformedClass, target, transformer, transformerMethod, injectionInstructions);
    }

}
