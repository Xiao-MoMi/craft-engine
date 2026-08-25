package net.momirealms.craftengine.core.plugin.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import net.momirealms.craftengine.core.plugin.Plugin;
import net.momirealms.craftengine.core.plugin.command.sender.Sender;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.locale.CraftEngineCaptionFormatter;
import net.momirealms.craftengine.core.plugin.locale.CraftEngineCaptionProvider;
import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.util.TriConsumer;
import net.momirealms.craftengine.core.util.YamlUtils;
import net.momirealms.sparrow.yaml.YamlDocument;
import net.momirealms.sparrow.yaml.node.SectionNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.caption.Caption;
import org.incendo.cloud.caption.StandardCaptionKeys;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.exception.*;
import org.incendo.cloud.exception.handling.ExceptionContext;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

public abstract class AbstractCommandManager<C> implements CraftEngineCommandManager<C> {
    protected final HashSet<CommandComponent<C>> registeredRootCommandComponents = new HashSet<>();
    protected final HashSet<CommandFeature<C>> registeredFeatures = new HashSet<>();
    protected final org.incendo.cloud.CommandManager<C> commandManager;
    protected final Plugin plugin;
    private final CraftEngineCaptionFormatter<C> captionFormatter;
    private final MinecraftExceptionHandler.Decorator<C> decorator = (formatter, ctx, msg) -> msg;
    private TriConsumer<C, String, Component> feedbackConsumer;

    public AbstractCommandManager(Plugin plugin, org.incendo.cloud.CommandManager<C> commandManager) {
        this.commandManager = commandManager;
        this.plugin = plugin;
        this.inject();
        this.feedbackConsumer = defaultFeedbackConsumer();
        this.captionFormatter = new CraftEngineCaptionFormatter<>(plugin.translationManager());
    }

    @Override
    public void setFeedbackConsumer(@NotNull TriConsumer<C, String, Component> feedbackConsumer) {
        this.feedbackConsumer = feedbackConsumer;
    }

    @Override
    public TriConsumer<C, String, Component> defaultFeedbackConsumer() {
        return ((sender, node, component) -> {
            wrapSender(sender).sendMessage(
                component, true
            );
        });
    }

    protected abstract Sender wrapSender(C c);

    private void inject() {
        getCommandManager().captionRegistry().registerProvider(new CraftEngineCaptionProvider<>());
        injectExceptionHandler(InvalidSyntaxException.class, MinecraftExceptionHandler.createDefaultInvalidSyntaxHandler(), StandardCaptionKeys.EXCEPTION_INVALID_SYNTAX);
        injectExceptionHandler(InvalidCommandSenderException.class, MinecraftExceptionHandler.createDefaultInvalidSenderHandler(), StandardCaptionKeys.EXCEPTION_INVALID_SENDER);
        injectExceptionHandler(NoPermissionException.class, MinecraftExceptionHandler.createDefaultNoPermissionHandler(), StandardCaptionKeys.EXCEPTION_NO_PERMISSION);
        injectExceptionHandler(ArgumentParseException.class, MinecraftExceptionHandler.createDefaultArgumentParsingHandler(), StandardCaptionKeys.EXCEPTION_INVALID_ARGUMENT);
        injectExceptionHandler(CommandExecutionException.class, MinecraftExceptionHandler.createDefaultCommandExecutionHandler(), StandardCaptionKeys.EXCEPTION_UNEXPECTED);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void injectExceptionHandler(Class<? extends Throwable> type, MinecraftExceptionHandler.MessageFactory<C, ?> factory, Caption key) {
        getCommandManager().exceptionController().registerHandler(type, ctx -> {
            final @Nullable ComponentLike message = factory.message(captionFormatter, (ExceptionContext) ctx);
            if (message != null) {
                handleCommandFeedback(ctx.context().sender(), key.key(), decorator.decorate(captionFormatter, ctx, message.asComponent()).asComponent());
            }
        });
    }

    @Override
    public CommandConfig<C> getCommandConfig(YamlDocument document, String featureID) {
        SectionNode section = YamlUtils.reader(document).getSection(featureID);
        if (section == null) return null;
        YamlUtils.Reader config = YamlUtils.reader(section);
        return new CommandConfig.Builder<C>()
                .permission(config.getString("permission"))
                .usages(config.getStringList("usage"))
                .enable(config.getBoolean("enable", false))
                .build();
    }

    @Override
    public Collection<Command.Builder<C>> buildCommandBuilders(CommandConfig<C> config) {
        ArrayList<Command.Builder<C>> list = new ArrayList<>();
        for (String usage : config.getUsages()) {
            if (!usage.startsWith("/")) continue;
            String command = usage.substring(1).trim();
            String[] split = command.split(" ");
            Command.Builder<C> builder = new ConfigurableCommandBuilder.BasicConfigurableCommandBuilder<>(getCommandManager(), split[0])
                        .nodes(ArrayUtils.subArray(split, 1))
                    .permission(config.getPermission())
                    .build();
            list.add(builder);
        }
        return list;
    }

    @Override
    public void registerFeature(CommandFeature<C> feature, CommandConfig<C> config) {
        if (!config.isEnable()) throw new RuntimeException("Registering a disabled command feature is not allowed");
        for (Command.Builder<C> builder : buildCommandBuilders(config)) {
            Command<C> command = feature.registerCommand(commandManager, builder);
            this.registeredRootCommandComponents.add(command.rootComponent());
        }
        feature.registerRelatedFunctions();
        this.registeredFeatures.add(feature);
        ((AbstractCommandFeature<C>) feature).setCommandConfig(config);
    }

    @Override
    public void registerDefaultFeatures() {
        YamlDocument document = Config.instance().loadYamlConfig(commandsFile,
                Config.yamlUpgradeBuilder()
                    .updateComments(true)
                    .deleteRemovedNodes(true)
                    .build()
        );
        try {
            document.save(this.plugin.dataFolderPath().resolve("commands.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.features().values().forEach(feature -> {
            CommandConfig<C> config = getCommandConfig(document, feature.getFeatureID());
            if (config != null && config.isEnable() && feature.isAvailable()) {
                registerFeature(feature, config);
            }
        });
    }

    @Override
    public void unregisterFeatures() {
        this.registeredRootCommandComponents.forEach(component -> this.commandManager.commandRegistrationHandler().unregisterRootCommand(component));
        this.registeredRootCommandComponents.clear();
        this.registeredFeatures.forEach(CommandFeature::unregisterRelatedFunctions);
        this.registeredFeatures.clear();
    }

    @Override
    public org.incendo.cloud.CommandManager<C> getCommandManager() {
        return commandManager;
    }

    @Override
    public void handleCommandFeedback(C sender, TranslatableComponent.Builder key, Component... args) {
        TranslatableComponent component = key.arguments(args).build();
        this.feedbackConsumer.accept(sender, component.key(), this.plugin.translationManager().render(component, getLocale(sender)));
    }

    @Override
    public void handleCommandFeedback(C sender, String node, Component component) {
        this.feedbackConsumer.accept(sender, node, component);
    }

    protected abstract Locale getLocale(C sender);
}
