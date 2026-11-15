package net.lenni0451.classtransform.exceptions;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An exception which is thrown when a field could not be found during the transformation.
 */
@ParametersAreNonnullByDefault
public class FieldNotFoundException extends RuntimeException {

    /**
     * The searched class name reported in the exception message.
     */
    private final String targetClassName;
    /**
     * The requesting transformer name reported in the exception message.
     */
    private final String transformerName;
    /**
     * The missing field name and descriptor reported in the exception message.
     */
    private final String fieldNameAndDesc;

    /**
     * Create an exception for a field which the target class does not declare.
     *
     * @param targetClass      The class which was searched
     * @param transformer      The transformer which requested the field
     * @param fieldNameAndDesc The name and descriptor of the missing field
     */
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
