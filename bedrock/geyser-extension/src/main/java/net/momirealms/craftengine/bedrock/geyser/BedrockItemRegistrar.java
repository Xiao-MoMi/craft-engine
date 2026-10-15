package net.momirealms.craftengine.bedrock.geyser;

import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.bedrock.BedrockPackMappingsCodec;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinitionRegisterException;
import org.geysermc.geyser.api.util.Identifier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class BedrockItemRegistrar {
    private final ExtensionLogger logger;
    private final List<Pack> packs = new ArrayList<>();
    private final ExperimentalBlockItemAdapter blockItems = new ExperimentalBlockItemAdapter();

    BedrockItemRegistrar(ExtensionLogger logger) {
        this.logger = logger;
    }

    void load(Path directory, GeyserDefineCustomBlocksEvent event) {
        if (!Files.exists(directory)) {
            this.logger.info("No Geyser packs directory; no CraftEngine items to register");
            return;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = path.getFileName().toString();
                if (name.endsWith(".zip") || name.endsWith(".mcpack")) {
                    this.loadPack(path, event);
                }
            }
        } catch (IOException exception) {
            this.logger.error("Could not scan Geyser packs directory " + directory, exception);
        }
    }

    private void loadPack(Path path, GeyserDefineCustomBlocksEvent event) {
        BedrockPackMappings mappings;
        Map<String, String> displayBlocks = Map.of();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry entry = zip.getEntry(BedrockPackMappings.PATH);
            if (entry == null) {
                return;
            }
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                mappings = BedrockPackMappingsCodec.read(reader);
            }
            if (event != null) {
                displayBlocks = this.registerBlocks(mappings, event);
                if (!displayBlocks.isEmpty()) {
                    this.logger.info("Registered " + displayBlocks.size() + " CraftEngine display blocks from " + path.getFileName());
                }
            }
        } catch (IOException exception) {
            this.logger.error("Could not read CraftEngine mappings from " + path, exception);
            return;
        }
        this.packs.add(new Pack(path, mappings, displayBlocks));
    }

    private Map<String, String> registerBlocks(BedrockPackMappings mappings, GeyserDefineCustomBlocksEvent event) {
        Map<String, String> blocks = new HashMap<>();
        for (BedrockPackMappings.Item item : mappings.items()) {
            BedrockPackMappings.DisplayBlock display = item.displayBlock();
            if (display == null || blocks.containsKey(display.name())) {
                continue;
            }
            try {
                blocks.put(display.name(), this.blockItems.registerBlock(display, event));
            } catch (RuntimeException exception) {
                this.logger.error("Could not register CraftEngine display block " + display.name(), exception);
            }
        }
        return blocks;
    }

    void register(GeyserDefineCustomItemsEvent event) {
        for (Pack pack : this.packs) {
            this.registerPack(pack, event);
        }
        this.packs.clear();
    }

    private void registerPack(Pack pack, GeyserDefineCustomItemsEvent event) {
        Path path = pack.path();
        BedrockPackMappings mappings = pack.mappings();
        int registered = 0;
        for (BedrockPackMappings.Item item : mappings.items()) {
            try {
                CustomItemBedrockOptions.Builder options = CustomItemBedrockOptions.builder().allowOffhand(true);
                if (item.displayBlock() == null) {
                    options.icon(item.icon());
                }
                CustomItemDefinition.Builder builder = CustomItemDefinition.builder(
                                Identifier.of(item.bedrockIdentifier()), Identifier.of(item.javaItemModel()))
                        .displayName(item.id())
                        .bedrockOptions(options);
                if (item.displayBlock() != null) {
                    String displayBlock = pack.displayBlocks().get(item.displayBlock().name());
                    if (displayBlock == null) {
                        this.logger.warning("Skipping CraftEngine item " + item.id() + ": display block was not registered");
                        continue;
                    }
                    this.blockItems.apply(builder, displayBlock);
                }
                event.register(Identifier.of(item.javaItem()), builder.build());
                registered++;
            } catch (CustomItemDefinitionRegisterException | IllegalArgumentException | ReflectiveOperationException exception) {
                this.logger.warning("Could not register CraftEngine item " + item.id() + " from " + path.getFileName()
                        + " (" + item.javaItem() + ", " + item.javaItemModel() + "): " + exception.getMessage());
            }
        }
        this.logger.info("Registered " + registered + '/' + mappings.items().size() + " CraftEngine items from " + path.getFileName());
    }

    private record Pack(Path path, BedrockPackMappings mappings, Map<String, String> displayBlocks) {
    }
}
