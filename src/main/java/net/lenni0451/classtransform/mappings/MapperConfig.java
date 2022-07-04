package net.lenni0451.classtransform.mappings;

public class MapperConfig {

    public static MapperConfig create() {
        return new MapperConfig();
    }


    protected boolean remapTransformer = false;

    private MapperConfig() {
    }

    public MapperConfig remapTransformer(final boolean remapTransformer) {
        this.remapTransformer = remapTransformer;
        return this;
    }

}
