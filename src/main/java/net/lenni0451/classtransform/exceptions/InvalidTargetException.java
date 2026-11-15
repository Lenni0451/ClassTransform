package net.lenni0451.classtransform.exceptions;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;

/**
 * An exception which is thrown when the target of an injection could not be found.
 */
@ParametersAreNonnullByDefault
public class InvalidTargetException extends RuntimeException {

    /**
     * The kind of member reported in the exception message.
     */
    private final String memberType;
    /**
     * The invalid transformer member name and descriptor reported in the exception message.
     */
    private final String memberNameAndDesc;
    /**
     * The transformer name reported in the exception message.
     */
    private final String transformerName;
    /**
     * The unresolvable target selector reported in the exception message.
     */
    private final String target;
    /**
     * The valid target selectors listed in the exception message when not empty.
     */
    private final Collection<String> targets;

    /**
     * Create an exception for an invalid injection target on a transformer method.
     *
     * @param method      The transformer method with the invalid target
     * @param transformer The transformer declaring the method
     * @param target      The target selector which could not be resolved
     * @param targets     The valid target selectors, empty if none are known
     */
    public InvalidTargetException(final MethodNode method, final ClassNode transformer, final String target, final Collection<String> targets) {
        this.memberType = "Method";
        this.memberNameAndDesc = method.name + method.desc;
        this.transformerName = transformer.name;
        this.target = target;
        this.targets = targets;
    }

    @Override
    public String getMessage() {
        String message = this.memberType + " '" + this.memberNameAndDesc + "' in transformer '" + this.transformerName + "' has invalid target '" + this.target + "'";
        if (!this.targets.isEmpty()) {
            message += " (valid targets: ";
            for (String validTarget : this.targets) message += validTarget + ", ";
            message = message.substring(0, message.length() - 2);
            message += ")";
        }
        return message;
    }

}
