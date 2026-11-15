package net.lenni0451.classtransform.utils.loader;

/**
 * The priority of the {@link InjectionClassLoader}.
 * <p>
 * {@link #CUSTOM_FIRST} is the default. Classes in a protected package are never defined from the
 * injection class loader class path and are resolved by the parent class loader instead, except for
 * prefixes with a protection exception such as {@code com.sun.jna.}. Without a parent these classes
 * fail to load rather than being resolved.
 */
public enum EnumLoaderPriority {

    /**
     * Try to load the classes from the injection class loader class path first.
     */
    CUSTOM_FIRST,
    /**
     * Try to load the classes from the parent class loader first.
     */
    PARENT_FIRST

}
