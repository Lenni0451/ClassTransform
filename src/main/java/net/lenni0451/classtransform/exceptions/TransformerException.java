package net.lenni0451.classtransform.exceptions;

import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class TransformerException extends RuntimeException {

    private final String memberType;
    private final String memberNameAndDesc;
    private final String transformerName;
    private final String state;

    private String help;

    public TransformerException(final MethodNode method, final ClassNode transformer, final String state) {
        this.memberType = "Method";
        this.memberNameAndDesc = method.name + method.desc;
        this.transformerName = transformer.name;
        this.state = state;
    }

    public TransformerException help(final Codifier codifier) {
        return this.help(codifier.build());
    }

    public TransformerException help(final String help) {
        this.help = help;
        return this;
    }

    @Override
    public String getMessage() {
        String message = this.memberType + " '" + this.memberNameAndDesc + "' in transformer '" + this.transformerName + "' " + this.state;
        if (this.help != null) message += ": " + this.help;
        return message;
    }

}
