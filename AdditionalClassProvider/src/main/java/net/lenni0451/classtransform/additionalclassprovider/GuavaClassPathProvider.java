package net.lenni0451.classtransform.additionalclassprovider;

import com.google.common.reflect.ClassPath;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.Sneaky.sneakySupply;

/**
 * A class provider that loads classes with a {@link ClassLoader} and enumerates them with Guava's {@link ClassPath}.
 * <p>
 * Class bytes are read through {@link ClassLoader#getResourceAsStream(String)}, while the map returned by
 * {@link #getAllClasses()} only contains lazy suppliers that read a class when they are invoked.
 */
@ParametersAreNonnullByDefault
public class GuavaClassPathProvider extends BasicClassProvider {

    private final ClassPath classPath;

    /**
     * Creates a provider that scans the class path of the class loader of this class.
     */
    public GuavaClassPathProvider() {
        this(GuavaClassPathProvider.class.getClassLoader());
    }

    /**
     * Creates a provider that scans the class path of the given class loader.
     *
     * @param classLoader The class loader used both to scan the class path and to load class bytes
     * @throws RuntimeException If the class path could not be scanned
     */
    public GuavaClassPathProvider(final ClassLoader classLoader) {
        super(classLoader);

        try {
            this.classPath = ClassPath.from(classLoader);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize ClassPath", t);
        }
    }

    @Override
    @Nonnull
    public Map<String, Supplier<byte[]>> getAllClasses() {
        Map<String, Supplier<byte[]>> map = new HashMap<>();
        for (ClassPath.ClassInfo classInfo : this.classPath.getAllClasses()) {
            map.put(classInfo.getName(), sneakySupply(() -> this.getClass(classInfo.getName())));
        }
        return map;
    }

}
