package net.momirealms.craftengine.core.plugin.network;

public enum ProtocolVersion {
    UNKNOWN(-1, "Unknown"),
    V1_20(763, "1.20"),
    V1_20_1(763, "1.20.1"),
    V1_20_2(764, "1.20.2"),
    V1_20_3(765, "1.20.3"),
    V1_20_4(765, "1.20.4"),
    V1_20_5(766, "1.20.5"),
    V1_20_6(766, "1.20.6"),
    V1_21(767, "1.21"),
    V1_21_1(767, "1.21.1"),
    V1_21_2(768, "1.21.2"),
    V1_21_3(768, "1.21.3"),
    V1_21_4(769, "1.21.4"),
    V1_21_5(770, "1.21.5"),
    V1_21_6(771, "1.21.6"),
    V1_21_7(772, "1.21.7"),
    V1_21_8(772, "1.21.8"),
    V1_21_9(773, "1.21.9"),
    V1_21_10(773, "1.21.10"),
    V1_21_11(774, "1.21.11"); // todo 确认正式版协议ID

    private final int id;
    private final String name;

    ProtocolVersion(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isVersionNewerThan(ProtocolVersion targetVersion) {
        return this.getId() >= targetVersion.getId();
    }

    public static ProtocolVersion getByName(String name) {
        for (ProtocolVersion version : values()) {
            if (version.getName().equals(name)) {
                return version;
            }
        }
        return UNKNOWN;
    }

    public static ProtocolVersion getById(int id) {
        for (ProtocolVersion version : values()) {
            if (version.getId() == id) {
                return version;
            }
        }
        return UNKNOWN;
    }
}
