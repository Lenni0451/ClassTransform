package net.lenni0451.classtransform.transformer;

import net.lenni0451.classtransform.transformer.impl.CASMTransformer;
import net.lenni0451.classtransform.transformer.impl.CInlineTransformer;

import java.util.List;

public enum HandlerPosition {

    /**
     * Add an annotation transformer on the top of the handler chain
     */
    PRE(CASMTransformer.class, InsertPosition.AFTER),
    /**
     * Add an annotation transformer in the end of the handler chain
     */
    POST(CInlineTransformer.class, InsertPosition.BEFORE);


    private final Class<? extends ATransformer> target;
    private final InsertPosition insertPosition;

    HandlerPosition(final Class<? extends ATransformer> target, final InsertPosition insertPosition) {
        this.target = target;
        this.insertPosition = insertPosition;
    }

    public void add(final List<ATransformer> annotationHandler, final ATransformer newHandler) {
        for (int i = 0; i < annotationHandler.size(); i++) {
            if (annotationHandler.get(i).getClass().equals(this.target)) {
                if (InsertPosition.BEFORE.equals(this.insertPosition)) annotationHandler.add(i, newHandler);
                else if (InsertPosition.AFTER.equals(this.insertPosition)) annotationHandler.add(i + 1, newHandler);
                return;
            }
        }
        throw new RuntimeException("Unable to find target handler '" + this.target.getName() + "'");
    }


    private enum InsertPosition {
        BEFORE, AFTER
    }

}
