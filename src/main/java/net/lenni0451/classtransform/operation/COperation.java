package net.lenni0451.classtransform.operation;

/**
 * Represents an operation which can be called by a {@link net.lenni0451.classtransform.annotations.injection.CWrapOperation}
 * handler.<br>
 * Calling the operation executes the next level of the operation chain, ending with the original operation.
 *
 * @param <R> The return type of the operation
 */
@FunctionalInterface
public interface COperation<R> {

    /**
     * Call the next level of the operation chain.
     *
     * @param args The arguments of the operation
     * @return The result of the operation
     */
    R call(Object... args);

}
