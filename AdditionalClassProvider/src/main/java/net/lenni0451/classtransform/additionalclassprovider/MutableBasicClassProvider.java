package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.BasicClassProvider;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A basic class provider with only the {@link #getClass(String)} implemented.<br>
 * You need to register all transformer classes with direct paths as the {@link #getAllClasses()} method is required for wildcard matching.
 */
@ParametersAreNonnullByDefault
public class MutableBasicClassProvider extends BasicClassProvider {

    /**
     * Creates a provider using the class loader of this class.
     */
    public MutableBasicClassProvider() {
        this(MutableBasicClassProvider.class.getClassLoader());
    }

    /**
     * Creates a provider using the given class loader.
     *
     * @param classLoader The class loader used to load class bytes
     */
    public MutableBasicClassProvider(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Replaces the class loader used to load class bytes.
     *
     * @param classLoader The new class loader
     */
    public void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
