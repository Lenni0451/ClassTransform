package net.lenni0451.classtransform.mixinstranslator.impl;

import net.lenni0451.classtransform.utils.mappings.MapRemapper;
import net.lenni0451.classtransform.utils.mappings.Remapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Remap MixinExtras local variable references to the native ClassTransform reference types.
 */
public class LocalRefRemapper {

    private static final String MIXIN_EXTRAS_REF = "com/llamalad7/mixinextras/sugar/ref/";
    private static final String CLASSTRANSFORM_REF = "net/lenni0451/classtransform/utils/local/";
    private static final Map<String, String> NATIVE_REFS = new HashMap<>();

    static {
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalRef", CLASSTRANSFORM_REF + "CLocalRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalBooleanRef", CLASSTRANSFORM_REF + "CLocalBooleanRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalByteRef", CLASSTRANSFORM_REF + "CLocalByteRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalCharRef", CLASSTRANSFORM_REF + "CLocalCharRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalShortRef", CLASSTRANSFORM_REF + "CLocalShortRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalIntRef", CLASSTRANSFORM_REF + "CLocalIntRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalLongRef", CLASSTRANSFORM_REF + "CLocalLongRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalFloatRef", CLASSTRANSFORM_REF + "CLocalFloatRef");
        NATIVE_REFS.put(MIXIN_EXTRAS_REF + "LocalDoubleRef", CLASSTRANSFORM_REF + "CLocalDoubleRef");
    }

    /**
     * Remap all MixinExtras local variable references in the given class.<br>
     * Calls to the reference interfaces are changed to calls to the native reference classes.
     *
     * @param node The class to remap
     */
    public static void remap(final ClassNode node) {
        if (!referencesRef(node)) return;
        MapRemapper remapper = new MapRemapper();
        for (Map.Entry<String, String> entry : NATIVE_REFS.entrySet()) remapper.addClassMapping(entry.getKey(), entry.getValue());
        ClassNode remapped = Remapper.remap(node, remapper);
        for (MethodNode method : remapped.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                if (!NATIVE_REFS.containsValue(methodInsnNode.owner) || methodInsnNode.getOpcode() != Opcodes.INVOKEINTERFACE) continue;
                methodInsnNode.setOpcode(Opcodes.INVOKEVIRTUAL);
                methodInsnNode.itf = false;
            }
        }
        Remapper.merge(node, remapped);
    }

    private static boolean referencesRef(final ClassNode node) {
        if (node.signature != null && referencesRef(node.signature)) return true;
        for (FieldNode field : node.fields) {
            if (referencesRef(field.desc) || (field.signature != null && referencesRef(field.signature))) return true;
        }
        for (MethodNode method : node.methods) {
            if (referencesRef(method.desc) || (method.signature != null && referencesRef(method.signature))) return true;
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                    if (referencesRef(methodInsnNode.owner) || referencesRef(methodInsnNode.desc)) return true;
                } else if (instruction instanceof FieldInsnNode) {
                    if (referencesRef(((FieldInsnNode) instruction).desc)) return true;
                } else if (instruction instanceof TypeInsnNode) {
                    if (referencesRef(((TypeInsnNode) instruction).desc)) return true;
                } else if (instruction instanceof LdcInsnNode && ((LdcInsnNode) instruction).cst instanceof Type) {
                    if (referencesRef(((Type) ((LdcInsnNode) instruction).cst).getDescriptor())) return true;
                }
            }
        }
        return false;
    }

    private static boolean referencesRef(final String value) {
        for (String ref : NATIVE_REFS.keySet()) {
            if (value.contains(ref)) return true;
        }
        return false;
    }

}
