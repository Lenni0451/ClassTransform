package net.lenni0451.classtransform.transformer.impl.general.membercopy;

import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.List;

public class InterfaceMerger {

    public static void mergeInterfaces(final ClassNode transformedClass, final ClassNode transformer) {
        if (transformer.interfaces != null) {
            List<String> interfaces = transformedClass.interfaces;
            if (interfaces == null) interfaces = transformedClass.interfaces = new ArrayList<>();
            for (String anInterface : transformer.interfaces) {
                if (!interfaces.contains(anInterface)) interfaces.add(anInterface);
            }
        }
    }

}
