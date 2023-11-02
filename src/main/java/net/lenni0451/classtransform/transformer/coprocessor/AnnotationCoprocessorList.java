package net.lenni0451.classtransform.transformer.coprocessor;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.transformer.IAnnotationCoprocessor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A list of {@link IAnnotationCoprocessor} used to track the state of the transformation as every coprocessor can only be used once.
 */
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

    /**
     * Add a new coprocessor to the list.
     *
     * @param coprocessorSupplier The supplier of the coprocessor
     */
    public void add(final Supplier<? extends IAnnotationCoprocessor> coprocessorSupplier) {
        this.expect(State.OPEN);
        this.coprocessorSupplier.add(coprocessorSupplier);
    }

    /**
     * @return A built list of coprocessors
     */
    public AnnotationCoprocessorList build() {
        this.expect(State.OPEN);
        AnnotationCoprocessorList list = new AnnotationCoprocessorList(null, new ArrayList<>(), State.BUILT);
        for (Supplier<? extends IAnnotationCoprocessor> supplier : this.coprocessorSupplier) list.coprocessors.add(supplier.get());
        return list;
    }

    /**
     * Preprocess the transformer method before the annotation handler injects calls to the target method.<br>
     * This happens before the transformer method is verified by the annotation handler.
     *
     * @param transformerManager The transformer manager
     * @param transformedClass   The target class node
     * @param transformedMethod  The target method node
     * @param transformer        The transformer class node
     * @param transformerMethod  The transformer method node
     * @return The preprocessed method node
     */
    public MethodNode preprocess(final TransformerManager transformerManager, final ClassNode transformedClass, final MethodNode transformedMethod, final ClassNode transformer, MethodNode transformerMethod) {
        this.expect(State.BUILT, State.USED);
        for (IAnnotationCoprocessor coprocessor : this.coprocessors) {
            transformerMethod = coprocessor.preprocess(transformerManager, transformedClass, transformedMethod, transformer, transformerMethod);
        }
        for (int i = this.coprocessors.size() - 1; i >= 0; i--) {
            IAnnotationCoprocessor coprocessor = this.coprocessors.get(i);
            transformerMethod = coprocessor.transform(transformerManager, transformedClass, transformedMethod, transformer, transformerMethod);
        }
        return transformerMethod;
    }

    /**
     * Postprocess the transformer and target method after the annotation handler injected calls to the target method.<br>
     * The {@code transformerMethodCalls} list only contains direct calls to the transformer method.
     *
     * @param transformerManager     The transformer manager
     * @param transformedClass       The target class node
     * @param transformedMethod      The target method node
     * @param transformerMethodCalls The list of calls to the transformer method
     * @param transformer            The transformer class node
     * @param transformerMethod      The transformer method node
     */
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
