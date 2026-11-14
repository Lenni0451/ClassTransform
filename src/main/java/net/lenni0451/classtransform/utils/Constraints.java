package net.lenni0451.classtransform.utils;

import net.lenni0451.classtransform.TransformerManager;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser and evaluator for simple Mixin style constraints.<br>
 * Constraints have the form {@code TOKEN(range)} and multiple constraints are separated by semicolons.
 */
@ParametersAreNonnullByDefault
public class Constraints {

    private static final Pattern PATTERN = Pattern.compile("^([A-Z0-9\\-_\\.]+)\\((?:(<|<=|>|>=|=)?([0-9]+)(<|(-)([0-9]+)?|>|(\\+)([0-9]+)?)?)?\\)$");

    /**
     * Validate a constraint expression against the tokens registered in the transformer manager.
     *
     * @param manager    The transformer manager
     * @param expression The constraint expression
     * @throws IllegalArgumentException If the expression is invalid, a token is unknown or a constraint fails
     */
    public static void check(final TransformerManager manager, final String expression) {
        if (expression == null) return;
        String normalized = expression.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) return;
        for (String constraint : normalized.split(";")) {
            if (constraint.isEmpty()) continue;
            Matcher matcher = PATTERN.matcher(constraint);
            if (!matcher.matches()) throw new IllegalArgumentException("Invalid constraint syntax '" + constraint + "'");
            String token = matcher.group(1);
            Integer value = manager.getConstraintToken(token);
            if (value == null) throw new IllegalArgumentException("Constraint token '" + token + "' is not registered");
            int min = Integer.MIN_VALUE;
            int max = Integer.MAX_VALUE;
            if (matcher.group(3) != null) {
                int number = Integer.parseInt(matcher.group(3));
                min = number;
                max = number;
                String leading = matcher.group(2);
                if (matcher.group(6) != null) {
                    max = Integer.parseInt(matcher.group(6));
                    if (max < min) throw new IllegalArgumentException("Invalid constraint range '" + constraint + "'");
                } else if (matcher.group(8) != null) {
                    max = min + Integer.parseInt(matcher.group(8));
                } else if (matcher.group(4) != null) {
                    max = min;
                    min = Integer.MIN_VALUE;
                } else if (matcher.group(7) != null) {
                    max = Integer.MAX_VALUE;
                } else if (leading != null) {
                    if (">".equals(leading)) min = number + 1;
                    else if ("<".equals(leading)) max = number - 1;
                    else if (">=".equals(leading)) min = number;
                    else if ("<=".equals(leading)) max = number;
                }
            }
            if (value < min || value > max) {
                throw new IllegalArgumentException("Constraint '" + constraint + "' failed with token value " + value);
            }
        }
    }

}
