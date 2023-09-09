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

@ParametersAreNonnullByDefault
public class MapClassProvider implements IClassProvider {

    @Nullable
    private final IClassProvider parent;
    private final Map<String, byte[]> classes;
    private final NameFormat nameFormat;

    public MapClassProvider(final Map<String, byte[]> classes, final NameFormat nameFormat) {
        this(null, classes, nameFormat);
    }

    public MapClassProvider(final Map<String, byte[]> classes, final NameFormat nameFormat, @Nullable final IClassProvider parent) {
        this(parent, classes, nameFormat);
    }

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


    public enum NameFormat {
        DOT(ASMUtils::dot, Function.identity()),
        SLASH(ASMUtils::slash, ASMUtils::dot),
        DOT_CLASS(name -> ASMUtils.dot(name) + ".class", name -> ASMUtils.dot(name.substring(0, name.length() - 6))),
        SLASH_CLASS(name -> ASMUtils.slash(name) + ".class", name -> ASMUtils.dot(name.substring(0, name.length() - 6)));

        private final Function<String, String> formatter;
        private final Function<String, String> toDot;

        NameFormat(final Function<String, String> formatter, final Function<String, String> toDot) {
            this.formatter = formatter;
            this.toDot = toDot;
        }

        public String format(final String name) {
            return this.formatter.apply(name);
        }

        public String toDot(final String name) {
            return this.toDot.apply(name);
        }
    }

}
