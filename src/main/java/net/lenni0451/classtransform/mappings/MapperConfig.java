package net.lenni0451.classtransform.mappings;

import net.lenni0451.classtransform.utils.FailStrategy;

public class MapperConfig {

    public static MapperConfig create() {
        return new MapperConfig();
    }


    protected boolean fillSuperMappings = false;
    protected FailStrategy superMappingsFailStrategy;
    protected boolean remapTransformer = false;

    private MapperConfig() {
    }

    public MapperConfig fillSuperMappings(final boolean fillSuperMappings) {
        return this.fillSuperMappings(fillSuperMappings, FailStrategy.CONTINUE);
    }

    public MapperConfig fillSuperMappings(final boolean fillSuperMappings, final FailStrategy superMappingsFailStrategy) {
        this.fillSuperMappings = fillSuperMappings;
        this.superMappingsFailStrategy = superMappingsFailStrategy;
        return this;
    }

    public MapperConfig remapTransformer(final boolean remapTransformer) {
        this.remapTransformer = remapTransformer;
        return this;
    }

}
