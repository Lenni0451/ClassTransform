package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.transformer.impl.CInlineAnnotationHandler;
import net.lenni0451.classtransform.transformer.impl.general.SyntheticMethodGeneralHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * The position to add an annotation handler to in the handler chain.
 */
@ParametersAreNonnullByDefault
public enum HandlerPosition {

    /**
     * Add an annotation handler on the top of the handler chain.
     * <p>
     * The handler is inserted directly after {@link SyntheticMethodGeneralHandler}, before all injection handlers.
     */
    PRE(SyntheticMethodGeneralHandler.class, InsertPosition.AFTER),
    /**
     * Add an annotation handler in the end of the handler chain.
     * <p>
     * The handler is inserted directly before {@link CInlineAnnotationHandler}, after all injection handlers.
     */
    POST(CInlineAnnotationHandler.class, InsertPosition.BEFORE);


    private final Class<? extends AnnotationHandler> target;
    private final InsertPosition insertPosition;

    HandlerPosition(final Class<? extends AnnotationHandler> target, final InsertPosition insertPosition) {
        this.target = target;
        this.insertPosition = insertPosition;
    }

    /**
     * Insert a handler into the given chain relative to this position's anchor.
     * <p>
     * The chain is modified in place. The anchor is matched by exact class, so the chain must contain the anchor
     * handler before this method is called.
     *
     * @param handler    The handler chain to modify
     * @param newHandler The handler to insert
     * @throws RuntimeException If the anchor handler is not part of the chain
     */
    public void add(final List<AnnotationHandler> handler, final AnnotationHandler newHandler) {
        for (int i = 0; i < handler.size(); i++) {
            if (handler.get(i).getClass().equals(this.target)) {
                if (InsertPosition.BEFORE.equals(this.insertPosition)) handler.add(i, newHandler);
                else if (InsertPosition.AFTER.equals(this.insertPosition)) handler.add(i + 1, newHandler);
                return;
            }
        }
        throw new RuntimeException("Unable to find target handler '" + this.target.getName() + "'");
    }


    private enum InsertPosition {
        BEFORE, AFTER
    }

}
