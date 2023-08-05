package net.lenni0451.classtransform.utils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

/**
 * Util class to generate method header strings.<br>
 * This is used to print example method headers when a transformer requires a specific method signature.
 */
@ParametersAreNonnullByDefault
public class Codifier {

    /**
     * @return An empty codifier
     */
    public static Codifier get() {
        return new Codifier();
    }

    /**
     * Create a codifier of the given method node.
     *
     * @param method The method node
     * @return The codifier
     */
    public static Codifier of(final MethodNode method) {
        return Codifier.get()
                .access(method.access)
                .returnType(Type.getReturnType(method.desc))
                .name(method.name)
                .params(Type.getArgumentTypes(method.desc))
                .exceptions(method.exceptions == null ? new Type[0] : method.exceptions.stream().map(Type::getObjectType).toArray(Type[]::new))
                ;
    }


    private String access = "";
    private String returnType = "";
    private String name = "";
    private final List<String> parameters = new ArrayList<>();
    private final List<String> exceptions = new ArrayList<>();
    private String body = "";

    private Codifier() {
    }

    /**
     * Set the access modifier of the method.
     *
     * @param access The access modifier
     * @return This codifier
     */
    public Codifier access(final int access) {
        final boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
        final boolean isPrivate = (access & Opcodes.ACC_PRIVATE) != 0;
        final boolean isProtected = (access & Opcodes.ACC_PROTECTED) != 0;
        final boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;

        this.access = "";
        if (isPublic) this.access += "public";
        else if (isPrivate) this.access += "private";
        else if (isProtected) this.access += "protected";
        if (isStatic) this.access += (this.access.isEmpty() ? "" : " ") + "static";
        return this;
    }

    /**
     * Set the return type of the method.
     *
     * @param returnType The return type
     * @return This codifier
     */
    public Codifier returnType(final Type returnType) {
        this.returnType = returnType.getClassName();
        if (this.returnType.contains(".")) this.returnType = this.stripPackage(this.returnType);
        return this;
    }

    /**
     * Set the name of the method.
     *
     * @param name The name
     * @return This codifier
     */
    public Codifier name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Add a parameter to the method.<br>
     * If {@code null} is passed as the parameter the current parameter list will be cleared.
     *
     * @param parameter The parameter
     * @return This codifier
     */
    public Codifier param(@Nullable final Type parameter) {
        if (parameter == null) {
            this.parameters.clear();
            return this;
        }

        String className = this.stripPackage(parameter.getClassName());
        if (this.isJavaKeyword(className)) className += " " + className.charAt(0);
        else className += " " + className.substring(0, 1).toLowerCase() + className.substring(1);
        this.parameters.add(className);
        return this;
    }

    /**
     * Add multiple parameters to the method.
     *
     * @param parameters The parameters
     * @return This codifier
     */
    public Codifier params(final Type... parameters) {
        for (Type type : parameters) this.param(type);
        return this;
    }

    /**
     * Add an exception to the method.<br>
     * If {@code null} is passed as the exception the current exception list will be cleared.
     *
     * @param exception The exception
     * @return This codifier
     */
    public Codifier exception(@Nullable final Type exception) {
        if (exception == null) {
            this.exceptions.clear();
            return this;
        }

        this.exceptions.add(this.stripPackage(exception.getClassName()));
        return this;
    }

    /**
     * Add multiple exceptions to the method.
     *
     * @param exceptions The exceptions
     * @return This codifier
     */
    public Codifier exceptions(final Type... exceptions) {
        for (Type type : exceptions) this.exception(type);
        return this;
    }

    /**
     * Set the body of the method.
     *
     * @param body The body
     * @return This codifier
     */
    public Codifier body(final String body) {
        this.body = body;
        return this;
    }

    /**
     * @return The generated method header
     */
    public String build() {
        StringBuilder out = new StringBuilder();
        if (!this.access.isEmpty()) out.append(this.access).append(" ");
        if (!this.returnType.isEmpty()) out.append(this.returnType).append(" ");
        if (!this.name.isEmpty()) out.append(this.name).append("(").append(String.join(", ", this.parameters)).append(")");
        if (!this.exceptions.isEmpty()) out.append(" throws ").append(String.join(", ", this.exceptions));
        if (!this.body.isEmpty()) out.append(" ").append(this.body);
        return out.toString().trim();
    }


    private String stripPackage(final String className) {
        if (className.contains(".")) return className.substring(className.lastIndexOf(".") + 1);
        return className;
    }

    private boolean isJavaKeyword(final String s) {
        return s.equals("abstract") || s.equals("assert") || s.equals("boolean") || s.equals("break") || s.equals("byte") || s.equals("case") || s.equals("catch") || s.equals("char") || s.equals("class") || s.equals("const") || s.equals("continue") || s.equals("default") || s.equals("do") || s.equals("double") || s.equals("else") || s.equals("enum") || s.equals("extends") || s.equals("final") || s.equals("finally") || s.equals("float") || s.equals("for") || s.equals("goto") || s.equals("if") || s.equals("implements") || s.equals("import") || s.equals("instanceof") || s.equals("int") || s.equals("interface") || s.equals("long") || s.equals("native") || s.equals("new") || s.equals("package") || s.equals("private") || s.equals("protected") || s.equals("public") || s.equals("return") || s.equals("short") || s.equals("static") || s.equals("strictfp") || s.equals("super") || s.equals("switch") || s.equals("synchronized") || s.equals("this") || s.equals("throw") || s.equals("throws") || s.equals("transient") || s.equals("try") || s.equals("void") || s.equals("volatile") || s.equals("while");
    }

}
