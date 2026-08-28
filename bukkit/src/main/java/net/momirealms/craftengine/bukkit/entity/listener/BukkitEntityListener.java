package net.momirealms.craftengine.bukkit.entity.listener;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntityManager;
import net.momirealms.craftengine.bukkit.plugin.listener.AbstractListener;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.EntityTypeKeys;
import net.momirealms.craftengine.core.entity.LivingEntityHolder;
import net.momirealms.craftengine.core.entity.effect.PotionEffectSnapshot;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.bukkit.event.entity.EntityEventProxy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;

public final class BukkitEntityListener extends AbstractListener {
    private final BukkitEntityManager manager;

    public BukkitEntityListener(BukkitEntityManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        BukkitServerPlayer player = BukkitAdaptor.adapt(event.getPlayer());
        if (player != null && player.isAlive() && Config.shouldTrackEntity(EntityTypeKeys.PLAYER)) {
            this.manager.trackLivingEntity(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (VersionHelper.hasFoliaPatch) {
            return;
        }
        BukkitServerPlayer player = BukkitAdaptor.adapt(event.getPlayer());
        if (player != null && Config.shouldTrackEntity(EntityTypeKeys.PLAYER)) {
            this.manager.trackLivingEntity(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.manager.untrackLivingEntity(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        this.manager.untrackLivingEntity(event.getEntity().getUniqueId(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        LivingEntityHolder holder = this.manager.getEntityHolder(EntityEventProxy.INSTANCE.getEntity(event).getUniqueId());
        if (holder == null || holder.potionEffects.isMutating()) return;
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect != null) {
            holder.potionEffects.observeExternalEffect(new PotionEffectSnapshot(
                    KeyUtils.namespacedKeyToKey(newEffect.getType().getKey()),
                    newEffect.getDuration(),
                    newEffect.getAmplifier(),
                    newEffect.isAmbient(),
                    newEffect.hasParticles(),
                    newEffect.hasIcon()
            ));
        } else if (event.getCause() != EntityPotionEffectEvent.Cause.EXPIRATION) {
            holder.potionEffects.observeExternalClear(KeyUtils.namespacedKeyToKey(event.getModifiedType().getKey()));
        }
    }
}
