package net.momirealms.craftengine.bukkit.compatibility.item;

import cn.gtemc.itembridge.api.Provider;
import cn.gtemc.itembridge.api.context.BuildContext;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.compatibility.ItemSource;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public class CustomItemProvider implements Provider<ItemStack, Player> {
    private final ItemSource<ItemStack> itemSource;
    private final String pluginName;

    public CustomItemProvider(ItemSource<ItemStack> itemSource) {
        this.itemSource = itemSource;
        this.pluginName = itemSource.plugin().toLowerCase(Locale.ROOT);
    }

    @Override
    public String plugin() {
        return this.pluginName;
    }

    @Override
    public Optional<ItemStack> build(String id, @Nullable Player player, @NotNull BuildContext context) {
        return Optional.ofNullable(this.itemSource.build(id, ItemBuildContext.of(player != null ? BukkitAdaptors.adapt(player) : null)));
    }

    @Nullable
    @Override
    public ItemStack buildOrNull(String id, @Nullable Player player, @NotNull BuildContext context) {
        return this.itemSource.build(id, ItemBuildContext.of(player != null ? BukkitAdaptors.adapt(player) : null));
    }

    @Override
    public Optional<String> id(@NotNull ItemStack item) {
        return Optional.ofNullable(this.itemSource.id(item));
    }

    @Nullable
    @Override
    public String idOrNull(@NotNull ItemStack item) {
        return this.itemSource.id(item);
    }

    @Override
    public boolean is(@NotNull ItemStack item) {
        return this.itemSource.id(item) != null;
    }

    @Override
    public boolean has(@NotNull String id) {
        return false;
    }
}
