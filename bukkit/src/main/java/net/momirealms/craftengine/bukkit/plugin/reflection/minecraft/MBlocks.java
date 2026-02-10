package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;

public final class MBlocks {
    private MBlocks() {}

    public static final Object AIR = getById("air");
    public static final Object AIR$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(AIR);
    public static final Object STONE = getById("stone");
    public static final Object STONE$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(STONE);
    public static final Object FIRE = getById("fire");
    public static final Object SOUL_FIRE = getById("soul_fire");
    public static final Object ICE = getById("ice");
    public static final Object SHORT_GRASS = getById(VersionHelper.isOrAbove1_20_3() ? "short_grass" : "grass");
    public static final Object SHORT_GRASS$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(SHORT_GRASS);
    public static final Object SHULKER_BOX = getById("shulker_box");
    public static final Object COMPOSTER = getById("composter");
    public static final Object SNOW = getById("snow");
    public static final Object WATER = getById("water");
    public static final Object WATER$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(WATER);
    public static final Object TNT = getById("tnt");
    public static final Object TNT$defaultState = BlockProxy.INSTANCE.getDefaultBlockState(TNT);
    public static final Object BARRIER = getById("barrier");
    public static final Object CARVED_PUMPKIN = getById("carved_pumpkin");
    public static final Object JACK_O_LANTERN = getById("jack_o_lantern");
    public static final Object MELON = getById("melon");
    public static final Object PUMPKIN = getById("pumpkin");
    public static final Object FARMLAND = getById("farmland");
    public static final Object LODESTONE = getById("lodestone");
    public static final Object BEDROCK = getById("bedrock");
    public static final Object OBSIDIAN = getById("obsidian");
    public static final Object END_PORTAL_FRAME = getById("end_portal_frame");

    private static Object getById(String path) {
        Object id = IdentifierProxy.INSTANCE.newInstance("minecraft", path);
        return RegistryUtils.getRegistryValue(MBuiltInRegistries.BLOCK, id);
    }
}
