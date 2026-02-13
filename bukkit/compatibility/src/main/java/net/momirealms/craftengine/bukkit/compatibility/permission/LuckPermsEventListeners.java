package net.momirealms.craftengine.bukkit.compatibility.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class LuckPermsEventListeners {
    private final JavaPlugin plugin;
    private final LuckPerms luckPerms;
    private final Consumer<UUID> playerCallback;
    private final List<EventSubscription<?>> subscriptions = new ArrayList<>();

    public LuckPermsEventListeners(JavaPlugin plugin, Consumer<UUID> playerCallback) {
        this.plugin = plugin;
        this.playerCallback = playerCallback;
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = provider.getProvider();
            this.registerEventListeners();
        } else {
            throw new IllegalStateException("Unable to hook LuckPerms");
        }
    }

    private void registerEventListeners() {
        EventBus eventBus = this.luckPerms.getEventBus();
        this.subscriptions.add(eventBus.subscribe(this.plugin, UserDataRecalculateEvent.class, this::onUserPermissionChange));
        this.subscriptions.add(eventBus.subscribe(this.plugin, GroupDataRecalculateEvent.class, this::onGroupPermissionChange));
    }

//    public void unregisterListeners() {
//        this.subscriptions.forEach(subscription -> {
//            try {
//                subscription.close();
//            } catch (Exception e) {
//                this.plugin.getLogger().log(Level.WARNING, "Failed to close event subscription", e);
//            }
//        });
//        this.subscriptions.clear();
//    }

    private void onUserPermissionChange(UserDataRecalculateEvent event) {
        CraftEngine.instance().scheduler().async().execute(() -> this.playerCallback.accept(event.getUser().getUniqueId()));
    }

    private void onGroupPermissionChange(GroupDataRecalculateEvent event) {
        CraftEngine.instance().scheduler().asyncLater(() -> {
            String groupName = event.getGroup().getName();
            Bukkit.getOnlinePlayers().forEach(player -> {
                UUID uuid = player.getUniqueId();
                User user = luckPerms.getUserManager().getUser(uuid);
                if (user == null) return;
                boolean inGroup = user.getInheritedGroups(user.getQueryOptions()).stream()
                        .anyMatch(g -> g.getName().equals(groupName));
                if (inGroup) {
                    this.playerCallback.accept(uuid);
                }
            });
        }, 1L, TimeUnit.SECONDS);
    }
}
