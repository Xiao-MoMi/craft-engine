package net.momirealms.craftengine.bukkit.advancement;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
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
import net.momirealms.craftengine.proxy.minecraft.advancements.*;
import net.momirealms.craftengine.proxy.minecraft.advancements.criterion.ImpossibleTriggerProxy;
import net.momirealms.craftengine.proxy.minecraft.network.chat.ComponentProxy;

import java.util.*;

public final class BukkitAdvancementManager extends AbstractAdvancementManager {
    private final BukkitCraftEngine plugin;

    public BukkitAdvancementManager(BukkitCraftEngine plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendToast(Player player, Item<?> icon, Component message, AdvancementType type) {
        try {
            Object displayInfo;
            if (VersionHelper.isOrAbove1_20_3()) {
                displayInfo = DisplayInfoProxy.INSTANCE.newInstance(
                        icon.getLiteralObject(),
                        ComponentUtils.adventureToMinecraft(message),  // title
                        ComponentProxy.INSTANCE.empty(), // description
                        Optional.empty(), // background
                        AdvancementTypeProxy.VALUES[type.ordinal()],
                        true, // show toast
                        false, // announce to chat
                        true // hidden
                );
            } else {
                displayInfo = DisplayInfoProxy.INSTANCE.newInstance$legacy(
                        icon.getLiteralObject(),
                        ComponentUtils.adventureToMinecraft(message),  // title
                        ComponentProxy.INSTANCE.empty(), // description
                        null, // background
                        AdvancementTypeProxy.VALUES[type.ordinal()],
                        true, // show toast
                        false, // announce to chat
                        true // hidden
                );
            }
            if (VersionHelper.isOrAbove1_20_2()) {
                displayInfo = Optional.of(displayInfo);
            }
            Object resourceLocation = KeyUtils.toIdentifier(Key.of("craftengine", "toast"));
            Object criterion;
            if (VersionHelper.isOrAbove1_20_2()) {
                criterion = CriterionProxy.INSTANCE.newInstance(ImpossibleTriggerProxy.INSTANCE.newInstance(), ImpossibleTriggerProxy.TriggerInstanceProxy.INSTANCE.newInstance());
            } else {
                criterion = CriterionProxy.INSTANCE.newInstance(ImpossibleTriggerProxy.TriggerInstanceProxy.INSTANCE.newInstance());
            }
            Map<String, Object> criteria = Map.of("impossible", criterion);
            Object advancementProgress = AdvancementProgressProxy.INSTANCE.newInstance();
            Object advancement;
            if (VersionHelper.isOrAbove1_20_2()) {
                Object advancementRequirements = VersionHelper.isOrAbove1_20_3() ?
                        AdvancementRequirementsProxy.INSTANCE.newInstance(List.of(List.of("impossible"))) :
                        AdvancementRequirementsProxy.INSTANCE.newInstance(new String[][] {{"impossible"}});
                advancement = AdvancementProxy.INSTANCE.newInstance(
                        Optional.empty(),
                        (Optional<Object>) displayInfo,
                        AdvancementRewardsProxy.EMPTY,
                        criteria,
                        advancementRequirements,
                        false
                );
                AdvancementProgressProxy.INSTANCE.update(advancementProgress, advancementRequirements);
                advancement = AdvancementHolderProxy.INSTANCE.newInstance(resourceLocation, advancement);
            } else {
                advancement = AdvancementProxy.INSTANCE.newInstance(
                        resourceLocation,
                        null, // parent
                        displayInfo,
                        AdvancementRewardsProxy.EMPTY,
                        criteria,
                        new String[][] {{"impossible"}},
                        false
                );
                AdvancementProgressProxy.INSTANCE.update(advancementProgress, criteria, new String[][] {{"impossible"}});
            }
            AdvancementProgressProxy.INSTANCE.grantProgress(advancementProgress, "impossible");
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
