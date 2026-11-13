package net.momirealms.craftengine.bedrock.geyser.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.momirealms.craftengine.bedrock.BedrockPackLayout;
import net.momirealms.craftengine.bedrock.RegistrationPlanKind;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphData;
import net.momirealms.craftengine.bedrock.geyser.scoreboard.SidebarProfiles;
import net.momirealms.craftengine.bedrock.geyser.glyph.GeyserGlyphs;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserLanguageData;
import net.momirealms.craftengine.bedrock.geyser.translation.GeyserTranslations;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.KIND;

public final class GeyserExtensionData {
    public static final GeyserExtensionData EMPTY = new GeyserExtensionData(null, null, GeyserGlyphData.EMPTY, GeyserLanguageData.EMPTY, SidebarProfiles.EMPTY);
    private final SidebarProfiles scoreboard;
    private final JsonObject items;
    private final boolean convertText;
    private final JsonObject blocks;
    private final GeyserGlyphData glyphs;
    private final GeyserLanguageData languages;

    private GeyserExtensionData(@Nullable JsonObject items, @Nullable JsonObject blocks, GeyserGlyphData glyphs, GeyserLanguageData languages, SidebarProfiles scoreboard) {
        this.scoreboard = scoreboard;
        this.items = items;
        JsonObject displays = items == null ? null : items.getAsJsonObject("item_displays");
        this.convertText = displays != null && displays.has("convert_text") && displays.get("convert_text").getAsBoolean();
        this.blocks = blocks;
        this.glyphs = glyphs;
        this.languages = languages;
    }

    public static GeyserExtensionData read(Path path) throws IOException {
        if (Files.notExists(path)) {
            return EMPTY;
        }
        try (ZipFile archive = new ZipFile(path.toFile())) {
            JsonObject items = readPlan(archive, RegistrationPlanKind.ITEM);
            JsonObject blocks = readPlan(archive, RegistrationPlanKind.BLOCK);
            GeyserGlyphData glyphs = GeyserGlyphs.parse(readJson(archive, BedrockPackLayout.GLYPHS));
            GeyserLanguageData languages = GeyserTranslations.parse(readJson(archive, BedrockPackLayout.TRANSLATIONS));
            return new GeyserExtensionData(items, blocks, glyphs, languages, SidebarProfiles.parse(readJson(archive, BedrockPackLayout.SCOREBOARD)));
        } catch (IOException | RuntimeException exception) {
            throw new IOException("Failed to load extension data from " + path, exception);
        }
    }

    @Nullable
    private static JsonObject readPlan(ZipFile archive, RegistrationPlanKind kind) throws IOException {
        JsonObject plan = readJson(archive, kind.file());
        if (plan != null && (!plan.get(KIND).getAsString().equals(kind.id()) || plan.get(FORMAT_VERSION).getAsInt() != kind.formatVersion())) {
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
            throw new IOException("Invalid extension data entry: " + name, exception);
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
}
