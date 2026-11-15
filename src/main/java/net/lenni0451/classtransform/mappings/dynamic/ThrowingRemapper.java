package net.lenni0451.classtransform.mappings.dynamic;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * The default dynamic remapper which always throws.
 * <p>
 * It is used when {@link RemapType#DYNAMIC} is selected without configuring a custom remapper.
 */
public class ThrowingRemapper implements IDynamicRemapper {

    @Nullable
    @Override
    public RemapType dynamicRemap(AMapper mapper, Class<?> annotation, Map<String, Object> values, Method remappedMethod, TransformerManager transformerManager, ClassNode target, ClassNode transformer) {
        throw new UnsupportedOperationException("Annotation '" + annotation.getName() + "' with dynamic remapping has no remapper set!");
    }

}
