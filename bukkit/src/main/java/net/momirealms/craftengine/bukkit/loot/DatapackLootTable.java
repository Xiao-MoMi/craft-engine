package net.momirealms.craftengine.bukkit.loot;

import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.Loot;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.ResourceKeyProxy;
import net.momirealms.craftengine.proxy.minecraft.server.MinecraftServerProxy;
import net.momirealms.craftengine.proxy.minecraft.server.ReloadableServerRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.util.context.ContextKeySetProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootDataResolverProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootParamsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.LootTableProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamSetsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.storage.loot.parameters.LootContextParamsProxy;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DatapackLootTable implements Loot {
    public static final Object ALL_OPTIONAL_PARAMS = DatapackLootTable.createAllOptionalParams();
    public LazyReference<Object> minecraftLootTable;

    public DatapackLootTable(Key identifier) {
        this.minecraftLootTable = LazyReference.lazyReference(() -> {
            Object minecraftServer = MinecraftServerProxy.INSTANCE.getServer();
            // 1.20.5 +
            if (VersionHelper.isOrAbove1_20_5) {
                Object registryKey = ResourceKeyProxy.INSTANCE.create(
                        RegistriesProxy.LOOT_TABLE,
                        IdentifierProxy.INSTANCE.newInstance(identifier.namespace(), identifier.value())
                );
                // 非空, 至少会返回一个 LootTable.EMPTY.
                Object reloadableRegistriesHolder = MinecraftServerProxy.INSTANCE.reloadableRegistries(minecraftServer);
                return ReloadableServerRegistriesProxy.HolderProxy.INSTANCE.getLootTable(reloadableRegistriesHolder, registryKey);
            }
            // 1.20 +
            else {
                Object lootDataManager = MinecraftServerProxy.INSTANCE.getLootData(minecraftServer);
                // 非空, 至少会返回一个 LootTable.EMPTY.
                return LootDataResolverProxy.INSTANCE.getLootTable(
                        lootDataManager,
                        IdentifierProxy.INSTANCE.newInstance(identifier.namespace(), identifier.value())
                );
            }
        });
    }

    @Override
    public List<Item> getRandomItems(LootContext context) {
        ArrayList<Item> list = new ArrayList<>();
        this.getRandomItems(context, list::add);
        return list;
    }

    @Override
    public List<Item> getRandomItems(ContextHolder parameters, World world) {
        return this.getRandomItems(parameters, world, null);
    }

    @Override
    public List<Item> getRandomItems(ContextHolder parameters, World world, @Nullable Player player) {
        return this.getRandomItems(new LootContext(world, player, player == null ? 1f : (float) player.luck(), parameters));
    }

    @Override
    public void getRandomItems(LootContext context, Consumer<Item> lootConsumer) {
        if (context instanceof BukkitLootContext bukkitLootContext) {
            Object minecraftLootParamsBuilder = bukkitLootContext.getMinecraftLootParamsBuilder();
            Object lootParams = LootParamsProxy.BuilderProxy.INSTANCE.create(minecraftLootParamsBuilder, ALL_OPTIONAL_PARAMS);
            Object lootTable = minecraftLootTable.get();
            List<Object> dropItems = LootTableProxy.INSTANCE.getRandomItems(lootTable, lootParams);
            for (int i = 0; i < dropItems.size(); i++) {
                lootConsumer.accept(ItemStackUtils.wrap(dropItems.get(i)));
            }
        }
    }

    public List</*Minecraft ItemStack*/ Object> getRandomItemsByLootParams(Object lootParams) {
        Object lootTable = minecraftLootTable.get();
        return LootTableProxy.INSTANCE.getRandomItems(lootTable, lootParams);
    }

    // 创建一个允许所有参数的 ContextKeySet
    private static Object createAllOptionalParams() {
        return LootContextParamSetsProxy.INSTANCE.register("generic_optional", builder -> {
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.THIS_ENTITY);
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.LAST_DAMAGE_PLAYER);
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.DAMAGE_SOURCE);
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.ORIGIN);
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.BLOCK_STATE);
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.BLOCK_ENTITY);
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.TOOL);
            ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.EXPLOSION_RADIUS);

            if (VersionHelper.isOrAbove1_21) {
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.ATTACKING_ENTITY);
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.DIRECT_ATTACKING_ENTITY);
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.ENCHANTMENT_LEVEL);
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.ENCHANTMENT_ACTIVE);
            }
            // 仅 1.21 版本以下.
            else {
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.KILLER_ENTITY);
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.DIRECT_KILLER_ENTITY);
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.LOOTING_MOD);
            }

            if (VersionHelper.isOrAbove1_21_9) {
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.INTERACTING_ENTITY);
                ContextKeySetProxy.BuilderProxy.INSTANCE.optional(builder, LootContextParamsProxy.TARGET_ENTITY);
            }
        });
    }
}
