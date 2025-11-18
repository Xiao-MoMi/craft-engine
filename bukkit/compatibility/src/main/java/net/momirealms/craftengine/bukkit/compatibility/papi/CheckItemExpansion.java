package net.momirealms.craftengine.bukkit.compatibility.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckItemExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "check";
    }

    @Override
    public @NotNull String getAuthor() {
        return "XiaoMoMi";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || !player.isOnline() || params.isEmpty()) { return null; }

        String lower = params.toLowerCase();

        if (lower.startsWith("has_")) {
            return String.valueOf(handleHas(player, params));
        }

        if (lower.startsWith("count_")) {
            return String.valueOf(handleCount(player, params));
        }

        return switch (lower) {
            case "is_custom" -> String.valueOf(CraftEngineItems.isCustomItem(player.getInventory().getItemInMainHand()));
            case "key" -> {
                Key key = CraftEngineItems.getCustomItemId(player.getInventory().getItemInMainHand());
                yield key != null ? key.toString() : null;
            }
            default -> null;
        };
    }

    private record ParsedParam(String itemId, int amount) {}

    private ParsedParam parseParams(String params) {
        int start = params.indexOf('<');
        int end = params.indexOf('>');

        if (start == -1 || end == -1 || end <= start + 1) { return null; }

        String itemId = params.substring(start + 1, end);
        int amount = 1;

        if (end + 1 < params.length() && params.charAt(end + 1) == '_') {
            try {
                amount = Integer.parseInt(params.substring(end + 2));
                amount = Math.max(1, amount);
            } catch (NumberFormatException ignored) {}
        }

        return new ParsedParam(itemId, amount);
    }

    private boolean handleHas(Player player, String params) {
        ParsedParam parsed = parseParams(params);
        if (parsed == null) { return false; }

        return countItem(player, parsed.itemId()) >= parsed.amount();
    }

    private int handleCount(Player player, String params) {
        ParsedParam parsed = parseParams(params);
        if (parsed == null) { return 0; }

        return countItem(player, parsed.itemId());
    }

    private int countItem(Player player, String itemId) {
        CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.from(itemId));
        if (customItem == null) { return 0; }

        ItemStack target = customItem.buildItemStack();

        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.isSimilar(target)) {
                total += item.getAmount();
            }
        }

        return total;
    }
}
