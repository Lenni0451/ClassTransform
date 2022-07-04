package net.lenni0451.classtransform.exceptions;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collection;

public class InvalidTargetException extends RuntimeException {

    private final String memberType;
    private final String memberNameAndDesc;
    private final String transformerName;
    private final String target;
    private final Collection<String> targets;

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
