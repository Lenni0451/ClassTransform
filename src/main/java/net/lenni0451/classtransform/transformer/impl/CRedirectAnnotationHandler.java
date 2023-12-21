package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.impl.credirect.CRedirectField;
import net.lenni0451.classtransform.transformer.impl.credirect.CRedirectInvoke;
import net.lenni0451.classtransform.transformer.impl.credirect.CRedirectNew;
import net.lenni0451.classtransform.transformer.types.RemovingTargetAnnotationHandler;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * The annotation handler for the {@link CRedirect} annotation.
 */
@ParametersAreNonnullByDefault
public class CRedirectAnnotationHandler extends RemovingTargetAnnotationHandler<CRedirect> {

    private static final Collection<String> TARGETS = Arrays.asList("INVOKE", "FIELD", "NEW");

    private final CRedirectInvoke redirectInvoke = new CRedirectInvoke();
    private final CRedirectField redirectField = new CRedirectField();
    private final CRedirectNew redirectNew = new CRedirectNew();

    public CRedirectAnnotationHandler() {
        super(CRedirect.class, CRedirect::method);
    }

    @Override
    public void transform(CRedirect annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        IInjectionTarget iInjectionTarget = injectionTargets.get(annotation.target().value().toUpperCase(Locale.ROOT));

        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
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

        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CRedirect");
        for (AbstractInsnNode injectionInstruction : injectionInstructions) {
            if (injectionInstruction instanceof MethodInsnNode) {
                if (injectionInstruction.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) injectionInstruction).name.equals("<init>")) {
                    this.redirectNew.inject(transformedClass, target, transformer, transformerMethod, injectionInstruction, transformerMethodCalls);
                } else {
                    this.redirectInvoke.inject(transformedClass, target, transformer, transformerMethod, injectionInstruction, transformerMethodCalls);
                }
            } else if (injectionInstruction instanceof FieldInsnNode) {
                this.redirectField.inject(transformedClass, target, transformer, transformerMethod, injectionInstruction, transformerMethodCalls);
            } else {
                throw new InvalidTargetException(transformerMethod, transformer, annotation.target().value(), TARGETS);
            }
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
    }

}
