package net.momirealms.craftengine.bukkit.loot.source;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.item.Item;
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
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.List;

public final class AdvancementLootListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Key advancementId = KeyUtils.namespacedKeyToKey(event.getAdvancement().getKey());
        List<LootSource> sources = LootSources.ADVANCEMENT.getSources(advancementId);
        if (sources.isEmpty()) return;
        Player player = event.getPlayer();
        Location location = player.getLocation();
        World world = BukkitAdaptor.adapt(location.getWorld());
        BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
        if (serverPlayer == null) return;
        ContextHolder holder = ContextHolder.builder()
                .withParameter(DirectContextParameters.ENTITY, serverPlayer)
                .withParameter(DirectContextParameters.PLAYER, serverPlayer)
                .withParameter(DirectContextParameters.WORLD, world)
                .withParameter(DirectContextParameters.POSITION, LocationUtils.toWorldPosition(location))
                .withParameter(DirectContextParameters.ID, advancementId)
                .build();
        LootOutcome outcome = LootManager.eval(sources, new LootContext(world, serverPlayer, (float) serverPlayer.luck(), holder));
        if (!outcome.matched()) return;
        for (Item item : outcome.items()) {
            serverPlayer.giveItem(item, false);
        }
    }
}
