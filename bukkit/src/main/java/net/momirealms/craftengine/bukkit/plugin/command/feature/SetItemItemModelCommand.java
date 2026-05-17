package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser;

public final class SetItemItemModelCommand extends BukkitCommandFeature<CommandSender> {

    public SetItemItemModelCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .required("value", NamespacedKeyParser.namespacedKeyParser())
                .handler(context -> {
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(context.sender());
                    if (serverPlayer == null) return;
                    Item itemInHand = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                    if (itemInHand.isEmpty()) {
                        return;
                    }

                    String dataValue = KeyUtils.namespacedKeyToKey(context.get("value")).asString();
                    itemInHand.itemModel(dataValue);
                    handleFeedback(context, MessageConstants.COMMAND_ITEM_SET_ITEM_MODEL, Component.text(dataValue));
                });
    }

    @Override
    public String getFeatureID() {
        return "set_item_item_model";
    }

    @Override
    public boolean isAvailable() {
        return VersionHelper.isOrAbove1_21_2;
    }
}
