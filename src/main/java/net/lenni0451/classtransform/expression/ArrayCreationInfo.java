package net.lenni0451.classtransform.expression;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Information about the initialization stores of an array creation.
 */
@ParametersAreNonnullByDefault
public class ArrayCreationInfo {

    private final List<ExpressionFlow.FlowValue> stores;
    private final ExpressionFlow.FlowValue initialized;

    public ArrayCreationInfo(final List<ExpressionFlow.FlowValue> stores, final ExpressionFlow.FlowValue initialized) {
        this.stores = stores;
        this.initialized = initialized;
    }

    /**
     * @return The stores which initialize the array
     */
    public List<ExpressionFlow.FlowValue> getStores() {
        return this.stores;
    }

    /**
     * @return The last store which initializes the array
     */
    public ExpressionFlow.FlowValue getInitialized() {
        return this.initialized;
    }

}
