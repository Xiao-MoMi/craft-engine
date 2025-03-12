package net.momirealms.craftengine.core.pack.obfuscation;

/*
    In order to reduce the possibility of being easily reversed,
    we have obfuscated some codes. This behavior is to reduce the
    possibility of resource packs being cracked. Hope you can understand.
 */
@SuppressWarnings({"all"})
public enum ObfA {
    G("sounds", ".ogg"),
    C("blockstates", ".json"),
    A("font", ".json"),
    B("items", ".json"),
    Z("models", ".json"),
    T("textures", ".png");

    private final String cxk;
    private final String dz;

    ObfA(String cxk, String dz) {
        this.cxk = cxk;
        this.dz = dz;
    }

    protected String rkwd() {
        return dz;
    }

    protected String jntm() {
        return cxk;
    }

    protected static ObfA xjjy(String xclf) {
        for (ObfA type : values()) {
            if (type.cxk.equals(xclf)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + xclf);
    }

    public static final byte[] VALUES = new byte[] {
            86, 109, 48, 120, 77, 70, 89, 121, 82, 110, 82, 86, 87, 71, 82, 80, 86, 109, 49, 111, 86, 70, 108, 114, 90, 70, 78, 106, 86, 108, 86, 51, 86, 50, 49, 71, 87, 70, 74, 116, 101, 68, 66, 85, 98, 70, 90, 80, 86, 50, 120, 97, 99, 49, 78, 115, 98, 71, 70, 83, 86, 110, 65, 122, 87, 86, 82, 66, 101, 70, 100, 72, 86, 107, 100, 104, 82, 109, 104, 89, 85, 48, 86, 75, 87, 86, 100, 87, 85, 107, 100, 90, 86, 109, 82, 89, 85, 109, 116, 115, 97, 86, 74, 115, 87, 107, 57, 87, 97, 107, 90, 76, 84, 109, 120, 90, 101, 70, 100, 116, 100, 70, 78, 105, 86, 108, 112, 89, 87, 87, 116, 83, 89, 87, 70, 72, 86, 110, 70, 82, 86, 71, 115, 57
    };
}
