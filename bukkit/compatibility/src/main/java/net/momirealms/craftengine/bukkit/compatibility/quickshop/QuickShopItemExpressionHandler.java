package net.momirealms.craftengine.bukkit.compatibility.quickshop;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.registry.BuiltInRegistry;
import com.ghostchu.quickshop.api.registry.Registry;
import com.ghostchu.quickshop.api.registry.builtin.itemexpression.ItemExpressionHandler;
import com.ghostchu.quickshop.api.registry.builtin.itemexpression.ItemExpressionRegistry;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class QuickShopItemExpressionHandler implements ItemExpressionHandler, Listener {
    private final BukkitCraftEngine plugin;

    public QuickShopItemExpressionHandler(BukkitCraftEngine plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Registry registry = QuickShopAPI.getInstance().getRegistry().getRegistry(BuiltInRegistry.ITEM_EXPRESSION);
        if (!(registry instanceof ItemExpressionRegistry itemExpressionRegistry)) return;
        itemExpressionRegistry.registerHandlerSafely(this);
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return this.plugin.javaPlugin();
    }

    @Override
    public String getPrefix() {
        return "craftengine";
    }

    @Override
    public boolean match(ItemStack itemStack, String id) {
        Key customId = CraftEngineItems.getCustomItemId(itemStack);
        return customId != null && id.equals(customId.asString());
    }
}
