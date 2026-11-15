package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.annotations.CDesc;
import net.lenni0451.classtransform.annotations.CTarget;

import java.lang.annotation.Annotation;

/**
 * Factory methods for creating {@link CTarget} annotation instances programmatically.
 */
public class CTargetImpl {

    private static final CDesc EMPTY_DESC = new CDesc() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return CDesc.class;
        }

        @Override
        public String id() {
            return "";
        }

        @Override
        public Class<?> owner() {
            return void.class;
        }

        @Override
        public String value() {
            return "";
        }

        @Override
        public Class<?> ret() {
            return void.class;
        }

        @Override
        public Class<?>[] args() {
            return new Class<?>[0];
        }

        @Override
        public CDesc.Next[] next() {
            return new CDesc.Next[0];
        }

        @Override
        public int min() {
            return 0;
        }

        @Override
        public int max() {
            return Integer.MAX_VALUE;
        }
    };

    /**
     * Create a target selecting a method invocation.
     *
     * @param methodDeclaration The declaration of the invoked method
     * @param ordinal           The zero-based index of the matching invocation, or a negative value for any
     * @return The target
     */
    public static CTarget invoke(final String methodDeclaration, final int ordinal) {
        return of("INVOKE", methodDeclaration, CTarget.Shift.AFTER, ordinal, false);
    }

    /**
     * Create a target selecting a field read.
     *
     * @param fieldDeclaration The declaration of the read field
     * @param ordinal          The zero-based index of the matching read, or a negative value for any
     * @return The target
     */
    public static CTarget getfield(final String fieldDeclaration, final int ordinal) {
        return of("GETFIELD", fieldDeclaration, CTarget.Shift.AFTER, ordinal, false);
    }

    /**
     * Create a target selecting a field write.
     *
     * @param fieldDeclaration The declaration of the written field
     * @param ordinal          The zero-based index of the matching write, or a negative value for any
     * @return The target
     */
    public static CTarget putfield(final String fieldDeclaration, final int ordinal) {
        return of("PUTFIELD", fieldDeclaration, CTarget.Shift.AFTER, ordinal, false);
    }

    /**
     * Create a target with no descriptor selector or explicit arguments.
     *
     * @param value    The target type, for example {@code INVOKE} or {@code HEAD}
     * @param target   The member declaration to select
     * @param shift    The instruction shift
     * @param ordinal  The zero-based index of the matching instruction, or a negative value for any
     * @param optional If a missing match is allowed
     * @return The target
     */
    public static CTarget of(final String value, final String target, final CTarget.Shift shift, final int ordinal, final boolean optional) {
        return of(value, target, shift, ordinal, optional, new String[0]);
    }

    /**
     * Create a target with explicit arguments and no descriptor selector.
     *
     * @param value    The target type, for example {@code INVOKE} or {@code HEAD}
     * @param target   The member declaration to select
     * @param shift    The instruction shift
     * @param ordinal  The zero-based index of the matching instruction, or a negative value for any
     * @param optional If a missing match is allowed
     * @param args     The expected invocation arguments
     * @return The target
     */
    public static CTarget of(final String value, final String target, final CTarget.Shift shift, final int ordinal, final boolean optional, final String[] args) {
        return of(value, target, shift, ordinal, optional, args, EMPTY_DESC);
    }

    /**
     * Create a target with all selector values.
     * <p>
     * The returned target has an empty slice and id, opcode {@code -1}, offset {@code 0}, remapping enabled and the {@code ONE} selector.
     *
     * @param value    The target type, for example {@code INVOKE} or {@code HEAD}
     * @param target   The member declaration to select
     * @param shift    The instruction shift
     * @param ordinal  The zero-based index of the matching instruction, or a negative value for any
     * @param optional If a missing match is allowed
     * @param args     The expected invocation arguments
     * @param desc     The descriptor selector
     * @return The target
     */
    public static CTarget of(final String value, final String target, final CTarget.Shift shift, final int ordinal, final boolean optional, final String[] args, final CDesc desc) {
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

            @Override
            public CDesc desc() {
                return desc;
            }
        };
    }

    /**
     * Clone a target with a different descriptor selector.
     *
     * @param target The original target
     * @param desc   The new descriptor selector
     * @return The cloned target
     */
    public static CTarget withDesc(final CTarget target, final CDesc desc) {
        return new CTarget() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return CTarget.class;
            }

            @Override
            public String value() {
                return target.value();
            }

            @Override
            public String target() {
                return target.target();
            }

            @Override
            public Shift shift() {
                return target.shift();
            }

            @Override
            public int ordinal() {
                return target.ordinal();
            }

            @Override
            public boolean optional() {
                return target.optional();
            }

            @Override
            public String slice() {
                return target.slice();
            }

            @Override
            public int opcode() {
                return target.opcode();
            }

            @Override
            public int by() {
                return target.by();
            }

            @Override
            public boolean remap() {
                return target.remap();
            }

            @Override
            public String id() {
                return target.id();
            }

            @Override
            public String selector() {
                return target.selector();
            }

            @Override
            public String[] args() {
                return target.args();
            }

            @Override
            public CDesc desc() {
                return desc;
            }
        };
    }

}
