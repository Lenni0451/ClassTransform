package net.lenni0451.classtransform.transformer.coprocessor;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.IAnnotationCoprocessor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AnnotationCoprocessorList {

    private final List<Supplier<? extends IAnnotationCoprocessor>> coprocessorSupplier;
    private final List<IAnnotationCoprocessor> coprocessors;
    private State state;

    public AnnotationCoprocessorList() {
        this(new ArrayList<>(), null, State.OPEN);
    }

    private AnnotationCoprocessorList(final List<Supplier<? extends IAnnotationCoprocessor>> coprocessorSupplier, final List<IAnnotationCoprocessor> coprocessors, final State state) {
        this.coprocessorSupplier = coprocessorSupplier;
        this.coprocessors = coprocessors;
        this.state = state;
    }

    private void expect(final State expected) {
        this.expect(expected, expected);
    }

    private void expect(final State expected, final State newState) {
        if (!this.state.equals(expected)) throw new IllegalStateException("Expected state " + expected.name() + " but got " + this.state.name());
        this.state = newState;
    }

    public void add(final Supplier<? extends IAnnotationCoprocessor> coprocessorSupplier) {
        this.expect(State.OPEN);
        this.coprocessorSupplier.add(coprocessorSupplier);
    }

    public AnnotationCoprocessorList build() {
        this.expect(State.OPEN);
        AnnotationCoprocessorList list = new AnnotationCoprocessorList(null, new ArrayList<>(), State.BUILT);
        for (Supplier<? extends IAnnotationCoprocessor> supplier : this.coprocessorSupplier) list.coprocessors.add(supplier.get());
        return list;
    }

    public MethodNode preprocess(final TransformerManager transformerManager, final ClassNode transformedClass, final MethodNode transformedMethod, final ClassNode transformer, MethodNode transformerMethod) {
        this.expect(State.BUILT, State.USED);
        for (IAnnotationCoprocessor coprocessor : this.coprocessors) {
            transformerMethod = coprocessor.preprocess(transformerManager, transformedClass, transformedMethod, transformer, transformerMethod);
        }
        return transformerMethod;
    }

    public void postprocess(final TransformerManager transformerManager, final ClassNode transformedClass, final MethodNode transformedMethod, final List<MethodInsnNode> transformerMethodCalls, final ClassNode transformer, final MethodNode transformerMethod) {
        this.expect(State.USED, State.CLOSED);
        if (this.coprocessors == null) throw new IllegalStateException("This list is not built");
        for (IAnnotationCoprocessor coprocessor : this.coprocessors) {
            coprocessor.postprocess(transformerManager, transformedClass, transformedMethod, transformerMethodCalls, transformer, transformerMethod);
        }
    }


    private enum State {
        OPEN, BUILT, USED, CLOSED
    }

}
