package net.lenni0451.classtransform.transformer.impl.general.membercopy;

import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges the interfaces of a transformer class into the transformed class.
 * <p>
 * The transformed class implements the same interfaces as the transformer after merging.
 */
public class InterfaceMerger {

    /**
     * Adds all interfaces of the transformer to the transformed class.
     * <p>
     * Interfaces which the transformed class already implements are not added twice.
     *
     * @param transformedClass The class receiving the interfaces
     * @param transformer      The transformer class providing the interfaces
     */
    public static void mergeInterfaces(final ClassNode transformedClass, final ClassNode transformer) {
        if (transformer.interfaces != null) {
            List<String> interfaces = transformedClass.interfaces;
            if (interfaces == null) {
                interfaces = new ArrayList<>();
                transformedClass.interfaces = interfaces;
            }
            for (String anInterface : transformer.interfaces) {
                if (!interfaces.contains(anInterface)) interfaces.add(anInterface);
            }
        }
    }

}
