package net.momirealms.craftengine.core.pack.revision;

import net.momirealms.craftengine.core.util.MinecraftVersion;

public final class Revisions {
    private Revisions() {}

    public static final Revision SINCE_1_21_6 = Revision.since(MinecraftVersion.V1_21_6);
    public static final Revision SINCE_1_21_2 = Revision.since(MinecraftVersion.V1_21_2);
    public static final Revision SINCE_1_21_9 = Revision.since(MinecraftVersion.V1_21_9);
    public static final Revision SINCE_1_21_11 = Revision.since(MinecraftVersion.V1_21_11);
    public static final Revision SINCE_26_1 = Revision.since(MinecraftVersion.V26_1);
    public static final Revision From_1_21_4_To_1_21_11 = Revision.fromTo(MinecraftVersion.V1_21_4, MinecraftVersion.V1_21_11);
}
