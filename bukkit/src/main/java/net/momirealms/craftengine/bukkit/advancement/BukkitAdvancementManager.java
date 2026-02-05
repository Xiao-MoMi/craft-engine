package net.momirealms.craftengine.bukkit.advancement;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.bukkit.util.ComponentUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.advancement.AbstractAdvancementManager;
import net.momirealms.craftengine.core.advancement.AdvancementType;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.*;

public final class BukkitAdvancementManager extends AbstractAdvancementManager {
    private final BukkitCraftEngine plugin;

    public BukkitAdvancementManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void sendToast(Player player, Item<?> icon, Component message, AdvancementType type) {
        try {
            Object displayInfo = CoreReflections.constructor$DisplayInfo.newInstance(
                    icon.getLiteralObject(),
                    ComponentUtils.adventureToMinecraft(message),  // title
                    CoreReflections.instance$Component$empty, // description
                    VersionHelper.isOrAbove1_20_3() ? Optional.empty() : null, // background
                    CoreReflections.instance$AdvancementType$values[type.ordinal()],
                    true, // show toast
                    false, // announce to chat
                    true // hidden
            );
            if (VersionHelper.isOrAbove1_20_2()) {
                displayInfo = Optional.of(displayInfo);
            }
            Object resourceLocation = KeyUtils.toIdentifier(Key.of("craftengine", "toast"));
            Object criterion = VersionHelper.isOrAbove1_20_2() ?
                    CoreReflections.constructor$Criterion.newInstance(CoreReflections.constructor$ImpossibleTrigger.newInstance(), CoreReflections.constructor$ImpossibleTrigger$TriggerInstance.newInstance()) :
                    CoreReflections.constructor$Criterion.newInstance(CoreReflections.constructor$ImpossibleTrigger$TriggerInstance.newInstance());
            Map<String, Object> criteria = Map.of("impossible", criterion);
            Object advancementProgress = CoreReflections.constructor$AdvancementProgress.newInstance();
            Object advancement;
            if (VersionHelper.isOrAbove1_20_2()) {
                Object advancementRequirements = VersionHelper.isOrAbove1_20_3() ?
                        CoreReflections.constructor$AdvancementRequirements.newInstance(List.of(List.of("impossible"))) :
                        CoreReflections.constructor$AdvancementRequirements.newInstance((Object) new String[][] {{"impossible"}});
                advancement = CoreReflections.constructor$Advancement.newInstance(
                        Optional.empty(),
                        displayInfo,
                        CoreReflections.instance$AdvancementRewards$EMPTY,
                        criteria,
                        advancementRequirements,
                        false
                );
                CoreReflections.method$AdvancementProgress$update.invoke(advancementProgress, advancementRequirements);
                advancement = CoreReflections.constructor$AdvancementHolder.newInstance(resourceLocation, advancement);
            } else {
                advancement = CoreReflections.constructor$Advancement.newInstance(
                        resourceLocation,
                        null, // parent
                        displayInfo,
                        CoreReflections.instance$AdvancementRewards$EMPTY,
                        criteria,
                        new String[][] {{"impossible"}},
                        false
                );
                CoreReflections.method$AdvancementProgress$update.invoke(advancementProgress, criteria, new String[][] {{"impossible"}});
            }
            CoreReflections.method$AdvancementProgress$grantProgress.invoke(advancementProgress, "impossible");
            Map<Object, Object> advancementsToGrant = new HashMap<>();
            advancementsToGrant.put(resourceLocation, advancementProgress);
            Object grantPacket = VersionHelper.isOrAbove1_21_5() ?
                    NetworkReflections.constructor$ClientboundUpdateAdvancementsPacket.newInstance(false, Arrays.asList(advancement), new HashSet<>(), advancementsToGrant, true) :
                    NetworkReflections.constructor$ClientboundUpdateAdvancementsPacket.newInstance(false, Arrays.asList(advancement), new HashSet<>(), advancementsToGrant);
            Object removePacket = VersionHelper.isOrAbove1_21_5() ?
                    NetworkReflections.constructor$ClientboundUpdateAdvancementsPacket.newInstance(false, new ArrayList<>(), MiscUtils.init(new HashSet<>(), s -> s.add(resourceLocation)), new HashMap<>(), true) :
                    NetworkReflections.constructor$ClientboundUpdateAdvancementsPacket.newInstance(false, new ArrayList<>(), MiscUtils.init(new HashSet<>(), s -> s.add(resourceLocation)), new HashMap<>());
            player.sendPackets(List.of(grantPacket, removePacket), false);
        } catch (ReflectiveOperationException e) {
            this.plugin.logger().warn("Failed to send toast for player " + player.name(), e);
        }
    }
}
