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
    public static final GeyserExtensionData EMPTY = new GeyserExtensionData(
            null, null, null, null,
            GeyserGlyphData.EMPTY, GeyserLanguageData.EMPTY, SidebarProfiles.EMPTY,
            null, null, null, null
    );
    private final JsonObject bossBars;
    private final SidebarProfiles scoreboard;
    private final JsonObject items;
    private final JsonObject blocks;
    private final JsonObject modelDisplays;
    private final JsonObject elytraVariants;
    private final GeyserGlyphData glyphs;
    private final GeyserLanguageData languages;
    private final JsonObject nativeModels;
    private final JsonObject soundMappings;
    private final Path clientPack;

    private GeyserExtensionData(@Nullable JsonObject items,
                                @Nullable JsonObject blocks,
                                @Nullable JsonObject modelDisplays,
                                @Nullable JsonObject elytraVariants,
                                GeyserGlyphData glyphs,
                                GeyserLanguageData languages,
                                SidebarProfiles scoreboard,
                                @Nullable JsonObject nativeModels,
                                @Nullable JsonObject soundMappings,
                                @Nullable JsonObject bossBars,
                                @Nullable Path clientPack) {
        this.bossBars = bossBars;
        this.soundMappings = soundMappings;
        this.clientPack = clientPack;
        this.scoreboard = scoreboard;
        this.nativeModels = nativeModels;
        this.items = items;
        this.blocks = blocks;
        this.modelDisplays = modelDisplays;
        this.elytraVariants = elytraVariants;
        this.glyphs = glyphs;
        this.languages = languages;
    }

    public static GeyserExtensionData read(Path path, Path cacheDirectory) throws IOException {
        if (Files.notExists(path)) {
            return EMPTY;
        }
        try (ZipFile archive = new ZipFile(path.toFile())) {
            JsonObject items = readPlan(archive, RegistrationPlanKind.ITEM);
            JsonObject blocks = readPlan(archive, RegistrationPlanKind.BLOCK);
            JsonObject sounds = readJson(archive, BedrockPackLayout.SOUND_MAPPINGS);
            GeyserGlyphData glyphs = GeyserGlyphs.parse(readJson(archive, BedrockPackLayout.GLYPHS));
            GeyserLanguageData languages = GeyserTranslations.parse(readJson(archive, BedrockPackLayout.TRANSLATIONS));
            return new GeyserExtensionData(items, blocks, readJson(archive, BedrockPackLayout.MODEL_DISPLAYS),
                    readJson(archive, BedrockPackLayout.ELYTRA_VARIANTS),
                    glyphs, languages, SidebarProfiles.parse(readJson(archive, BedrockPackLayout.SCOREBOARD)),
                    readJson(archive, BedrockPackLayout.NATIVE_MODELS), sounds,
                    readJson(archive, BedrockPackLayout.BOSS_BARS),
                    extractClientPack(archive, cacheDirectory));
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
    private static JsonObject readPlan(ZipFile archive, RegistrationPlanKind kind) throws IOException {
        JsonObject plan = readJson(archive, kind.file());
        if (plan == null) {
            return null;
        }
        if (!plan.get(KIND).getAsString().equals(kind.id()) || plan.get(FORMAT_VERSION).getAsInt() != kind.formatVersion()) {
            throw new IOException("Unsupported registration plan format: " + kind.file());
        }
        return plan;
    }

    @Nullable
    private static JsonObject readJson(ZipFile archive, String name) throws IOException {
        ZipEntry entry = archive.getEntry(name);
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

    @Nullable
    public JsonObject modelDisplays() {
        return this.modelDisplays;
    }

    @Nullable
    public JsonObject elytraVariants() {
        return this.elytraVariants;
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
