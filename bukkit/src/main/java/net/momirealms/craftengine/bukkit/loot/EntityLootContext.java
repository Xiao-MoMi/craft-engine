package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.proxy.minecraft.world.damagesource.DamageSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityLootContext extends BukkitLootContext {
    public final Entity deathEntity;

    public EntityLootContext(
            @NotNull World world, @Nullable Player player, float luck, @NotNull ContextHolder contexts,
            @NotNull Entity deathEntity
    ) {
        super(world, player, luck, contexts);
        this.deathEntity = deathEntity;
    }

    @Override
    protected Object getMinecraftLootParamsBuilder() {
        Object lootParamsBuilder = LootParamsProxy.BuilderProxy.INSTANCE.newInstance(this.world().minecraftWorld());
        Location pos = deathEntity.getLocation();
        Object serverEntity = BukkitAdaptor.adapt(this.deathEntity).serverEntity();
        Object lastDamageSource = LivingEntityProxy.INSTANCE.getLastDamageSource(serverEntity);
        // 必须参数
        LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.THIS_ENTITY, serverEntity);
        LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.ORIGIN, Vec3Proxy.INSTANCE.newInstance(pos.x(), pos.y(), pos.z()));
        LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.DAMAGE_SOURCE, lastDamageSource);
        // 可选参数
        if (LivingEntityProxy.CLASS.isInstance(serverEntity)) {
            Object lastHurtByPlayer = VersionHelper.isOrAbove1_21_5 ?
                    LivingEntityProxy.INSTANCE.getLastHurtByPlayer(serverEntity) :
                    LivingEntityProxy.INSTANCE.getLastHurtByPlayerField(serverEntity);
            LootParamsProxy.BuilderProxy.INSTANCE.withOptionalParameter(lootParamsBuilder, LootContextParamsProxy.LAST_DAMAGE_PLAYER, lastHurtByPlayer);
        }
        if (VersionHelper.isOrAbove1_21_9 && lastDamageSource != null) {
            LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.ATTACKING_ENTITY, DamageSourceProxy.INSTANCE.getCausingEntity(lastDamageSource));
            LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.DIRECT_ATTACKING_ENTITY, DamageSourceProxy.INSTANCE.getDirectEntity(lastDamageSource));
        }
        // 额外参数
        this.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(data -> {
            LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.LAST_DAMAGE_PLAYER, data.serverPlayer());
        });
        this.getOptionalParameter(DirectContextParameters.ITEM_IN_HAND).ifPresent(data -> {
            LootParamsProxy.BuilderProxy.INSTANCE.withParameter(lootParamsBuilder, LootContextParamsProxy.TOOL, data.minecraftItem());
        });
        LootParamsProxy.BuilderProxy.INSTANCE.withLuck(lootParamsBuilder, this.luck());
        return lootParamsBuilder;
    }

}
