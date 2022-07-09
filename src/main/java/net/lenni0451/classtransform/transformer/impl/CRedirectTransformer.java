package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.MethodNotFoundException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.impl.credirect.*;
import net.lenni0451.classtransform.transformer.types.ARemovingTransformer;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.Codifier;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CRedirectTransformer extends ARemovingTransformer<CRedirect> {

    private final Map<String, IRedirectTarget> redirectTargets = new HashMap<>();

    public CRedirectTransformer() {
        super(CRedirect.class);

        this.redirectTargets.put("INVOKE", new CRedirectInvoke());
        this.redirectTargets.put("GETFIELD", new CRedirectGetField());
        this.redirectTargets.put("PUTFIELD", new CRedirectPutField());
        this.redirectTargets.put("NEW", new CRedirectNew());
    }

    @Override
    public void transform(CRedirect annotation, TransformerManager transformerManager, IClassProvider classProvider, Map<String, IInjectionTarget> injectionTargets, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod) {
        IInjectionTarget iInjectionTarget = injectionTargets.get(annotation.target().value().toUpperCase(Locale.ROOT));
        IRedirectTarget iRedirectTarget = this.redirectTargets.get(annotation.target().value().toUpperCase(Locale.ROOT));
        if (iInjectionTarget == null || iRedirectTarget == null) {
            throw new InvalidTargetException(transformerMethod, transformer, annotation.target().value(), this.redirectTargets.keySet());
        }

        for (String targetCombi : annotation.method()) {
            List<MethodNode> targets = ASMUtils.getMethodsFromCombi(transformedClass, targetCombi);
            if (targets.isEmpty()) throw new MethodNotFoundException(transformedClass, transformer, targetCombi);
            for (MethodNode target : targets) {
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
    }

}
