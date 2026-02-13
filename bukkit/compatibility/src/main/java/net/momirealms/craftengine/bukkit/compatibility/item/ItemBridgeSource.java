package net.momirealms.craftengine.bukkit.compatibility.item;

import cn.gtemc.itembridge.api.Provider;
import cn.gtemc.itembridge.api.context.BuildContext;
import cn.gtemc.itembridge.api.context.ContextKey;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.compatibility.ItemSource;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

public final class ItemBridgeSource implements ItemSource<ItemStack> {
    private final Provider<ItemStack, Player> provider;

    public ItemBridgeSource(Provider<ItemStack, Player> provider) {
        this.provider = provider;
    }

    @Override
    public String plugin() {
        return this.provider.plugin();
    }

    @Nullable
    @Override
    public ItemStack build(String id, ItemBuildContext context) {
        net.momirealms.craftengine.core.entity.player.Player player = context.player();
        Player bukkitPlayer = null;
        if (player != null) {
            bukkitPlayer = (Player) player.platformPlayer();
        }
        return this.provider.buildOrNull(id, bukkitPlayer, adapt(context));
    }

    private static BuildContext adapt(ItemBuildContext context) {
        if (!VersionHelper.IS_RUNNING_IN_DEV) return BuildContext.empty(); // 先不在生产环境启用
        ContextHolder contexts = context.contexts();
        if (contexts.isEmpty()) {
            return BuildContext.empty();
        }
        BuildContext.Builder builder = BuildContext.builder();
        for (Map.Entry<net.momirealms.craftengine.core.plugin.context.ContextKey<?>, Supplier<Object>> entry : contexts.params().entrySet()) {
            Object value = entry.getValue().get();
            if (value == null) {
                continue;
            }
            Class<?> type = value.getClass(); // fixme 这个获取办法并不正确，net.momirealms.craftengine.core.plugin.context.ContextKey 应该在创建的时候记录是什么类型
            @SuppressWarnings("unchecked")
            ContextKey<Object> contextKey = (ContextKey<Object>) ContextKey.of(type, entry.getKey().node());
            with(builder, contextKey, entry.getValue());
        }
        return builder.build();
    }

    private static <T> void with(BuildContext.Builder builder, ContextKey<T> key, Supplier<T> value) {
        builder.with(key, value);
    }

    @Override
    public String id(ItemStack item) {
        return this.provider.idOrNull(item);
    }
}
