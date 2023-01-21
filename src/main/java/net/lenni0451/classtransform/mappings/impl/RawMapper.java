package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;

/**
 * A remapper that uses the given {@link MapRemapper} for remapping.
 */
public class RawMapper extends AMapper {

    private final MapRemapper remapper;

    public RawMapper(final MapperConfig config, final MapRemapper remapper) {
        super(config);
        this.remapper = remapper;
    }

    @Override
    protected void init() throws Throwable {
        super.remapper.copy(this.remapper);
    }

}
