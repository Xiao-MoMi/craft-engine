package net.momirealms.craftengine.bukkit.compatibility.axiom;

import com.moulberry.axiom.paperapi.AxiomAlreadyRegisteredException;
import com.moulberry.axiom.paperapi.AxiomCustomDisplayAPI;
import com.moulberry.axiom.paperapi.display.AxiomCustomDisplayBuilder;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class AxiomCraftEngineDisplay {
    private final BukkitCraftEngine plugin;
    private final BukkitItemManager itemManager;
    private final AxiomCustomDisplayAPI api = AxiomCustomDisplayAPI.getAPI();

    public AxiomCraftEngineDisplay(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.itemManager();
    }

    public void registerAllItems() {
        this.api.unregisterAll(this.plugin.javaPlugin());
        List<Key> ids = this.itemManager.orderedItemIds();
        if (ids.isEmpty()) return;
        for (Key id : ids) {
            if (this.itemManager.isVanillaItem(id)) continue;
            ItemDefinition definition = this.itemManager.getItemDefinition(id).orElse(null);
            if (definition == null) continue;
            this.registerItem(definition);
        }
    }

    private void registerItem(ItemDefinition definition) {
        Item item;
        try {
            item = definition.buildItem(ItemBuildContext.empty());
        } catch (Throwable t) {
            return;
        }
        try {
            item = item.toClientSide(null);
        } catch (Throwable ignored) {}
        if (item == null || item.isEmpty()) return;
        Key id = definition.id();
        Object key = KeyUtils.toPaperAdventureKey(id);
        String searchKey = definition.translationKey();
        ItemStack itemStack = (ItemStack) item.platformItem();
        AxiomCustomDisplayBuilder builder = AxiomCustomDisplayAPIProxy.INSTANCE.create(this.api, key, searchKey, itemStack);
        try {
            this.api.register(this.plugin.javaPlugin(), builder);
        } catch (AxiomAlreadyRegisteredException e) {
            this.plugin.logger().warn("Item " + id + " is already registered, skipping.");
        }
    }
}
