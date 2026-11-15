package net.lenni0451.classtransform.mappings.dynamic;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A hook to remap annotation values which cannot be handled by a static {@link RemapType}.
 * <p>
 * Implementations are instantiated through their no-arg constructor for every annotation method
 * marked with {@link RemapType#DYNAMIC} while a transformer is remapped. The hook may update the
 * annotation value in {@code values} directly. Returning {@code null} or {@link RemapType#DYNAMIC}
 * skips the default remapping; any other type makes the default remapper process the value with
 * that type.
 */
public interface IDynamicRemapper {

    /**
     * Remap the annotation fields dynamically.<br>
     * A new type can be returned to let the default remapper handle the remapping.<br>
     * If the return value is {@code null} or {@link RemapType#DYNAMIC} the default remapper will be skipped.
     * <p>
     * The value of the remapped method in {@code values} is never null or empty when this method is called.
     *
     * @param mapper             The mapper instance
     * @param annotation         The annotation class
     * @param values             The raw values of the annotation, may be modified in place
     * @param remappedMethod     The annotation method that is being remapped
     * @param transformerManager The transformer manager
     * @param target             The transformer target class
     * @param transformer        The transformer class
     * @return The new remap type
     */
    @Nullable
    RemapType dynamicRemap(final AMapper mapper, final Class<?> annotation, final Map<String, Object> values, final Method remappedMethod, final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer);

}
