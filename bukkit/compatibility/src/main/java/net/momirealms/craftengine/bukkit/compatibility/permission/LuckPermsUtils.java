package net.momirealms.craftengine.bukkit.compatibility.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.momirealms.craftengine.core.plugin.network.NetWorkUser;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LuckPermsUtils {
    private static final LuckPerms API = LuckPermsProvider.get();

    private LuckPermsUtils() {}

    public static boolean hasPermission(NetWorkUser user, String permission) {
        return hasPermission(getUser(user.uuid()), permission);
    }

    private static User getUser(UUID uuid) {
        UserManager userManager = API.getUserManager();
        User user = userManager.getUser(uuid);
        if (user != null) return user;
        CompletableFuture<User> userFuture = userManager.loadUser(uuid);
        return userFuture.join(); // 在玩家加载完成前阻塞
    }

    private static boolean hasPermission(User user, String permission) {
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
}
