package net.momirealms.craftengine.bukkit.compatibility.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public class LuckPermsEventListeners {
    private final JavaPlugin plugin;
    private final LuckPerms luckPerms;
    private final BiConsumer<UUID, Boolean> consumer;
    private final SchedulerAdapter<World> scheduler;
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();

    public LuckPermsEventListeners(JavaPlugin plugin, BiConsumer<UUID, Boolean> consumer, SchedulerAdapter<World> scheduler) {
        this.plugin = plugin;
        this.consumer = consumer;
        this.scheduler = scheduler;
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            this.registerEventListeners();
        } else {
            throw new IllegalStateException("Unable to hook into LuckPerms");
        }
    }

    private void registerEventListeners() {
        EventBus eventBus = this.luckPerms.getEventBus();
        this.subscriptions.add(eventBus.subscribe(this.plugin, UserDataRecalculateEvent.class, this::onUserPermissionChange));
        this.subscriptions.add(eventBus.subscribe(this.plugin, GroupDataRecalculateEvent.class, this::onGroupPermissionChange));
    }

    public void unregisterListeners() {
        this.subscriptions.forEach(subscription -> {
            try {
                subscription.close();
            } catch (Exception e) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to close event subscription", e);
            }
        });
        this.subscriptions.clear();
    }

    private void onUserPermissionChange(UserDataRecalculateEvent event) {
        this.consumer.accept(event.getUser().getUniqueId(), true);
    }

    private void onGroupPermissionChange(GroupDataRecalculateEvent event) {
        this.scheduler.asyncLater(() -> {
            String groupName = event.getGroup().getName();
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID uuid = player.getUniqueId();
                User user = luckPerms.getUserManager().getUser(uuid);
                if (user == null) return;
                boolean inGroup = user.getInheritedGroups(user.getQueryOptions()).stream()
                        .anyMatch(g -> g.getName().equals(groupName));
                if (inGroup) {
                    this.consumer.accept(uuid, false);
                }
            });
        }, 1L, TimeUnit.SECONDS);
    }
}
