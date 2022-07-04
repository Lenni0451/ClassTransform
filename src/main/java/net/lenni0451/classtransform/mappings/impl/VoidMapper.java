package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;

public class VoidMapper extends AMapper {

    public VoidMapper() {
        super(MapperConfig.create());
    }

    @Override
    protected void init() {
    }

}
