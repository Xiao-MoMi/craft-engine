package net.momirealms.craftengine.bukkit.plugin.command;

import net.kyori.adventure.util.Index;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.feature.*;
import net.momirealms.craftengine.core.plugin.command.AbstractCommandManager;
import net.momirealms.craftengine.core.plugin.command.CommandFeature;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.setting.ManagerSetting;

import java.util.List;

public class BukkitCommandManager extends AbstractCommandManager<CommandSender> {
    private final BukkitCraftEngine plugin;
    private final Index<String, CommandFeature<CommandSender>> index;

    public BukkitCommandManager(BukkitCraftEngine plugin) {
        super(plugin, new LegacyPaperCommandManager<>(
                plugin.javaPlugin(),
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        ));
        this.plugin = plugin;
        this.index = Index.create(CommandFeature::getFeatureID, List.of(
                new ReloadCommand(this, plugin),
                new GetItemCommand(this, plugin),
                new GiveItemCommand(this, plugin),
                new ItemBrowserPlayerCommand(this, plugin),
                new ItemBrowserAdminCommand(this, plugin),
                new SearchRecipePlayerCommand(this, plugin),
                new SearchUsagePlayerCommand(this, plugin),
                new SearchRecipeAdminCommand(this, plugin),
                new SearchUsageAdminCommand(this, plugin),
                new TestCommand(this, plugin),
                new DebugGetBlockStateRegistryIdCommand(this, plugin),
                new DebugGetBlockInternalIdCommand(this, plugin),
                new DebugAppearanceStateUsageCommand(this, plugin),
                new DebugClearCooldownCommand(this, plugin),
                new DebugEntityIdCommand(this, plugin),
                new DebugRealStateUsageCommand(this, plugin),
                new DebugItemDataCommand(this, plugin),
                new DebugSetBlockCommand(this, plugin),
                new DebugSpawnFurnitureCommand(this, plugin),
                new DebugTargetBlockCommand(this, plugin),
                new DebugIsSectionInjectedCommand(this, plugin),
                new DebugMigrateTemplatesCommand(this, plugin),
                new DebugIsChunkPersistentLoadedCommand(this, plugin),
                new TotemAnimationCommand(this, plugin),
                new EnableResourceCommand(this, plugin),
                new DisableResourceCommand(this, plugin),
                new ListResourceCommand(this, plugin),
                new UploadPackCommand(this, plugin),
                new SendResourcePackCommand(this, plugin)
        ));
        final LegacyPaperCommandManager<CommandSender> manager = (LegacyPaperCommandManager<CommandSender>) getCommandManager();
        manager.settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true);
        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
            manager.brigadierManager().setNativeNumberSuggestions(true);
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }
    }

    @Override
    protected Sender wrapSender(CommandSender sender) {
        return this.plugin.senderFactory().wrap(sender);
    }

    @Override
    public Index<String, CommandFeature<CommandSender>> features() {
        return this.index;
    }
}
