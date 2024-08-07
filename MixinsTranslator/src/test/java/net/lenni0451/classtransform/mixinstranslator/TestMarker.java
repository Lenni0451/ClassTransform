package net.lenni0451.classtransform.mixinstranslator;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestMarker {

    Class<? extends Annotation>[] from();

    Class<? extends Annotation>[] to();

}
