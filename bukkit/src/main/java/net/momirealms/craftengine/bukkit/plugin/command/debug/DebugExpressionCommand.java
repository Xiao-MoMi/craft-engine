package net.momirealms.craftengine.bukkit.plugin.command.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.context.expression.Expressions;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;

public final class DebugExpressionCommand extends BukkitCommandFeature<CommandSender> {

    public DebugExpressionCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("expression", StringParser.greedyStringParser())
                .handler(context -> {
                    CommandSender sender = context.sender();
                    BukkitServerPlayer serverPlayer = sender instanceof Player player ? BukkitAdaptor.adapt(player) : null;
                    PlayerOptionalContext ctx = PlayerOptionalContext.of(serverPlayer);
                    String resolved = TextProviders.fromString(context.<String>get("expression")).get(ctx).replace("\\<", "<"); // 与 ExpressionCondition 保持一致
                    Sender ceSender = plugin().senderFactory().wrap(sender);
                    try {
                        double value = Expressions.evaluate(resolved);
                        ceSender.sendMessage(Component.text("Expression: ", NamedTextColor.GRAY)
                                .append(Component.text(resolved, NamedTextColor.WHITE)));
                        ceSender.sendMessage(Component.text("Result: ", NamedTextColor.GRAY)
                                .append(Component.text(Double.toString(value), NamedTextColor.GREEN)));
                    } catch (RuntimeException e) {
                        ceSender.sendMessage(Component.text("Invalid expression: ", NamedTextColor.RED)
                                .append(Component.text(resolved, NamedTextColor.WHITE)));
                        ceSender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_expression";
    }
}
