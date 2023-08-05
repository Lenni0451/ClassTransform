package net.lenni0451.classtransform.exceptions;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An exception which is thrown when a method could not be found during the transformation.
 */
@ParametersAreNonnullByDefault
public class MethodNotFoundException extends RuntimeException {

    private final String targetClassName;
    private final String transformerName;
    private final String methodNameAndDesc;

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
