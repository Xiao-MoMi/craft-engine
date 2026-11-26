package net.momirealms.craftengine.bedrock.geyser.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.momirealms.craftengine.bedrock.BedrockPackFiles;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.SidebarProfiles;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserLanguageData;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserTranslations;
import net.momirealms.craftengine.bedrock.text.FontData;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.KIND;

public final class GeyserExtensionData {
    public static final GeyserExtensionData EMPTY = new GeyserExtensionData(
            null, null, null, null,
            GeyserGlyphData.EMPTY, GeyserLanguageData.EMPTY, SidebarProfiles.EMPTY, false, null,
            null, null, null, null
    );
    private final JsonObject bossBars;
    private final SidebarProfiles scoreboard;
    private final boolean forceJavaTitleLayout;
    @Nullable private final Map<String, Integer> verticalOffsets;
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
                                boolean forceJavaTitleLayout,
                                @Nullable Map<String, Integer> verticalOffsets,
                                @Nullable JsonObject nativeModels,
                                @Nullable JsonObject soundMappings,
                                @Nullable JsonObject bossBars,
                                @Nullable Path clientPack) {
        this.bossBars = bossBars;
        this.soundMappings = soundMappings;
        this.clientPack = clientPack;
        this.scoreboard = scoreboard;
        this.forceJavaTitleLayout = forceJavaTitleLayout;
        this.verticalOffsets = verticalOffsets;
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
            JsonObject title = readJson(archive, BedrockPackLayout.TITLE);
            boolean forceJavaTitleLayout = title != null && title.has(BedrockPackLayout.FORCE_JAVA_LAYOUT) && title.get(BedrockPackLayout.FORCE_JAVA_LAYOUT).getAsBoolean();
            Map<String, Integer> verticalOffsets = readVerticalOffsets(title);
            GeyserGlyphData glyphs = readFonts(archive, verticalOffsets == null ? Map.of() : verticalOffsets);
            GeyserLanguageData languages = GeyserTranslations.parse(readJson(archive, BedrockPackLayout.TRANSLATIONS));
            return new GeyserExtensionData(items, blocks, readJson(archive, BedrockPackLayout.MODEL_DISPLAYS),
                    readJson(archive, BedrockPackLayout.ELYTRA_VARIANTS),
                    glyphs, languages, SidebarProfiles.parse(readJson(archive, BedrockPackLayout.SCOREBOARD)),
                    forceJavaTitleLayout, verticalOffsets,
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

    private static GeyserGlyphData readFonts(ZipFile archive, Map<String, Integer> verticalOffsets) throws IOException {
        ZipEntry entry = archive.getEntry(BedrockPackLayout.FONTS);
        if (entry == null) {
            throw new IOException("Missing " + BedrockPackLayout.FONTS + "; regenerate the Bedrock pack");
        }
        try (JsonReader reader = new JsonReader(new InputStreamReader(archive.getInputStream(entry), StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.LENIENT);
            FontData fonts = FontData.read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Unexpected trailing data in " + entry.getName());
            }
            return new GeyserGlyphData(fonts, verticalOffsets);
        }
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

    @Nullable
    private static Map<String, Integer> readVerticalOffsets(@Nullable JsonObject title) {
        if (title == null || !title.has(BedrockPackLayout.TEXT_VERTICAL_OFFSETS)) {
            return null;
        }
        Map<String, Integer> offsets = new HashMap<>();
        title.getAsJsonObject(BedrockPackLayout.TEXT_VERTICAL_OFFSETS).asMap().forEach((font, value) -> offsets.put(font, value.getAsInt()));
        return Map.copyOf(offsets);
    }

    @Nullable
    public Map<String, Integer> verticalOffsets() {
        return this.verticalOffsets;
    }

    public boolean forceJavaTitleLayout() {
        return this.forceJavaTitleLayout;
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
