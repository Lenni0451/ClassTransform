package net.lenni0451.classtransform.utils.tree;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A provider for class bytecode.
 */
@ParametersAreNonnullByDefault
public interface IClassProvider {

    /**
     * Get the bytecode of a class.<br>
     * Class name is with '.' instead of '/'.
     *
     * @param name The name of the class
     * @return The bytecode of the class
     */
    @Nonnull
    byte[] getClass(final String name) throws ClassNotFoundException;

    /**
     * Get a map of all classes with a supplier for their bytecode.<br>
     * Class names need to be with '.' instead of '/'.<br>
     * <b>The map must be mutable!</b>
     *
     * @return A map of all class names to their bytecode supplier
     */
    @Nonnull
    Map<String, Supplier<byte[]>> getAllClasses();

}
