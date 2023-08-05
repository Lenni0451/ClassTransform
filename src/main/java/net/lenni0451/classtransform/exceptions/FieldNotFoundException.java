package net.lenni0451.classtransform.exceptions;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An exception which is thrown when a field could not be found during the transformation.
 */
@ParametersAreNonnullByDefault
public class FieldNotFoundException extends RuntimeException {

    private final String targetClassName;
    private final String transformerName;
    private final String fieldNameAndDesc;

    public FieldNotFoundException(final ClassNode targetClass, final ClassNode transformer, final String fieldNameAndDesc) {
        this.targetClassName = targetClass.name;
        this.transformerName = transformer.name;
        this.fieldNameAndDesc = fieldNameAndDesc;
    }

    @Override
    public String getMessage() {
        return "Target class '" + this.targetClassName + "' does not have field '" + this.fieldNameAndDesc + "' from transformer '" + this.transformerName + "'";
    }

}
