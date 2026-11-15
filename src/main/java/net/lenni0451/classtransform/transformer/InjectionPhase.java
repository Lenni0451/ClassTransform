package net.lenni0451.classtransform.transformer;

/**
 * The phases of the injection pipeline.<br>
 * All regular injections are applied before expression modifiers and conditions.
 * <p>
 * Phases are applied in declaration order, and plans within one phase are sorted by their explicit order value. Stable
 * ties preserve transformer registration and handler method declaration order.
 */
public enum InjectionPhase {

    /** Regular callbacks, redirects, constant modifiers and return value modifiers. */
    REGULAR,
    /** Expression modifiers run after every regular injection. */
    EXPRESSION,
    /** Conditions run after regular injections and expression modifiers. */
    CONDITION,
    /** Operation wrappers run after every other injection to allow wrapping redirects and other replacements. */
    LATE

}
