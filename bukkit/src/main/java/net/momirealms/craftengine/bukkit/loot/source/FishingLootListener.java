package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItem;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.ItemStackUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.component.value.Enchantment;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.LootManager;
import net.momirealms.craftengine.core.loot.source.LootOutcome;
import net.momirealms.craftengine.core.loot.source.LootSource;
import net.momirealms.craftengine.core.loot.source.LootSources;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.List;

public final class FishingLootListener implements Listener {
    private static final Key LUCK_OF_SEA = Key.minecraft("luck_of_the_sea");

    private static Vector fishingVelocity(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return new Vector(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        List<LootSource> sources = LootSources.FISHING.getSources(null);
        if (sources.isEmpty()) return;
        Player player = event.getPlayer();
        Location hookLocation = event.getHook().getLocation();
        World world = BukkitAdaptor.adapt(hookLocation.getWorld());
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;

        InteractionHand hand = event.getHand() == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        BukkitItem itemInHand = serverPlayer.getItemInHand(hand);
        float luck = (float) serverPlayer.luck() + itemInHand.getEnchantment(LUCK_OF_SEA).map(Enchantment::level).orElse(0);
        ContextHolder holder = ContextHolder.builder()
                .withOptionalParameter(DirectContextParameters.THIS_ENTITY, event.getCaught() == null ? null : BukkitAdaptor.adapt(event.getCaught()))
                .withParameter(DirectContextParameters.ENTITY, serverPlayer)
                .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(hookLocation))
                .withParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand)
                .withParameter(DirectContextParameters.OPEN_WATER, event.getHook().isInOpenWater())
                .build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, serverPlayer, luck, holder));
        if (!outcome.matched()) return;
        List<Item> items = outcome.items();
        if (event.getCaught() instanceof org.bukkit.entity.Item caught && outcome.overwriteItems() && !items.isEmpty()) {
            caught.setItemStack(ItemStackUtils.getBukkitStack(items.removeFirst()));
        }
        if (!items.isEmpty()) {
            Vector velocity = fishingVelocity(hookLocation, player.getLocation());
            for (Item extra : items) {
                org.bukkit.entity.Item drop = hookLocation.getWorld().dropItem(hookLocation, ItemStackUtils.getBukkitStack(extra));
                drop.setVelocity(velocity);
            }
        }
        if (outcome.overwriteExperience()) {
            event.setExpToDrop(0);
        }
    }
}
