package net.lenni0451.classtransform.annotations.expression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeated {@link CExpression} annotations.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CExpressions {

    /**
     * @return The repeated expressions
     */
    CExpression[] value();

}
