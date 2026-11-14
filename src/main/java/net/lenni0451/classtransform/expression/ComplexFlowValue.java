package net.lenni0451.classtransform.expression;

import org.objectweb.asm.Type;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A value which results from merging multiple control flow branches.
 */
@ParametersAreNonnullByDefault
public class ComplexFlowValue extends ExpressionFlow.FlowValue {

    private final Set<ExpressionFlow.FlowValue> sources;

    public ComplexFlowValue(final Type type, final Collection<ExpressionFlow.FlowValue> sources) {
        super(type, null, new ExpressionFlow.FlowValue[0]);
        this.sources = new LinkedHashSet<>(sources);
    }

    /**
     * @return The values which were merged into this value
     */
    public Set<ExpressionFlow.FlowValue> getSources() {
        return Collections.unmodifiableSet(this.sources);
    }

}
