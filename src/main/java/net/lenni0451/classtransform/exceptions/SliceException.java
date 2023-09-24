package net.lenni0451.classtransform.exceptions;

public class SliceException extends IllegalArgumentException {

    public static SliceException unknown(final String position, final String target) {
        return new SliceException("Unknown " + position + " target in slice: " + target);
    }

    public static SliceException count(final String position, final String target, final int count) {
        if (count == 0) return new SliceException(position + " target in slice not found: " + target);
        else return new SliceException(position + " target in slice has more than one match: " + target + " (" + count + " matches)");
    }


    private SliceException(final String message) {
        super(message);
    }

}
