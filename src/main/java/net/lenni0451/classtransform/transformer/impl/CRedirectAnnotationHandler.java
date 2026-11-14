package net.lenni0451.classtransform.transformer.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.injection.CRedirect;
import net.lenni0451.classtransform.exceptions.InvalidTargetException;
import net.lenni0451.classtransform.exceptions.TransformerException;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import net.lenni0451.classtransform.transformer.coprocessor.AnnotationCoprocessorList;
import net.lenni0451.classtransform.transformer.impl.redirect.CRedirectField;
import net.lenni0451.classtransform.transformer.impl.redirect.CRedirectInvoke;
import net.lenni0451.classtransform.transformer.impl.redirect.CRedirectNew;
import net.lenni0451.classtransform.transformer.types.InjectionAnnotationHandler;
import net.lenni0451.classtransform.transformer.InjectionPlan;
import net.lenni0451.classtransform.utils.DescMatcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * The annotation handler for the {@link CRedirect} annotation.
 */
@ParametersAreNonnullByDefault
public class CRedirectAnnotationHandler extends InjectionAnnotationHandler<CRedirect> {

    private final CRedirectInvoke redirectInvoke = new CRedirectInvoke();
    private final CRedirectField redirectField = new CRedirectField();
    private final CRedirectNew redirectNew = new CRedirectNew();

    public CRedirectAnnotationHandler() {
        super(CRedirect.class, CRedirect::method);
    }

    @Override
    protected Set<MethodNode> getTargetMethods(final CRedirect annotation, final ClassNode target, final ClassNode transformer, final MethodNode method) {
        Set<MethodNode> targets = super.getTargetMethods(annotation, target, transformer, method);
        CDesc[] descs = annotation.desc();
        if (descs.length > 0) DescMatcher.addMethods(targets, target, descs);
        return targets;
    }

    @Override
    public void transform(CRedirect annotation, TransformerManager transformerManager, ClassNode transformedClass, ClassNode transformer, MethodNode transformerMethod, MethodNode target) {
        this.checkConstraints(transformerManager, annotation.constraints(), transformerMethod, transformer);
        AnnotationCoprocessorList coprocessors = transformerManager.getCoprocessors();
        transformerMethod = coprocessors.preprocess(transformerManager, transformedClass, target, transformer, transformerMethod);
        Map<String, IInjectionTarget> injectionTargets = transformerManager.getInjectionTargets();
        IInjectionTarget injectionTarget = injectionTargets.get(annotation.target().value().toUpperCase(Locale.ROOT));
        if (injectionTarget == null) throw new InvalidTargetException(transformerMethod, transformer, annotation.target().target(), injectionTargets.keySet());

        if (Modifier.isStatic(target.access) != Modifier.isStatic(transformerMethod.access)) {
            throw TransformerException.wrongStaticAccess(transformerMethod, transformer, Modifier.isStatic(target.access));
        }

        List<AbstractInsnNode> injectionInstructions = InjectionPlan.select(transformerManager, target, annotation.target(), annotation.slice());
        if (injectionInstructions == null) {
            throw TransformerException.invalidMemberDeclaration(transformerMethod, transformer, annotation.target().target());
        }

        List<MethodInsnNode> transformerMethodCalls = new ArrayList<>();
        MethodNode copiedTransformerMethod = this.renameAndCopy(transformerMethod, target, transformer, transformedClass, "CRedirect");
        for (AbstractInsnNode injectionInstruction : injectionInstructions) {
            AbstractInsnNode next = injectionInstruction.getNext();
            if (injectionInstruction instanceof MethodInsnNode) {
                if (injectionInstruction.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) injectionInstruction).name.equals("<init>")) {
                    this.redirectNew.inject(transformedClass, target, transformer, transformerMethod, injectionInstruction, transformerMethodCalls);
                } else {
                    this.redirectInvoke.inject(transformedClass, target, transformer, transformerMethod, injectionInstruction, transformerMethodCalls);
                }
            } else if (injectionInstruction instanceof FieldInsnNode) {
                this.redirectField.inject(transformedClass, target, transformer, transformerMethod, injectionInstruction, transformerMethodCalls);
            } else {
                throw new InvalidTargetException(transformerMethod, transformer, annotation.target().value(), Arrays.asList("INVOKE", "FIELD", "NEW"));
            }
            InjectionPlan.replace(injectionInstruction, transformerMethodCalls.get(transformerMethodCalls.size() - 1));
            InjectionPlan.setAfter(transformerMethodCalls.get(transformerMethodCalls.size() - 1), next == null ? target.instructions.getLast() : next.getPrevious());
        }
        coprocessors.postprocess(transformerManager, transformedClass, target, transformerMethodCalls, transformer, copiedTransformerMethod);
        InjectionPlan.record(transformerMethodCalls.size());
    }

}
