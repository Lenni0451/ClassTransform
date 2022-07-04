package net.lenni0451.classtransform.exceptions;

import org.objectweb.asm.tree.ClassNode;

public class FieldNotFoundException extends RuntimeException {

    private final String targetClassName;
    private final String transformerName;
    private final String methodNameAndDesc;

    public FieldNotFoundException(final ClassNode targetClass, final ClassNode transformer, final String methodNameAndDesc) {
        this.targetClassName = targetClass.name;
        this.transformerName = transformer.name;
        this.methodNameAndDesc = methodNameAndDesc;
    }

    @Override
    public String getMessage() {
        return "Target class '" + this.targetClassName + "' does not have field '" + this.methodNameAndDesc + "' from transformer '" + this.transformerName + "'";
    }

}
