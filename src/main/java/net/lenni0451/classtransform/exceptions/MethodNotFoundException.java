package net.lenni0451.classtransform.exceptions;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An exception which is thrown when a method could not be found during the transformation.
 */
@ParametersAreNonnullByDefault
public class MethodNotFoundException extends RuntimeException {

    /**
     * The searched class name reported in the exception message.
     */
    private final String targetClassName;
    /**
     * The requesting transformer name reported in the exception message.
     */
    private final String transformerName;
    /**
     * The missing method name and descriptor reported in the exception message.
     */
    private final String methodNameAndDesc;

    /**
     * Create an exception for a method which the target class does not declare.
     *
     * @param targetClass       The class which was searched
     * @param transformer       The transformer which requested the method
     * @param methodNameAndDesc The name and descriptor of the missing method
     */
    public MethodNotFoundException(final ClassNode targetClass, final ClassNode transformer, final String methodNameAndDesc) {
        this.targetClassName = targetClass.name;
        this.transformerName = transformer.name;
        this.methodNameAndDesc = methodNameAndDesc;
    }

    @Override
    public String getMessage() {
        return "Target class '" + this.targetClassName + "' does not have method '" + this.methodNameAndDesc + "' from transformer '" + this.transformerName + "'";
    }

}
