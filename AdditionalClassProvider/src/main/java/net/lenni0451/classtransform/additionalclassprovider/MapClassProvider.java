package net.lenni0451.classtransform.additionalclassprovider;

import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.tree.IClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A class provider backed by an in-memory map of class name to class bytes.
 * <p>
 * The given map is used directly and not copied, so later changes to it are visible to this provider. Lookups apply
 * the configured {@link NameFormat} to the requested name, and classes missing from the map are delegated to an
 * optional parent provider before a {@link ClassNotFoundException} is thrown.
 */
@ParametersAreNonnullByDefault
public class MapClassProvider implements IClassProvider {

    @Nullable
    private final IClassProvider parent;
    private final Map<String, byte[]> classes;
    private final NameFormat nameFormat;

    /**
     * Creates a provider for the given classes.
     *
     * @param classes    The map of class names to class bytes
     * @param nameFormat The format of the class names used as map keys
     */
    public MapClassProvider(final Map<String, byte[]> classes, final NameFormat nameFormat) {
        this(null, classes, nameFormat);
    }

    /**
     * Creates a provider for the given classes and falls back to a parent provider.
     *
     * @param classes    The map of class names to class bytes
     * @param nameFormat The format of the class names used as map keys
     * @param parent     The parent provider queried when a class is not in the map
     */
    public MapClassProvider(final Map<String, byte[]> classes, final NameFormat nameFormat, @Nullable final IClassProvider parent) {
        this(parent, classes, nameFormat);
    }

    /**
     * Creates a provider for the given classes and falls back to a parent provider.
     *
     * @param parent     The parent provider queried when a class is not in the map
     * @param classes    The map of class names to class bytes
     * @param nameFormat The format of the class names used as map keys
     */
    public MapClassProvider(@Nullable final IClassProvider parent, final Map<String, byte[]> classes, final NameFormat nameFormat) {
        this.parent = parent;
        this.classes = classes;
        this.nameFormat = nameFormat;
    }

    @Nonnull
    @Override
    public byte[] getClass(String name) throws ClassNotFoundException {
        String formattedName = this.nameFormat.format(name);
        byte[] clazz = this.classes.get(formattedName);
        if (clazz != null) return clazz;
        if (this.parent != null) return this.parent.getClass(name);
        throw new ClassNotFoundException(name);
    }

    @Nonnull
    @Override
    public Map<String, Supplier<byte[]>> getAllClasses() {
        Map<String, Supplier<byte[]>> classes = this.parent != null ? this.parent.getAllClasses() : new HashMap<>();
        for (String name : this.classes.keySet()) classes.put(this.nameFormat.toDot(name), () -> this.classes.get(name));
        return classes;
    }


    /**
     * The format of the class names used as keys in the backing map.
     */
    public enum NameFormat {
        /**
         * Dot-separated class names without a suffix, such as {@code java.lang.String}.
         */
        DOT(ASMUtils::dot, Function.identity()),
        /**
         * Slash-separated class names without a suffix, such as {@code java/lang/String}.
         */
        SLASH(ASMUtils::slash, ASMUtils::dot),
        /**
         * Dot-separated class names with a {@code .class} suffix, such as {@code java.lang.String.class}.
         */
        DOT_CLASS(name -> ASMUtils.dot(name) + ".class", name -> ASMUtils.dot(name.substring(0, name.length() - 6))),
        /**
         * Slash-separated class names with a {@code .class} suffix, such as {@code java/lang/String.class}.
         */
        SLASH_CLASS(name -> ASMUtils.slash(name) + ".class", name -> ASMUtils.dot(name.substring(0, name.length() - 6)));

        private final Function<String, String> formatter;
        private final Function<String, String> toDot;

        NameFormat(final Function<String, String> formatter, final Function<String, String> toDot) {
            this.formatter = formatter;
            this.toDot = toDot;
        }

        /**
         * Converts a dot-separated class name to the key format of this constant.
         *
         * @param name The dot-separated class name
         * @return The class name in this format
         */
        public String format(final String name) {
            return this.formatter.apply(name);
        }

        /**
         * Converts a class name in the key format of this constant to a dot-separated class name.
         *
         * @param name The class name in this format
         * @return The dot-separated class name
         */
        public String toDot(final String name) {
            return this.toDot.apply(name);
        }
    }

}
