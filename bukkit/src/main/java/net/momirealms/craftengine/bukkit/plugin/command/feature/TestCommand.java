package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.parser.BlockStateParser;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.parser.location.LocationParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TestCommand extends BukkitCommandFeature<CommandSender> {

    public TestCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(org.incendo.cloud.CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("start", IntegerParser.integerParser(0))
                .senderType(Player.class)
                .handler(context -> {
                    Player sender = context.sender();
                    int start = context.get("start");
                    int x = sender.getChunk().getX() * 16;
                    int z = sender.getChunk().getZ() * 16;
                    int y = (sender.getLocation().getBlockY() / 16) * 16;
                    for (int a = 0; a < 16; a++) {
                        for (int b = 0; b < 16; b++) {
                            for (int c = 0; c < 16; c++) {
                                BlockData blockData = BlockStateUtils.fromBlockData(BlockStateUtils.idToBlockState(start + a + b * 16 + c * 256));
                                sender.getWorld().setBlockData(new Location(sender.getWorld(), x + a, y + b, z + c), blockData);
                            }
                        }
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_test";
    }
}
