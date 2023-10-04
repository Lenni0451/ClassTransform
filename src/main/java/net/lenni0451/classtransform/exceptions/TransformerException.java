package net.lenni0451.classtransform.exceptions;

import net.lenni0451.classtransform.utils.Codifier;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Modifier;

import static net.lenni0451.classtransform.utils.Types.type;

/**
 * An exception which is thrown when there is a general error during the transformation.
 */
@ParametersAreNonnullByDefault
public class TransformerException extends RuntimeException {

    public static TransformerException wrongStaticAccess(final MethodNode transformerMethod, final ClassNode transformer, final boolean shouldBeStatic) {
        return new TransformerException(transformerMethod, transformer, "must " + (shouldBeStatic ? "" : "not ") + "be static")
                .help(Codifier.of(transformerMethod).access(shouldBeStatic ? transformerMethod.access | Modifier.STATIC : transformerMethod.access & ~Modifier.STATIC));
    }

    public static TransformerException mustReturnVoid(final MethodNode transformerMethod, final ClassNode transformer) {
        return new TransformerException(transformerMethod, transformer, "must return 'void'")
                .help(Codifier.of(transformerMethod).returnType(Type.VOID_TYPE));
    }

    public static TransformerException wrongArguments(final MethodNode transformerMethod, final ClassNode transformer, final Class<?>... expected) {
        Type[] argTypes = new Type[expected.length];
        for (int i = 0; i < expected.length; i++) argTypes[i] = type(expected[i]);
        return wrongArguments(transformerMethod, transformer, argTypes);
    }

    public static TransformerException wrongArguments(final MethodNode transformerMethod, final ClassNode transformer, final Type... expected) {
        String expectedArgs = "must have ";
        if (expected.length == 0) {
            expectedArgs += "no arguments";
        } else {
            expectedArgs += "the following argument";
            if (expected.length != 1) expectedArgs += "s";
        }
        throw new TransformerException(transformerMethod, transformer, expectedArgs)
                .help(Codifier.of(transformerMethod).param(null).params(expected));
    }

    public static TransformerException alreadyExists(final MethodNode method, final ClassNode transformer, final ClassNode transformedClass) {
        return new TransformerException(method, transformer, "already exists in class '" + transformedClass.name + "'");
    }

    public static TransformerException alreadyExists(final FieldNode field, final ClassNode transformer, final ClassNode transformedClass) {
        return new TransformerException(field, transformer, "already exists in class '" + transformedClass.name + "'");
    }


    private final String memberType;
    private final String memberNameAndDesc;
    private final String transformerName;
    private final String state;

    private String help;

    public TransformerException(final FieldNode field, final ClassNode transformer, final String state) {
        this.memberType = "Field";
        this.memberNameAndDesc = field.name + field.desc;
        this.transformerName = transformer.name;
        this.state = state;
    }

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
