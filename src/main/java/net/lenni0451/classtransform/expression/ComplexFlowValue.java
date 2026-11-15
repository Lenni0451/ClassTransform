package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A value which results from merging multiple control flow branches.
 * It has no instruction and is therefore always {@linkplain ExpressionFlow.FlowValue#isComplex() complex}; its
 * sources are the original values that were merged.
 */
@ParametersAreNonnullByDefault
public class ComplexFlowValue extends ExpressionFlow.FlowValue {

    private final Set<ExpressionFlow.FlowValue> sources;

    /**
     * Creates a merged value.
     *
     * @param type    the common type of the merged values
     * @param sources the values that were merged; duplicates are removed
     */
    public ComplexFlowValue(final Type type, final Collection<ExpressionFlow.FlowValue> sources) {
        super(type, null, new ExpressionFlow.FlowValue[0]);
        this.sources = new LinkedHashSet<>(sources);
    }

    /**
     * Returns the values which were merged into this value.
     * <p>
     * The returned set is an unmodifiable view of the internal set, in insertion order.
     *
     * @return The values which were merged into this value
     */
    public Set<ExpressionFlow.FlowValue> getSources() {
        return Collections.unmodifiableSet(this.sources);
    }

}
