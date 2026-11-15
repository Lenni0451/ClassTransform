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
 * <p>
 * The message names the failing transformer member and transformer class. Optional target class and member context
 * can be attached through the fluent modifier methods before the exception is thrown, as can a help hint.
 */
@ParametersAreNonnullByDefault
public class TransformerException extends RuntimeException {

    /**
     * Create an exception for a transformer method with a wrong {@code static} modifier.
     *
     * @param transformerMethod The transformer method
     * @param transformer       The transformer class declaring the method
     * @param shouldBeStatic    If the method is required to be static
     * @return The created exception with a corrected method header as help
     */
    public static TransformerException wrongStaticAccess(final MethodNode transformerMethod, final ClassNode transformer, final boolean shouldBeStatic) {
        return new TransformerException(transformerMethod, transformer, "must " + (shouldBeStatic ? "" : "not ") + "be static")
                .help(Codifier.of(transformerMethod).access(shouldBeStatic ? transformerMethod.access | Modifier.STATIC : transformerMethod.access & ~Modifier.STATIC));
    }

    /**
     * Create an exception for a transformer method which does not return {@code void}.
     *
     * @param transformerMethod The transformer method
     * @param transformer       The transformer class declaring the method
     * @return The created exception with a corrected method header as help
     */
    public static TransformerException mustReturnVoid(final MethodNode transformerMethod, final ClassNode transformer) {
        return new TransformerException(transformerMethod, transformer, "must return 'void'")
                .help(Codifier.of(transformerMethod).returnType(Type.VOID_TYPE));
    }

    /**
     * Create an exception for a transformer method with wrong arguments.
     * <p>
     * The expected types are supplied as classes and converted to ASM types before the message is built.
     *
     * @param transformerMethod The transformer method
     * @param transformer       The transformer class declaring the method
     * @param expected          The expected argument types as classes
     * @return The created exception with a corrected method header as help
     */
    public static TransformerException wrongArguments(final MethodNode transformerMethod, final ClassNode transformer, final Class<?>... expected) {
        Type[] argTypes = new Type[expected.length];
        for (int i = 0; i < expected.length; i++) argTypes[i] = type(expected[i]);
        return wrongArguments(transformerMethod, transformer, argTypes);
    }

    /**
     * Create an exception for a transformer method with wrong arguments.
     * <p>
     * This overload takes already converted ASM types, so no class conversion happens here.
     *
     * @param transformerMethod The transformer method
     * @param transformer       The transformer class declaring the method
     * @param expected          The expected argument types as ASM types
     * @return The created exception with a corrected method header as help
     */
    public static TransformerException wrongArguments(final MethodNode transformerMethod, final ClassNode transformer, final Type... expected) {
        String expectedArgs = "must have ";
        if (expected.length == 0) {
            expectedArgs += "no arguments";
        } else {
            expectedArgs += "the following argument";
            if (expected.length != 1) expectedArgs += "s";
        }
        return new TransformerException(transformerMethod, transformer, expectedArgs)
                .help(Codifier.of(transformerMethod).param(null).params(expected));
    }

    /**
     * Create an exception for a method which already exists in the transformed class.
     *
     * @param method           The method which should have been added
     * @param transformer      The transformer declaring the method
     * @param transformedClass The class which already declares the method
     * @return The created exception
     */
    public static TransformerException alreadyExists(final MethodNode method, final ClassNode transformer, final ClassNode transformedClass) {
        return new TransformerException(method, transformer, "already exists in class '" + transformedClass.name + "'");
    }

    /**
     * Create an exception for a field which already exists in the transformed class.
     *
     * @param field            The field which should have been added
     * @param transformer      The transformer declaring the field
     * @param transformedClass The class which already declares the field
     * @return The created exception
     */
    public static TransformerException alreadyExists(final FieldNode field, final ClassNode transformer, final ClassNode transformedClass) {
        return new TransformerException(field, transformer, "already exists in class '" + transformedClass.name + "'");
    }

