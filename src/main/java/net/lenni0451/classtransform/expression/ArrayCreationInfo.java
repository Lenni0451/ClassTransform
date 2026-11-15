package net.lenni0451.classtransform.expression;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * Information about the initialization stores of an array creation.
 * Produced by {@link NewArrayPostProcessor} when an array is created with a constant size: the first
 * {@code size} consumers of the array must be stores at indices {@code 0} to {@code size - 1}, while unrelated
 * instructions may be interleaved.
 */
@ParametersAreNonnullByDefault
public class ArrayCreationInfo {

    private final List<ExpressionFlow.FlowValue> stores;
    private final ExpressionFlow.FlowValue initialized;

    /**
     * Creates new array creation information.
     *
     * @param stores      the stores initializing the array, in element index order
     * @param initialized the last store, which represents the fully initialized array
     */
    public ArrayCreationInfo(final List<ExpressionFlow.FlowValue> stores, final ExpressionFlow.FlowValue initialized) {
        this.stores = stores;
        this.initialized = initialized;
    }

    /**
     * Returns the stores which initialize the array.
     * <p>
     * The stores are ordered by array index; the returned list is the list passed to the constructor, not a copy.
     *
     * @return The stores which initialize the array
     */
    public List<ExpressionFlow.FlowValue> getStores() {
        return this.stores;
    }

    /**
     * Returns the store after which the array is fully initialized.
     *
     * @return The last store which initializes the array
     */
    public ExpressionFlow.FlowValue getInitialized() {
        return this.initialized;
    }

}
