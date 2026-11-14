package net.lenni0451.classtransform.mappings.dynamic;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.annotation.RemapType;
import net.lenni0451.classtransform.utils.ASMUtils;
import net.lenni0451.classtransform.utils.MemberDeclaration;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Remap the method and field declarations of {@code @CDefinition}.
 */
public class DefinitionRemapper implements IDynamicRemapper {

    @Nullable
    @Override
    public RemapType dynamicRemap(AMapper mapper, Class<?> annotation, Map<String, Object> values, Method remappedMethod, TransformerManager transformerManager, ClassNode target, ClassNode transformer) {
        String member = remappedMethod.getName();
        if (!member.equals("method") && !member.equals("field")) return null;
        Object raw = values.get(member);
        if (raw == null) return null;
        boolean field = member.equals("field");
        List<String> remapped = new ArrayList<>();
        for (String declaration : toStrings(raw)) {
            remapped.add(this.remapDeclaration(mapper.getRemapper(), target, declaration, field));
        }
        values.put(member, remapped);
        return null;
    }

    private String remapDeclaration(final MapRemapper remapper, final ClassNode target, final String declaration, final boolean field) {
        MemberDeclaration member = ASMUtils.splitMemberDeclaration(declaration);
        String owner;
        String name;
        String desc;
        if (member != null) {
            owner = member.getOwner();
            name = member.getName();
            desc = member.getDesc();
        } else {
            int index = field ? declaration.indexOf(':') : declaration.indexOf('(');
            if (index < 0) return declaration;
            owner = target.name;
            name = declaration.substring(0, index);
            desc = declaration.substring(field ? index + 1 : index);
        }
        if (owner.contains("*") || owner.contains("?")) return declaration;
        String mappedOwner = remapper.mapType(owner);
        if (field) {
            String mappedName = remapper.mapFieldName(owner, name, desc);
            String mappedDesc = remapper.mapDesc(desc);
            return mappedOwner + "." + mappedName + ":" + mappedDesc;
        }
        String mappedName = remapper.mapMethodName(owner, name, desc);
        String mappedDesc = remapper.mapMethodDesc(desc);
        return mappedOwner + "." + mappedName + mappedDesc;
    }

    private static List<String> toStrings(final Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof String) result.add((String) value);
        else if (value instanceof List) for (Object entry : (List<?>) value) if (entry instanceof String) result.add((String) entry);
        return result;
    }

}
