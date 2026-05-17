package net.momirealms.craftengine.core.plugin.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public final class ProtocolVersion {
    private static final Map<Integer, ProtocolVersion> BY_ID = new Int2ObjectOpenHashMap<>();
    private static final Map<String, ProtocolVersion> BY_NAME = new Object2ObjectOpenHashMap<>();

    public static final ProtocolVersion UNKNOWN = new ProtocolVersion(-1, "Unknown");
    public static final ProtocolVersion V1_20 = new ProtocolVersion(763, "1.20");
    public static final ProtocolVersion V1_20_1 = new ProtocolVersion(763, "1.20.1");
    public static final ProtocolVersion V1_20_2 = new ProtocolVersion(764, "1.20.2");
    public static final ProtocolVersion V1_20_3 = new ProtocolVersion(765, "1.20.3");
    public static final ProtocolVersion V1_20_4 = new ProtocolVersion(765, "1.20.4");
    public static final ProtocolVersion V1_20_5 = new ProtocolVersion(766, "1.20.5");
    public static final ProtocolVersion V1_20_6 = new ProtocolVersion(766, "1.20.6");
    public static final ProtocolVersion V1_21 = new ProtocolVersion(767, "1.21");
    public static final ProtocolVersion V1_21_1 = new ProtocolVersion(767, "1.21.1");
    public static final ProtocolVersion V1_21_2 = new ProtocolVersion(768, "1.21.2");
    public static final ProtocolVersion V1_21_3 = new ProtocolVersion(768, "1.21.3");
    public static final ProtocolVersion V1_21_4 = new ProtocolVersion(769, "1.21.4");
    public static final ProtocolVersion V1_21_5 = new ProtocolVersion(770, "1.21.5");
    public static final ProtocolVersion V1_21_6 = new ProtocolVersion(771, "1.21.6");
    public static final ProtocolVersion V1_21_7 = new ProtocolVersion(772, "1.21.7");
    public static final ProtocolVersion V1_21_8 = new ProtocolVersion(772, "1.21.8");
    public static final ProtocolVersion V1_21_9 = new ProtocolVersion(773, "1.21.9");
    public static final ProtocolVersion V1_21_10 = new ProtocolVersion(773, "1.21.10");
    public static final ProtocolVersion V1_21_11 = new ProtocolVersion(774, "1.21.11");
    public static final ProtocolVersion V26_1 = new ProtocolVersion(775, "26.1");
    public static final ProtocolVersion V26_1_1 = new ProtocolVersion(775, "26.1.1");
    public static final ProtocolVersion V26_1_2 = new ProtocolVersion(775, "26.1.2");
    public static final ProtocolVersion V26_2 = new ProtocolVersion(776, "26.2");

    private final int id;
    private final String name;

    private ProtocolVersion(int id, String name) {
        this.id = id;
        this.name = name;
        BY_ID.put(id, this);
        BY_NAME.put(name, this);
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
        return BY_NAME.getOrDefault(name, UNKNOWN);
    }

    public static ProtocolVersion getById(int id) {
        return BY_ID.getOrDefault(id, UNKNOWN);
    }

    @Override
    public String toString() {
        return "ProtocolVersion{" +
                "id=" + id +
                ", name=" + name +
                '}';
    }
}
