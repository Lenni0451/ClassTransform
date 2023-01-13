package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.transformer.impl.CInlineAnnotationHandler;
import net.lenni0451.classtransform.transformer.impl.general.SyntheticMethodGeneralHandler;

import java.util.List;

public enum HandlerPosition {

    /**
     * Add an annotation handler on the top of the handler chain
     */
    PRE(SyntheticMethodGeneralHandler.class, InsertPosition.AFTER),
    /**
     * Add an annotation handler in the end of the handler chain
     */
    POST(CInlineAnnotationHandler.class, InsertPosition.BEFORE);


    private final Class<? extends AnnotationHandler> target;
    private final InsertPosition insertPosition;

    HandlerPosition(final Class<? extends AnnotationHandler> target, final InsertPosition insertPosition) {
        this.target = target;
        this.insertPosition = insertPosition;
    }

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
