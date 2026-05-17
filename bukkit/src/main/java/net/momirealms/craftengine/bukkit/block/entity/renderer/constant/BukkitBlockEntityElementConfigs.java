package net.momirealms.craftengine.bukkit.block.entity.renderer.constant;

import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigType;
import net.momirealms.craftengine.core.block.entity.render.element.BlockEntityElementConfigs;
import net.momirealms.craftengine.core.util.Key;

public final class BukkitBlockEntityElementConfigs extends BlockEntityElementConfigs {
    public static final BlockEntityElementConfigType<ItemDisplayBlockEntityElement> ITEM_DISPLAY = register(Key.ce("item_display"), ItemDisplayBlockEntityElementConfig.FACTORY);
    public static final BlockEntityElementConfigType<TextDisplayBlockEntityElement> TEXT_DISPLAY = register(Key.ce("text_display"), TextDisplayBlockEntityElementConfig.FACTORY);
    public static final BlockEntityElementConfigType<ItemBlockEntityElement> ITEM = register(Key.ce("item"), ItemBlockEntityElementConfig.FACTORY);
    public static final BlockEntityElementConfigType<ArmorStandBlockEntityElement> ARMOR_STAND = register(Key.ce("armor_stand"), ArmorStandBlockEntityElementConfig.FACTORY);

    private BukkitBlockEntityElementConfigs() {}

    public static void init() {
    }
}
