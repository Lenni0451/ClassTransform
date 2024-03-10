package net.lenni0451.classtransform.mappings.dynamic;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;

public interface IDynamicRemapper {

    /**
     * Remap the annotation fields dynamically.<br>
     * A new type can be returned to let the default remapper handle the remapping.<br>
     * If the return value is {@code null} or {@link RemapType#DYNAMIC} the default remapper will be skipped.
     *
     * @param mapper             The mapper instance
     * @param annotation         The annotation class
     * @param values             The raw values of the annotation
     * @param remappedMethod     The annotation method that is being remapped
     * @param transformerManager The transformer manager
     * @param target             The transformer target class
     * @param transformer        The transformer class
     * @return The new remap type
     */
    @Nullable
    RemapType dynamicRemap(final AMapper mapper, final Class<?> annotation, final Map<String, Object> values, final Method remappedMethod, final TransformerManager transformerManager, final ClassNode target, final ClassNode transformer);

}
