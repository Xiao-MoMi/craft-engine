package net.momirealms.craftengine.bedrock.geyser;

import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationAdapter;
import net.momirealms.craftengine.bedrock.geyser.item.GeyserItemRegistrationResult;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostReloadEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinitionRegisterException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

public final class CraftEngineGeyserExtension implements Extension {
    @Subscribe
    public void onDefineCustomBlocks(GeyserDefineCustomBlocksEvent event) {
    }

    @Subscribe
    public void onDefineCustomItems(GeyserDefineCustomItemsEvent event) {
        Path directory = this.geyserApi().packDirectory();
        if (!Files.isDirectory(directory)) {
            this.logger().info("No Geyser packs directory at " + directory + "; skipping custom item registration.");
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".mcpack") || name.endsWith(".zip")) {
                    this.registerPack(path, event);
                }
            }
        } catch (IOException exception) {
            this.logger().error("Failed to scan Geyser packs directory " + directory, exception);
        }
    }

    private void registerPack(Path packPath, GeyserDefineCustomItemsEvent event) {
        ExtensionLogger logger = this.logger();
        GeyserItemRegistrationResult result;
        try {
            result = new GeyserItemRegistrationAdapter().readPack(packPath);
        } catch (IOException | RuntimeException exception) {
            logger.error("Failed to read item registration plan from " + packPath + "; no items from this pack were registered.", exception);
            return;
        }
        if (result == null) {
            return;
        }
        for (GeyserItemRegistrationResult.Diagnostic diagnostic : result.diagnostics()) {
            String message = "Item registration plan: " + diagnostic.message()
                    + " [bedrock=" + diagnostic.bedrockId()
                    + ", model=" + diagnostic.model()
                    + ", file=" + diagnostic.definitionFile()
                    + ", pack=" + packPath + "]";
            switch (diagnostic.severity()) {
                case "ERROR" -> logger.error(message);
                case "WARNING" -> logger.warning(message);
                default -> logger.info(message);
            }
        }
        if (result.hasErrors()) {
            logger.error("Item registration plan in " + packPath + " contains errors; no items from this pack were registered.");
            return;
        }
        int registered = 0;
        for (GeyserItemRegistrationResult.Registration registration : result.registrations()) {
            try {
                event.register(registration.javaItem(), registration.definition());
                registered++;
            } catch (CustomItemDefinitionRegisterException exception) {
                logger.error("Failed to register custom item " + registration.definition().bedrockIdentifier()
                        + " [java=" + registration.javaItem()
                        + ", model=" + registration.definition().model()
                        + ", pack=" + packPath + "]", exception);
            }
        }
        int failed = result.registrations().size() - registered;
        String summary = "Registered " + registered + "/" + result.registrations().size()
                + " custom items from " + packPath + "; failed=" + failed + ".";
        if (failed > 0) {
            logger.warning(summary);
        } else {
            logger.info(summary);
        }
    }

    @Subscribe
    public void onInitialized(GeyserPostInitializeEvent event) {
    }

    @Subscribe
    public void onReloaded(GeyserPostReloadEvent event) {
    }
}
