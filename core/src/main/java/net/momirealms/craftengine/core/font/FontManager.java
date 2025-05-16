package net.momirealms.craftengine.core.font;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.FormatUtils;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FontManager extends Manageable {
    Key DEFAULT_FONT = Key.of("minecraft:default");
    String BYPASS_BOOK = "craftengine.filter.bypass.book";
    String BYPASS_SIGN = "craftengine.filter.bypass.sign";
    String BYPASS_CHAT = "craftengine.filter.bypass.chat";
    String BYPASS_COMMAND = "craftengine.filter.bypass.command";
    String BYPASS_ANVIL = "craftengine.filter.bypass.anvil";

    default EmojiComponentProcessResult replaceComponentEmoji(@NotNull Component text, @Nullable Player player) {
        return replaceComponentEmoji(text, player, Config.maxEmojisPerParse());
    }

    default EmojiComponentProcessResult replaceComponentEmoji(@NotNull Component text, @Nullable Player player, int maxTimes) {
        return replaceComponentEmoji(text, player, AdventureHelper.plainTextContent(text), maxTimes);
    }

    default EmojiComponentProcessResult replaceComponentEmoji(@NotNull Component text, @Nullable Player player, String raw) {
        return replaceComponentEmoji(text, player, raw, Config.maxEmojisPerParse());
    }

    EmojiComponentProcessResult replaceComponentEmoji(@NotNull Component text, @Nullable Player player, @NotNull String raw, int maxTimes);

    default IllegalCharacterProcessResult processIllegalCharacters(String raw) {
        return processIllegalCharacters(raw, '*');
    }

    IllegalCharacterProcessResult processIllegalCharacters(String raw, char replacement);

    ConfigParser[] parsers();

    default EmojiTextProcessResult replaceMiniMessageEmoji(@NotNull String miniMessage, @Nullable Player player) {
        return replaceMiniMessageEmoji(miniMessage, player, Config.maxEmojisPerParse());
    }

    EmojiTextProcessResult replaceMiniMessageEmoji(@NotNull String miniMessage, @Nullable Player player, int maxTimes);

    default EmojiTextProcessResult replaceJsonEmoji(@NotNull String json, @Nullable Player player) {
        return replaceJsonEmoji(json, player, Config.maxEmojisPerParse());
    }

    EmojiTextProcessResult replaceJsonEmoji(@NotNull String jsonText, @Nullable Player player, int maxTimes);

    boolean isDefaultFontInUse();

    boolean isIllegalCodepoint(int codepoint);

    Collection<Font> fonts();

    Optional<BitmapImage> bitmapImageByCodepoint(Key font, int codepoint);

    default Optional<BitmapImage> bitmapImageByChars(Key font, char[] chars) {
        return bitmapImageByCodepoint(font, CharacterUtils.charsToCodePoint(chars));
    }

    Optional<BitmapImage> bitmapImageByImageId(Key imageId);

    Optional<Font> fontById(Key font);

    int codepointByImageId(Key imageId, int x, int y);

    default int codepointByImageId(Key imageId) {
        return this.codepointByImageId(imageId, 0, 0);
    }

    default char[] charsByImageId(Key imageId) {
        return charsByImageId(imageId, 0, 0);
    }

    default char[] charsByImageId(Key imageId, int x, int y) {
        return Character.toChars(this.codepointByImageId(imageId, x, y));
    }

    String createOffsets(int offset, FontTagFormatter tagFormatter);

    default String createMiniMessageOffsets(int offset) {
        return createOffsets(offset, FormatUtils::miniMessageFont);
    }

    default String createMineDownOffsets(int offset) {
        return createOffsets(offset, FormatUtils::mineDownFont);
    }

    default String createRawOffsets(int offset) {
        return createOffsets(offset, (raw, font) -> raw);
    }

    Map<String, Component> matchTags(String json);

    void refreshEmojiSuggestions(UUID uuid);
}
