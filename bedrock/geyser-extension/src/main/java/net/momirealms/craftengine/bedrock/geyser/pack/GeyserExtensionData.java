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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.KIND;

public final class GeyserExtensionData {
    public static final GeyserExtensionData EMPTY = new GeyserExtensionData(
            null, null, null, null,
            GeyserGlyphData.EMPTY, GeyserLanguageData.EMPTY, SidebarProfiles.EMPTY, false, Map.of(),
            null, null, null, null
    );
    private final RegistrationJson bossBars;
    private final SidebarProfiles scoreboard;
    private final boolean forceJavaTitleLayout;
    private final Map<String, Integer> verticalOffsets;
    private final RegistrationJson items;
    private final RegistrationJson blocks;
    private final RegistrationJson modelDisplays;
    private final RegistrationJson elytraVariants;
    private final GeyserGlyphData glyphs;
    private final GeyserLanguageData languages;
    private final RegistrationJson nativeModels;
    private final RegistrationJson soundMappings;
    private final Path clientPack;

    private GeyserExtensionData(@Nullable RegistrationJson items,
                                @Nullable RegistrationJson blocks,
                                @Nullable RegistrationJson modelDisplays,
                                @Nullable RegistrationJson elytraVariants,
                                GeyserGlyphData glyphs,
                                GeyserLanguageData languages,
                                SidebarProfiles scoreboard,
                                boolean forceJavaTitleLayout,
                                Map<String, Integer> verticalOffsets,
                                @Nullable RegistrationJson nativeModels,
                                @Nullable RegistrationJson soundMappings,
                                @Nullable RegistrationJson bossBars,
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
            RegistrationJson items = RegistrationJson.read(archive, RegistrationPlanKind.ITEM.file(), RegistrationPlanKind.ITEM);
            RegistrationJson blocks = RegistrationJson.read(archive, RegistrationPlanKind.BLOCK.file(), RegistrationPlanKind.BLOCK);
            RegistrationJson sounds = RegistrationJson.read(archive, BedrockPackLayout.SOUND_MAPPINGS);
            JsonObject title = readJson(archive, BedrockPackLayout.TITLE);
            boolean forceJavaTitleLayout = title != null && title.has(BedrockPackLayout.FORCE_JAVA_LAYOUT) && title.get(BedrockPackLayout.FORCE_JAVA_LAYOUT).getAsBoolean();
            Map<String, Integer> verticalOffsets = readVerticalOffsets(title);
            GeyserGlyphData glyphs = readFonts(archive, verticalOffsets);
            GeyserLanguageData languages = GeyserTranslations.parse(readJson(archive, BedrockPackLayout.TRANSLATIONS));
            return new GeyserExtensionData(items, blocks, RegistrationJson.read(archive, BedrockPackLayout.MODEL_DISPLAYS),
                    RegistrationJson.read(archive, BedrockPackLayout.ELYTRA_VARIANTS),
                    glyphs, languages, SidebarProfiles.parse(readJson(archive, BedrockPackLayout.SCOREBOARD)),
                    forceJavaTitleLayout, verticalOffsets,
                    RegistrationJson.read(archive, BedrockPackLayout.NATIVE_MODELS), sounds,
                    RegistrationJson.read(archive, BedrockPackLayout.BOSS_BARS),
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
        String expected = BedrockPackFiles.readDigest(archive, BedrockPackFiles.CLIENT_SHA256);
        if (Files.isRegularFile(pack) && Files.size(pack) == entry.getSize() && expected.equals(BedrockPackFiles.sha256(pack))) {
            return pack;
        }
        Path temporary = Files.createTempFile(cacheDirectory, "client-", ".tmp");
        try {
            MessageDigest digest = BedrockPackFiles.sha256();
            try (InputStream input = new DigestInputStream(archive.getInputStream(entry), digest)) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!expected.equals(HexFormat.of().formatHex(digest.digest()))) {
                throw new IOException("Checksum mismatch for " + BedrockPackFiles.CLIENT_PACK);
            }
            return Files.move(temporary, pack, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Nullable
    public JsonObject bossBars() {
        return this.bossBars == null ? null : this.bossBars.parse();
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
        return this.items == null ? null : this.items.parse();
    }

    @Nullable
    public JsonObject blocks() {
        return this.blocks == null ? null : this.blocks.parse();
    }

    @Nullable
    public JsonObject modelDisplays() {
        return this.modelDisplays == null ? null : this.modelDisplays.parse();
    }

    @Nullable
    public JsonObject elytraVariants() {
        return this.elytraVariants == null ? null : this.elytraVariants.parse();
    }

    public SidebarProfiles scoreboard() {
        return this.scoreboard;
    }

    private record RegistrationJson(byte[] utf8) {
        @Nullable
        private static RegistrationJson read(ZipFile archive, String name) throws IOException {
            return read(archive, name, null);
        }

        @Nullable
        private static RegistrationJson read(ZipFile archive, String name, @Nullable RegistrationPlanKind plan) throws IOException {
            ZipEntry entry = archive.getEntry(name);
            if (entry == null) {
                return null;
            }
            try (DataInputStream input = new DataInputStream(archive.getInputStream(entry))) {
                byte[] utf8 = new byte[Math.toIntExact(entry.getSize())];
                input.readFully(utf8);
                RegistrationJson snapshot = new RegistrationJson(utf8);
                try (JsonReader reader = new JsonReader(snapshot.reader())) {
                    reader.setStrictness(Strictness.LENIENT);
                    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                        throw new IOException("Expected an object in " + name);
                    }
                    if (plan == null) {
                        reader.skipValue();
                    } else {
                        validatePlan(reader, plan);
                    }
                    if (reader.peek() != JsonToken.END_DOCUMENT) {
                        throw new IOException("Unexpected trailing data in " + name);
                    }
                }
                return snapshot;
            } catch (RuntimeException exception) {
                throw new IOException("Invalid extension data entry: " + name, exception);
            }
        }

        private static void validatePlan(JsonReader reader, RegistrationPlanKind plan) throws IOException {
            String kind = null;
            int version = -1;
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case KIND -> kind = reader.nextString();
                    case FORMAT_VERSION -> version = reader.nextInt();
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
            if (!plan.id().equals(kind) || version != plan.formatVersion()) {
                throw new IOException("Unsupported registration plan format: " + plan.file());
            }
        }

        private InputStreamReader reader() {
            return new InputStreamReader(new ByteArrayInputStream(this.utf8), StandardCharsets.UTF_8);
        }

        private JsonObject parse() {
            return JsonParser.parseReader(this.reader()).getAsJsonObject();
        }
    }

    private static Map<String, Integer> readVerticalOffsets(@Nullable JsonObject title) {
        if (title == null || !title.has(BedrockPackLayout.TEXT_VERTICAL_OFFSETS)) {
            return Map.of();
        }
        Map<String, Integer> offsets = new HashMap<>();
        title.getAsJsonObject(BedrockPackLayout.TEXT_VERTICAL_OFFSETS).asMap().forEach((font, value) -> offsets.put(font, value.getAsInt()));
        return Map.copyOf(offsets);
    }

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
        return this.nativeModels == null ? null : this.nativeModels.parse();
    }

    @Nullable
    public JsonObject soundMappings() {
        return this.soundMappings == null ? null : this.soundMappings.parse();
    }
}
