package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

//import net.momirealms.craftengine.core.util.MiscUtils;
//import net.momirealms.craftengine.core.util.ReflectionUtils;
//import net.momirealms.craftengine.core.util.VersionHelper;
//import org.bukkit.event.HandlerList;
//
//import java.lang.reflect.Field;
//import java.util.Optional;

import net.momirealms.craftengine.core.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

// TODO API 太新了需要1.21.8，目前先采用其他方式解决假人问题
public final class LeavesReflections {
    private LeavesReflections() {}

//    public static final Class<?> clazz$BotJoinEvent = MiscUtils.requireNonNullIf(ReflectionUtils.getClazz("org.leavesmc.leaves.event.bot.BotJoinEvent"), VersionHelper.isLeaves());
//
//    public static final Class<?> clazz$BotRemoveEvent = MiscUtils.requireNonNullIf(ReflectionUtils.getClazz("org.leavesmc.leaves.event.bot.BotRemoveEvent"), VersionHelper.isLeaves());
//
//    public static final Class<?> clazz$BotEvent = MiscUtils.requireNonNullIf(ReflectionUtils.getClazz("org.leavesmc.leaves.event.bot.BotEvent"), VersionHelper.isLeaves());
//
//    public static final Class<?> clazz$Bot = MiscUtils.requireNonNullIf(ReflectionUtils.getClazz("org.leavesmc.leaves.entity.bot.Bot"), VersionHelper.isLeaves());
//
//    public static final Field field$BotEvent$bot = Optional.ofNullable(clazz$BotEvent)
//            .map(it -> ReflectionUtils.getDeclaredField(it, clazz$Bot, 0))
//            .orElse(null);
//
//    public static final Field field$BotJoinEvent$handlers = Optional.ofNullable(clazz$BotJoinEvent)
//            .map(it -> ReflectionUtils.getDeclaredField(it, HandlerList.class, 0))
//            .orElse(null);
//
//    public static final Field field$BotRemoveEvent$handlers = Optional.ofNullable(clazz$BotRemoveEvent)
//            .map(it -> ReflectionUtils.getDeclaredField(it, HandlerList.class, 0))
//            .orElse(null);


    public static final Class<?> clazz$ServerBot = ReflectionUtils.getClazz("org.leavesmc.leaves.bot.ServerBot");

    // 注入BotList来实现全版本的监听
    public static final Class<?> clazz$BotList = ReflectionUtils.getClazz("org.leavesmc.leaves.bot.BotList");

    public static final Field field$BotList$INSTANCE = ReflectionUtils.getDeclaredField(clazz$BotList, clazz$BotList, 0);

    public static final Field field$BotList$bots = ReflectionUtils.getDeclaredField(clazz$BotList, List.class, 0);
}
