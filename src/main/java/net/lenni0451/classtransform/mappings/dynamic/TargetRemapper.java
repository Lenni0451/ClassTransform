package net.lenni0451.classtransform.mappings.dynamic;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.targets.IInjectionTarget;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public class TargetRemapper implements IDynamicRemapper {

    @Nullable
    @Override
    public RemapType dynamicRemap(AMapper mapper, Class<?> annotation, Map<String, Object> values, Method remappedMethod, TransformerManager transformerManager, ClassNode target, ClassNode transformer) {
        String injectionTargetName = (String) values.get("value");
        Optional<IInjectionTarget> injectionTarget = transformerManager.getInjectionTarget(injectionTargetName);
        if (!injectionTarget.isPresent()) return RemapType.MEMBER; //The target could not be found, so just return MEMBER and hope for the best
        return injectionTarget.get().dynamicRemap(mapper, annotation, values, remappedMethod, transformerManager, target, transformer);
    }

}
