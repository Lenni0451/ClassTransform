package net.lenni0451.classtransform.mappings;

import net.lenni0451.classtransform.utils.FailStrategy;

import javax.annotation.Nonnull;

/**
 * A config to modify the behavior of an {@link AMapper}.
 */
public class MapperConfig {

    public static MapperConfig create() {
        return new MapperConfig();
    }


    protected boolean fillSuperMappings = false;
    protected FailStrategy superMappingsFailStrategy;
    protected boolean remapTransformer = false;

    private MapperConfig() {
    }

    /**
     * Fill all super mappings of the remapper.<br>
     * This is required if the mappings are missing mappings for super methods.
     *
     * @param fillSuperMappings If super mappings should be filled (default: false)
     * @return This config
     */
    public MapperConfig fillSuperMappings(final boolean fillSuperMappings) {
        return this.fillSuperMappings(fillSuperMappings, FailStrategy.CONTINUE);
    }

    /**
     * Fill all super mappings of the remapper.<br>
     * This is required if the mappings are missing mappings for super methods.
     *
     * @param fillSuperMappings         If super mappings should be filled (default: false)
     * @param superMappingsFailStrategy The fail strategy to use if super mappings could not be filled (default: {@link FailStrategy#CONTINUE})
     * @return This config
     */
    public MapperConfig fillSuperMappings(final boolean fillSuperMappings, @Nonnull final FailStrategy superMappingsFailStrategy) {
        this.fillSuperMappings = fillSuperMappings;
        this.superMappingsFailStrategy = superMappingsFailStrategy;
        return this;
    }

    /**
     * Remap the transformer class to match the transformed class.<br>
     * This can be used if the transformer is made using a remapped class which is still obfuscated during runtime.
     *
     * @param remapTransformer If the transformer should be remapped (default: false)
     * @return This config
     */
    public MapperConfig remapTransformer(final boolean remapTransformer) {
        this.remapTransformer = remapTransformer;
        return this;
    }

}
