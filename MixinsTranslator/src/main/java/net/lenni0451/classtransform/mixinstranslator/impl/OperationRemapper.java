package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Remaps MixinExtras operation references to the native ClassTransform operation interface.
 * <p>
 * The class is scanned for {@code com/llamalad7/mixinextras/injector/wrapoperation/Operation} in
 * generic signatures, field and method descriptors, and method instruction owners or descriptors. If
 * none is found, the class is left untouched. Otherwise all references are remapped to
 * {@code net/lenni0451/classtransform/operation/COperation} and the result is merged back into the
 * supplied node.
 * <p>
 * This runs before annotation translation so translated operation wrappers use the native interface.
 */
public class OperationRemapper {

    private static final String MIXIN_EXTRAS_OPERATION = "com/llamalad7/mixinextras/injector/wrapoperation/Operation";
    private static final String CLASSTRANSFORM_OPERATION = "net/lenni0451/classtransform/operation/COperation";

    /**
     * Remap all MixinExtras operation references in the given class.
     * <p>
     * If the class does not reference the operation interface, it is left unchanged.
     *
     * @param node The class to remap
     */
    public static void remap(final ClassNode node) {
        if (!referencesOperation(node)) return;
        MapRemapper remapper = new MapRemapper();
        remapper.addClassMapping(MIXIN_EXTRAS_OPERATION, CLASSTRANSFORM_OPERATION);
        ClassNode remapped = Remapper.remap(node, remapper);
        Remapper.merge(node, remapped);
    }

    private static boolean referencesOperation(final ClassNode node) {
        if (node.signature != null && node.signature.contains(MIXIN_EXTRAS_OPERATION)) return true;
        for (FieldNode field : node.fields) {
            if (field.desc.contains(MIXIN_EXTRAS_OPERATION) || (field.signature != null && field.signature.contains(MIXIN_EXTRAS_OPERATION))) return true;
        }
        for (MethodNode method : node.methods) {
            if (method.desc.contains(MIXIN_EXTRAS_OPERATION)) return true;
            if (method.signature != null && method.signature.contains(MIXIN_EXTRAS_OPERATION)) return true;
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                if (methodInsn.owner.equals(MIXIN_EXTRAS_OPERATION) || methodInsn.desc.contains(MIXIN_EXTRAS_OPERATION)) return true;
            }
        }
        return false;
    }

}