    /**
     * Create an exception for a member declaration which could not be parsed.
     *
     * @param transformerMethod        The transformer method declaring the member declaration
     * @param transformer              The transformer class declaring the method
     * @param invalidMemberDeclaration The unparsable declaration
     * @return The created exception with a declaration syntax hint as help
     */
    public static TransformerException invalidMemberDeclaration(final MethodNode transformerMethod, final ClassNode transformer, final String invalidMemberDeclaration) {
        return new TransformerException(transformerMethod, transformer, "has invalid member declaration '" + invalidMemberDeclaration + "'")
                .help("e.g. Ljava/lang/String;toString()V, Ljava/lang/Integer;MAX_VALUE:I");
    }


    /**
     * The kind of member reported in the exception message.
     */
    private final String memberType;
    /**
     * The transformer member name and descriptor reported in the exception message.
     */
    private final String memberNameAndDesc;
    /**
     * The transformer name reported in the exception message.
     */
    private final String transformerName;
    /**
     * The failure description appended to the exception message.
     */
    private final String state;

    /**
     * The optional help hint appended to the exception message, {@code null} if absent.
     */
    private String help;
    /**
     * The target class name appended to the exception message, {@code null} if not set.
     */
    private String targetClass;
    /**
     * The target member description appended to the exception message, {@code null} if not set.
     */
    private String targetMember;

    /**
     * Create an exception for a transformer field.
     *
     * @param field       The transformer field in a failed state
     * @param transformer The transformer class declaring the field
     * @param state       A description of the failure, appended to the message
     */
    public TransformerException(final FieldNode field, final ClassNode transformer, final String state) {
        this.memberType = "Field";
        this.memberNameAndDesc = field.name + ":" + field.desc;
        this.transformerName = transformer.name;
        this.state = state;
    }

    /**
     * Create an exception for a transformer method.
     *
     * @param method      The transformer method in a failed state
     * @param transformer The transformer class declaring the method
     * @param state       A description of the failure, appended to the message
     */
    public TransformerException(final MethodNode method, final ClassNode transformer, final String state) {
        this.memberType = "Method";
        this.memberNameAndDesc = method.name + method.desc;
        this.transformerName = transformer.name;
        this.state = state;
    }

    /**
     * Set the cause of this exception.
     *
     * @param cause The underlying cause
     * @return This exception
     */
    public TransformerException cause(final Throwable cause) {
        this.initCause(cause);
        return this;
    }

    /**
     * Set a help hint built from the given codifier.
     *
     * @param codifier The codifier to build the hint from
     * @return This exception
     */
    public TransformerException help(final Codifier codifier) {
        return this.help(codifier.build());
    }

    /**
     * Set a help hint shown after the failure description.
     *
     * @param help The help text
     * @return This exception
     */
    public TransformerException help(final String help) {
        this.help = help;
        return this;
    }

    /**
     * Set the class which was being transformed.
     *
     * @param targetClass The internal name of the target class
     * @return This exception
     */
    public TransformerException targetClass(final String targetClass) {
        this.targetClass = targetClass;
        return this;
    }

    /**
     * Set the target method; it is only shown in the message when a target class is set.
     *
     * @param targetMethod The target method
     * @return This exception
     */
    public TransformerException targetMethod(final MethodNode targetMethod) {
        this.targetMember = "method '" + targetMethod.name + targetMethod.desc + "'";
        return this;
    }

    /**
     * Set the target field; it is only shown in the message when a target class is set.
     *
     * @param targetField The target field
     * @return This exception
     */
    public TransformerException targetField(final FieldNode targetField) {
        this.targetMember = "field '" + targetField.name + ":" + targetField.desc + "'";
        return this;
    }

    @Override
    public String getMessage() {
        String message = this.memberType + " '" + this.memberNameAndDesc + "' in transformer '" + this.transformerName + "' " + this.state;
        if (this.targetClass != null) {
            message += " when targeting class '" + this.targetClass + "'";
            if (this.targetMember != null) message += " and " + this.targetMember;
        }
        if (this.help != null) message += ": " + this.help;
        return message;
    }

}
