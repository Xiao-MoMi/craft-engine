package net.momirealms.craftengine.bedrock.geyser.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphs;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.SidebarProfiles;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserLanguageData;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserTranslations;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.KIND;

public final class GeyserExtensionData {
    public static final GeyserExtensionData EMPTY = new GeyserExtensionData(null, null, GeyserGlyphData.EMPTY, GeyserLanguageData.EMPTY, SidebarProfiles.EMPTY, null, null, null, null);
    private final JsonObject bossBars;
    private final SidebarProfiles scoreboard;
    private final JsonObject items;
    private final boolean convertText;
    private final JsonObject blocks;
    private final GeyserGlyphData glyphs;
    private final GeyserLanguageData languages;
    private final JsonObject nativeModels;
    private final JsonObject soundMappings;
    private final Path clientPack;

    private GeyserExtensionData(@Nullable JsonObject items, @Nullable JsonObject blocks, GeyserGlyphData glyphs, GeyserLanguageData languages, SidebarProfiles scoreboard,
                               @Nullable JsonObject nativeModels, @Nullable JsonObject soundMappings, @Nullable JsonObject bossBars, @Nullable Path clientPack) {
        this.bossBars = bossBars;
        this.soundMappings = soundMappings;
        this.clientPack = clientPack;
        this.scoreboard = scoreboard;
        this.nativeModels = nativeModels;
        this.items = items;
        JsonObject displays = items == null ? null : items.getAsJsonObject("item_displays");
        this.convertText = displays != null && displays.has("convert_text") && displays.get("convert_text").getAsBoolean();
        this.blocks = blocks;
        this.glyphs = glyphs;
        this.languages = languages;
    }

    public static GeyserExtensionData read(Path path) throws IOException {
        return read(path, null);
    }

    public static GeyserExtensionData read(Path path, @Nullable Path cacheDirectory) throws IOException {
        if (Files.notExists(path)) {
            return EMPTY;
        }
        try (ZipFile archive = new ZipFile(path.toFile())) {
            JsonObject items = readPlan(archive, RegistrationPlanKind.ITEM, "craftengine/item_registration_plan.json");
            JsonObject blocks = readPlan(archive, RegistrationPlanKind.BLOCK, "craftengine/block_registration_plan.json");
            JsonObject sounds = readJson(archive, findEntry(archive, BedrockPackLayout.SOUND_MAPPINGS, "craftengine/sound_mappings.json"));
            GeyserGlyphData glyphs = GeyserGlyphs.parse(readJson(archive, BedrockPackLayout.GLYPHS));
            GeyserLanguageData languages = GeyserTranslations.parse(readJson(archive, BedrockPackLayout.TRANSLATIONS));
            return new GeyserExtensionData(items, blocks, glyphs, languages, SidebarProfiles.parse(readJson(archive, BedrockPackLayout.SCOREBOARD)),
                    readJson(archive, BedrockPackLayout.NATIVE_MODELS), sounds,
                    readJson(archive, BedrockPackLayout.BOSS_BARS),
                    cacheDirectory == null ? null : extractClientPack(archive, cacheDirectory));
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Failed to load extension data from " + path, exception);
        }
    }

    @Nullable
    private static Path extractClientPack(ZipFile archive, Path cacheDirectory) throws IOException {
        ZipEntry entry = archive.getEntry(BedrockPackFiles.CLIENT_PACK);
        if (entry == null) {
            return null;
        }
        Files.createDirectories(cacheDirectory);
        Path pack = cacheDirectory.resolve(BedrockPackFiles.CLIENT_PACK);
        Path temporary = Files.createTempFile(cacheDirectory, "client-", ".tmp");
        try {
            try (InputStream input = archive.getInputStream(entry)) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            return Files.move(temporary, pack, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Nullable
    public JsonObject bossBars() {
        return this.bossBars;
    }

    @Nullable
    public Path clientPack() {
        return this.clientPack;
    }

    @Nullable
    private static JsonObject readPlan(ZipFile archive, RegistrationPlanKind kind, String legacyName) throws IOException {
        ZipEntry entry = findEntry(archive, kind.file(), legacyName);
        if (entry == null) {
            return null;
        }
        JsonObject plan = readJson(archive, entry);
        if (!plan.get(KIND).getAsString().equals(kind.id()) || plan.get(FORMAT_VERSION).getAsInt() != kind.formatVersion()) {
            throw new IOException("Unsupported registration plan format: " + entry.getName());
        }
        return plan;
    }

    @Nullable
    private static ZipEntry findEntry(ZipFile archive, String name, String legacyName) {
        ZipEntry entry = archive.getEntry(name);
        return entry != null ? entry : archive.getEntry(legacyName);
    }

    @Nullable
    private static JsonObject readJson(ZipFile archive, String name) throws IOException {
        return readJson(archive, archive.getEntry(name));
    }

    @Nullable
    private static JsonObject readJson(ZipFile archive, @Nullable ZipEntry entry) throws IOException {
        if (entry == null) {
            return null;
        }
        try (Reader reader = new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid extension data entry: " + entry.getName(), exception);
        }
    }

    @Nullable
    public JsonObject items() {
        return this.items;
    }

    @Nullable
    public JsonObject blocks() {
        return this.blocks;
    }

    public boolean convertText() {
        return this.convertText;
    }

    public SidebarProfiles scoreboard() {
        return this.scoreboard;
    }

    public GeyserGlyphData glyphs() {
        return this.glyphs;
    }

    public GeyserLanguageData languages() {
        return this.languages;
    }

    public JsonObject nativeModels() {
        return this.nativeModels;
    }

    @Nullable
    public JsonObject soundMappings() {
        return this.soundMappings;
    }
}
