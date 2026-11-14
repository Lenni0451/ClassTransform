package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.exceptions.TransformerException;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;

import static net.lenni0451.classtransform.utils.Types.argumentTypes;

/**
 * Utils for passing enclosing method arguments to injection methods.
 */
@ParametersAreNonnullByDefault
public class InjectionUtils {

    /**
     * Validate the required arguments followed by an optional prefix of the enclosing method arguments.<br>
     * Special parameters must be removed by the coprocessors before calling this method.
     *
     * @param transformer       The transformer class node
     * @param transformerMethod The transformer method node
     * @param target            The enclosing method
     * @param requiredArguments The arguments required by the injection
     * @return The number of captured enclosing method arguments
     */
    public static int getCapturedArgumentCount(final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target, final Type... requiredArguments) {
        Type[] arguments = argumentTypes(transformerMethod);
        Type[] targetArguments = argumentTypes(target);
        int captured = arguments.length - requiredArguments.length;
        boolean valid = captured >= 0 && captured <= targetArguments.length;
        for (int i = 0; valid && i < arguments.length; i++) {
            Type expected = i < requiredArguments.length ? requiredArguments[i] : targetArguments[i - requiredArguments.length];
            valid = ASMUtils.compareType(expected, arguments[i]);
        }
        if (!valid) {
            throw new TransformerException(transformerMethod, transformer, "must have the required arguments followed by an optional prefix of the target method arguments")
                    .help(Codifier.of(transformerMethod).param(null).params(requiredArguments));
        }
        return captured;
    }

    /**
     * Load a prefix of the enclosing method arguments.
     *
     * @param target The enclosing method
     * @param count  The number of arguments to load
     * @return The instructions loading the arguments
     */
    public static InsnList loadCapturedArguments(final MethodNode target, final int count) {
        InsnList instructions = new InsnList();
        Type[] arguments = argumentTypes(target);
        int index = Modifier.isStatic(target.access) ? 0 : 1;
        for (int i = 0; i < count; i++) {
            Type argument = arguments[i];
            instructions.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), index));
            index += argument.getSize();
        }
        return instructions;
    }

}
