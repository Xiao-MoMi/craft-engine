package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockLootContext extends BukkitLootContext {
    public final ExistingBlock existingBlock;
    public final Item itemInHand;
    public final Object sourceEntity;

    public BlockLootContext(@NotNull World world, @Nullable Player player, float luck, @NotNull ContextHolder contexts,
                            @NotNull ExistingBlock existingBlock, @Nullable Item itemInHand, @Nullable Object sourceEntity
    ) {
        super(world, player, luck, contexts);
        this.existingBlock = existingBlock;
        this.itemInHand = itemInHand;
        this.sourceEntity = sourceEntity;
    }

    @Override
    protected Object getMinecraftLootParamsBuilder() {
        Object lootParamsBuilder = LootParamsProxy.BuilderProxy.INSTANCE.newInstance(this.world().minecraftWorld());
        // 必须参数
        LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.BLOCK_STATE, existingBlock.blockState().minecraftState());
        LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.ORIGIN, Vec3Proxy.INSTANCE.newInstance(existingBlock.x(), existingBlock.y(), existingBlock.z()));
        LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.TOOL, itemInHand == null ? ItemStackProxy.EMPTY : itemInHand.minecraftItem());
        // 可选参数
        LootParamsProxy.BuilderProxy.INSTANCE.withOptionalParameter(lootParamsBuilder, LootContextParamsProxy.THIS_ENTITY, sourceEntity);
        this.getOptionalParameter(DirectContextParameters.EXPLOSION_RADIUS).ifPresent(data -> {
            LootParamsProxy.BuilderProxy.INSTANCE.withOptionalParameter(lootParamsBuilder, LootContextParamsProxy.EXPLOSION_RADIUS, data);
        });
        LootParamsProxy.BuilderProxy.INSTANCE.withLuck(lootParamsBuilder, this.luck());
        return lootParamsBuilder;
    }
}
