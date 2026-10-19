package net.momirealms.craftengine.bedrock.geyser;

import net.momirealms.craftengine.bedrock.BedrockPackMappings;
import net.momirealms.craftengine.bedrock.BedrockPackMappingsCodec;
import net.momirealms.craftengine.bedrock.BedrockPackLanguages;
import net.momirealms.craftengine.bedrock.BedrockPackLanguagesCodec;
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
    private final Map<String, Map<String, String>> translations = new HashMap<>();
    private final Map<String, String> spears = new HashMap<>();

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
        BedrockPackLanguages languages = BedrockPackLanguages.EMPTY;
        Map<String, String> displayBlocks = Map.of();
        try (ZipFile zip = new ZipFile(path.toFile())) {
            ZipEntry entry = zip.getEntry(BedrockPackMappings.PATH);
            if (entry == null) {
                return;
            }
            try (Reader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                mappings = BedrockPackMappingsCodec.read(reader);
            }
            ZipEntry languageEntry = zip.getEntry(BedrockPackLanguages.PATH);
            if (languageEntry != null) {
                try (Reader reader = new InputStreamReader(zip.getInputStream(languageEntry), StandardCharsets.UTF_8)) {
                    languages = BedrockPackLanguagesCodec.read(reader);
                }
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
        for (Map.Entry<String, Map<String, String>> locale : languages.locales().entrySet()) {
            this.translations.computeIfAbsent(locale.getKey(), ignored -> new HashMap<>()).putAll(locale.getValue());
        }
        this.packs.add(new Pack(path, mappings, displayBlocks, languages.itemNames()));
    }

    Map<String, Map<String, String>> translations() {
        return this.translations;
    }

    Map<String, String> spears() {
        return Map.copyOf(this.spears);
    }

    private Map<String, String> registerBlocks(BedrockPackMappings mappings, GeyserDefineCustomBlocksEvent event) {
        Map<String, String> blocks = new HashMap<>();
        for (BedrockPackMappings.Item item : mappings.items()) {
            this.registerBlock(item.displayBlock(), event, blocks);
        }
        return blocks;
    }

    private void registerBlock(BedrockPackMappings.DisplayBlock display, GeyserDefineCustomBlocksEvent event, Map<String, String> blocks) {
        if (display == null || blocks.containsKey(display.name())) {
            return;
        }
        try {
            blocks.put(display.name(), this.blockItems.registerBlock(display, event));
        } catch (RuntimeException exception) {
            this.logger.error("Could not register CraftEngine display block " + display.name(), exception);
        }
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
                CustomItemDefinition idle = this.definition(pack, item, item.bedrockIdentifier(), item.icon(), item.displayBlock()).build();
                event.register(Identifier.of(item.javaItem()), idle);
                registered++;
            } catch (CustomItemDefinitionRegisterException | IllegalArgumentException | ReflectiveOperationException exception) {
                this.logger.warning("Could not register CraftEngine item " + item.id() + " from " + path.getFileName()
                        + " (" + item.javaItem() + ", " + item.javaItemModel() + "): " + exception.getMessage());
            }
        }
        this.logger.info("Registered " + registered + '/' + mappings.items().size() + " CraftEngine items from " + path.getFileName());
    }

    private CustomItemDefinition.Builder definition(Pack pack, BedrockPackMappings.Item item, String identifier, String icon,
                                                    BedrockPackMappings.DisplayBlock block) throws ReflectiveOperationException {
        CustomItemBedrockOptions.Builder options = CustomItemBedrockOptions.builder()
                .allowOffhand(true)
                .displayHandheld(item.displayHandheld());
        switch (item.javaItem()) {
            case "minecraft:wooden_spear", "minecraft:stone_spear", "minecraft:copper_spear", "minecraft:iron_spear",
                 "minecraft:golden_spear", "minecraft:diamond_spear", "minecraft:netherite_spear" -> {
                options.displayHandheld(true);
                options.tag(Identifier.of("minecraft:is_spear"));
                this.spears.put(identifier, item.javaItem());
            }
            default -> {
            }
        }
        if (block == null) {
            options.icon(icon);
        }
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(Identifier.of(identifier), Identifier.of(item.javaItemModel()))
                .displayName(pack.itemNames().getOrDefault(item.id(), item.id()))
                .bedrockOptions(options);
        if (block != null) {
            String displayBlock = pack.displayBlocks().get(block.name());
            if (displayBlock == null) {
                throw new IllegalArgumentException("Display block was not registered: " + block.name());
            }
            this.blockItems.apply(builder, displayBlock);
        }
        return builder;
    }

    private record Pack(Path path, BedrockPackMappings mappings, Map<String, String> displayBlocks, Map<String, String> itemNames) {
    }
}
