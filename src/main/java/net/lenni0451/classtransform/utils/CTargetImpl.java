package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CTarget;

import java.lang.annotation.Annotation;

public class CTargetImpl {

    public static CTarget invoke(final String methodDeclaration, final int ordinal) {
        return of("INVOKE", methodDeclaration, CTarget.Shift.AFTER, ordinal, false);
    }

    public static CTarget getfield(final String fieldDeclaration, final int ordinal) {
        return of("GETFIELD", fieldDeclaration, CTarget.Shift.AFTER, ordinal, false);
    }

    public static CTarget putfield(final String fieldDeclaration, final int ordinal) {
        return of("PUTFIELD", fieldDeclaration, CTarget.Shift.AFTER, ordinal, false);
    }

    public static CTarget of(final String value, final String target, final CTarget.Shift shift, final int ordinal, final boolean optional) {
        return of(value, target, shift, ordinal, optional, new String[0]);
    }

    public static CTarget of(final String value, final String target, final CTarget.Shift shift, final int ordinal, final boolean optional, final String[] args) {
        return new CTarget() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return CTarget.class;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public String target() {
                return target;
            }

            @Override
            public Shift shift() {
                return shift;
            }

            @Override
            public int ordinal() {
                return ordinal;
            }

            @Override
            public boolean optional() {
                return optional;
            }

            @Override
            public String slice() {
                return "";
            }

            @Override
            public int opcode() {
                return -1;
            }

            @Override
            public int by() {
                return 0;
            }

            @Override
            public boolean remap() {
                return true;
            }

            @Override
            public String id() {
                return "";
            }

            @Override
            public String selector() {
                return "ONE";
            }

            @Override
            public String[] args() {
                return args.clone();
            }
        };
    }

}
