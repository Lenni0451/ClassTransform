package net.lenni0451.classtransform.mappings.impl;

import net.lenni0451.classtransform.mappings.AMapper;
import net.lenni0451.classtransform.mappings.MapperConfig;
import net.lenni0451.classtransform.utils.mappings.MapRemapper;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A remapper that uses the given {@link MapRemapper} for remapping.
 * <p>
 * The mappings are copied once when the mapper is loaded, so later changes to the source remapper
 * have no effect on this mapper.
 */
@ParametersAreNonnullByDefault
public class RawMapper extends AMapper {

    private final MapRemapper remapper;

    /**
     * Create a mapper that copies the mappings of the given remapper.
     *
     * @param config   The mapper config
     * @param remapper The remapper to copy the mappings from
     */
    public RawMapper(final MapperConfig config, final MapRemapper remapper) {
        super(config);
        this.remapper = remapper;
    }

    @Override
    protected void init() {
        super.remapper.copy(this.remapper);
    }

}
