package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;

/**
 * Only used internally when no other {@link AMapper} has been specified in the {@link TransformerManager}.<br>
 * This mapper does not remap anything but is still required to fill all annotation values with the correct values (e.g. resolving wildcard members).
 */
public class VoidMapper extends AMapper {

    public VoidMapper() {
        super(MapperConfig.create());
    }

    @Override
    protected void init() {
    }

}
