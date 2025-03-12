package net.momirealms.craftengine.core.util;

public enum Instrument {
    HARP("harp"),
    BASEDRUM("basedrum"),
    SNARE("snare"),
    HAT("hat"),
    BASS("bass"),
    FLUTE("flute"),
    BELL("bell"),
    GUITAR("guitar"),
    CHIME("chime"),
    XYLOPHONE("xylophone"),
    IRON_XYLOPHONE("iron_xylophone"),
    COW_BELL("cow_bell"),
    DIDGERIDOO("didgeridoo"),
    BIT("bit"),
    BANJO("banjo"),
    PLING("pling"),
    ZOMBIE("zombie"),
    SKELETON("skeleton"),
    CREEPER("creeper"),
    DRAGON("dragon"),
    WITHER_SKELETON("wither_skeleton"),
    PIGLIN("piglin"),
    CUSTOM_HEAD("custom_head");

    private final String id;

    Instrument(final String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
