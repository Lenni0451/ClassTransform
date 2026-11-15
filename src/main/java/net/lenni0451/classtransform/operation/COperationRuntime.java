package net.lenni0451.classtransform.operation;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Runtime helpers used by generated operation wrappers.
 * <p>
 * These methods are called from bytecode inserted by the operation wrapper handlers and are
 * not intended to be used directly.
 */
@ParametersAreNonnullByDefault
public class COperationRuntime {

    /**
     * Validate the number of arguments passed to {@link COperation#call(Object...)}.
     *
     * @param args     The passed arguments
     * @param expected The expected argument count
     * @throws IllegalArgumentException If the argument count differs from the expected count
     */
    public static void checkArgumentCount(final Object[] args, final int expected) {
        if (args.length != expected) {
            throw new IllegalArgumentException("Incorrect number of arguments passed to COperation.call: expected " + expected + " but got " + args.length);
        }
    }

}
