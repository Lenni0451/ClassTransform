package net.lenni0451.classtransform.mappings;

public class MapperConfig {

    public static MapperConfig create() {
        return new MapperConfig();
    }


    protected boolean fillSuperMappings = false;
    protected boolean remapTransformer = false;

    private MapperConfig() {
    }

    public MapperConfig fillSuperMappings(final boolean fillSuperMappings) {
        this.fillSuperMappings = fillSuperMappings;
        return this;
    }

    public MapperConfig remapTransformer(final boolean remapTransformer) {
        this.remapTransformer = remapTransformer;
        return this;
    }

}
