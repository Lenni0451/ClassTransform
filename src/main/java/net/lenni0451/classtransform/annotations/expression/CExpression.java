package net.lenni0451.classtransform.annotations.expression;

import net.lenni0451.classtransform.annotations.CTarget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Match a complex piece of bytecode with a java-like expression.<br>
 * The expression is attached to a transformer method and selected with
 * {@code @CTarget(value = "EXPRESSION", target = "id")}.<br>
 * <br>
 * Example:
 * <pre>
 * &#64;CExpression("?.(*)")
 * &#64;CModifyExpressionValue(method = "print", target = &#64;CTarget(value = "EXPRESSION", target = ""))
 * private static String modify(String original) {
 *     return original.toUpperCase();
 * }
 * </pre>
 *
 * @see CDefinition
 * @see CTarget
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@Repeatable(CExpressions.class)
public @interface CExpression {

    /**
     * The expressions to match.
     *
     * @return The expressions
     */
    String[] value();

    /**
     * The id of this expression.<br>
     * Used to select the expression from a {@link CTarget}.
     *
     * @return The expression id
     */
    String id() default "";

}
