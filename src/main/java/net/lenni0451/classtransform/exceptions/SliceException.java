package net.lenni0451.classtransform.exceptions;

/**
 * An exception which is thrown when a slice boundary cannot be resolved to exactly one instruction.
 */
public class SliceException extends IllegalArgumentException {

    /**
     * Create an exception for a slice boundary with an unknown target type.
     *
     * @param position The slice position, usually {@code from} or {@code to}
     * @param target   The target declaration which could not be resolved
     * @return The created exception
     */
    public static SliceException unknown(final String position, final String target) {
        return new SliceException("Unknown " + position + " target in slice: " + target);
    }

    /**
     * Create an exception for a slice boundary which did not match exactly once.
     *
     * @param position The slice position, usually {@code from} or {@code to}
     * @param target   The target declaration
     * @param count    The number of matched instructions; zero reports a missing match and any other value reports an ambiguous match
     * @return The created exception
     */
    public static SliceException count(final String position, final String target, final int count) {
        if (count == 0) return new SliceException(position + " target in slice not found: " + target);
        else return new SliceException(position + " target in slice has more than one match: " + target + " (" + count + " matches)");
    }


    private SliceException(final String message) {
        super(message);
    }

}
