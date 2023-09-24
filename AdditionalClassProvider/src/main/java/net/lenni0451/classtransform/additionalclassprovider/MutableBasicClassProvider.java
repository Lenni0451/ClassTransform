package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.tree.BasicClassProvider;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A basic class provider with only the {@link #getClass(String)} implemented.<br>
 * You need to register all transformer classes with direct paths as the {@link #getAllClasses()} method is required for wildcard matching.
 */
@ParametersAreNonnullByDefault
public class MutableBasicClassProvider extends BasicClassProvider {

    public MutableBasicClassProvider() {
        this(MutableBasicClassProvider.class.getClassLoader());
    }

    public MutableBasicClassProvider(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
