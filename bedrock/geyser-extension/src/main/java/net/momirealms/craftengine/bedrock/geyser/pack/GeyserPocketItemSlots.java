package net.momirealms.craftengine.bedrock.geyser.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.BedrockPlayerPack;
import net.momirealms.craftengine.bedrock.geyser.proxy.registry.type.ItemMappingsProxy;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.Registry;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class GeyserPocketItemSlots {
    private static final String ICON = "pack_icon.png";

    private GeyserPocketItemSlots() {
    }

    @SuppressWarnings("unchecked")
    public static Map<ItemMappings, ResourcePack> createPacks(Path mainPack, Predicate<String> usesBlockIcon, BedrockPlayerPack metadata) throws IOException {
        JsonObject screens;
        try (InputStream input = GeyserPocketItemSlots.class.getResourceAsStream("/pocket_item_slots.json")) {
            if (input == null) {
                throw new IOException("Missing pocket_item_slots.json");
            }
            screens = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        Map<ItemMappings, ResourcePack> packs = new IdentityHashMap<>();
        byte[] icon = null;
        try (ZipFile archive = new ZipFile(mainPack.toFile())) {
            ZipEntry entry = archive.getEntry(ICON);
            if (entry != null) {
                try (InputStream input = archive.getInputStream(entry)) {
                    icon = input.readAllBytes();
                }
            }
        }
        String iconHash = icon == null ? "" : HexFormat.of().formatHex(BedrockPackFiles.sha256().digest(icon));
        // Geyser relocates fastutil on Spigot. Access both registries through java.util.Map.
        Map<Integer, ItemMappings> palettes = (Map<Integer, ItemMappings>) ((Registry<?>) Registries.ITEMS).get();
        for (Map.Entry<Integer, ItemMappings> palette : palettes.entrySet()) {
            ItemMappings mappings = palette.getValue();
            BitSet ids = new BitSet();
            Map<?, ItemDefinition> definitions = (Map<?, ItemDefinition>) ItemMappingsProxy.INSTANCE.itemDefinitions(mappings);
            for (ItemDefinition item : definitions.values()) {
                if (usesBlockIcon.test(item.getIdentifier())) {
                    ids.set(item.getRuntimeId());
                }
            }
            if (!ids.isEmpty()) {
                Path path = mainPack.resolveSibling("pocket-slots-" + palette.getKey() + ".mcpack");
                writePack(path, screens, ids, metadata, icon, iconHash);
                packs.put(mappings, ResourcePack.create(PackCodec.path(path)));
            }
        }
        return packs;
    }

    private static void writePack(Path path, JsonObject screens, BitSet ids, BedrockPlayerPack metadata, byte[] icon, String iconHash) throws IOException {
        int first = ids.nextSetBit(0);
        int end = ids.length();
        StringBuilder mask = new StringBuilder(end - first);
        // JSON UI can read a character by formatting a prefix whose length is the relative runtime ID plus one.
        for (int id = first; id < end; id++) {
            mask.append(ids.get(id) ? '1' : '0');
        }
        JsonObject control = screens.getAsJsonObject("ui/ui_common.json").getAsJsonObject("ce_pocket_block_slot");
        JsonObject lookup = new JsonObject();
        lookup.addProperty("#ce_first", first);
        lookup.addProperty("#ce_end", end);
        lookup.addProperty("#ce_mask", mask.toString());
        control.getAsJsonArray("controls").get(0).getAsJsonObject().getAsJsonObject("margin").add("property_bag", lookup);
        JsonObject manifest = manifest(metadata, screens, iconHash);
        Path temporary = Files.createTempFile(path.getParent(), "pocket-slots-", ".tmp");
        try {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                writeEntry(zip, "manifest.json", manifest.toString().getBytes(StandardCharsets.UTF_8));
                if (icon != null) {
                    writeEntry(zip, ICON, icon);
                }
                for (Map.Entry<String, JsonElement> file : screens.entrySet()) {
                    writeEntry(zip, file.getKey(), file.getValue().toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] contents) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(contents);
        zip.closeEntry();
    }

    private static JsonObject manifest(BedrockPlayerPack metadata, JsonObject screens, String iconHash) {
        JsonObject manifest = JsonParser.parseString("""
                {"format_version":2,"header":{
                "version":[1,0,0],"min_engine_version":[1,21,0]},"modules":[{"type":"resources","version":[1,0,0]}]}
                """).getAsJsonObject();
        JsonObject header = manifest.getAsJsonObject("header");
        header.addProperty("name", metadata.name());
        header.addProperty("description", metadata.description());
        UUID uuid = UUID.nameUUIDFromBytes((manifest.toString() + screens + iconHash).getBytes(StandardCharsets.UTF_8));
        header.addProperty("uuid", uuid.toString());
        UUID module = UUID.nameUUIDFromBytes(("module/" + uuid).getBytes(StandardCharsets.US_ASCII));
        JsonArray modules = manifest.getAsJsonArray("modules");
        modules.get(0).getAsJsonObject().addProperty("uuid", module.toString());
        return manifest;
    }
}
