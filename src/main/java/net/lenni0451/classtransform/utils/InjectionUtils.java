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
     * @throws TransformerException If the handler arguments do not match the required arguments followed by a prefix of the target arguments
     */
    public static int getCapturedArgumentCount(final ClassNode transformer, final MethodNode transformerMethod, final MethodNode target, final Type... requiredArguments) {
        Type[] arguments = argumentTypes(transformerMethod);
        Type[] targetArguments = argumentTypes(target);
        int captured = arguments.length - requiredArguments.length;
        boolean valid = captured >= 0 && captured <= targetArguments.length;
        for (int i = 0; valid && i < arguments.length; i++) {
            Type expected = i < requiredArguments.length ? requiredArguments[i] : targetArguments[i - requiredArguments.length];
            if (i < requiredArguments.length) {
                valid = CoerceUtils.canCoerce(expected, arguments[i], CoerceUtils.hasCoerce(transformerMethod, i));
            } else {
                valid = CoerceUtils.canCoerce(expected, arguments[i], true);
            }
        }
        if (!valid) {
            throw new TransformerException(transformerMethod, transformer, "must have the required arguments followed by an optional prefix of the target method arguments")
                    .help(Codifier.of(transformerMethod).param(null).params(requiredArguments));
        }
        return captured;
    }

    /**
     * Compare a prefix of target values with handler arguments.
     *
     * @param transformerMethod The handler method
     * @param expected          The target value types
     * @param handler           The handler argument types
     * @param handlerOffset     The offset of the compared arguments in the handler descriptor
     * @return If all compared arguments are compatible
     */
    public static boolean compareTypes(final MethodNode transformerMethod, final Type[] expected, final Type[] handler, final int handlerOffset) {
        if (expected.length != handler.length - handlerOffset) return false;
        for (int i = 0; i < expected.length; i++) {
            int handlerIndex = handlerOffset + i;
            if (!CoerceUtils.canCoerce(expected[i], handler[handlerIndex], CoerceUtils.hasCoerce(transformerMethod, handlerIndex))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compare target values with handler arguments.
     *
     * @param transformerMethod The handler method
     * @param expected          The target value types
     * @param handler           The handler argument types
     * @return If all arguments are compatible
     */
    public static boolean compareTypes(final MethodNode transformerMethod, final Type[] expected, final Type[] handler) {
        return compareTypes(transformerMethod, expected, handler, 0);
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
