package net.momirealms.craftengine.core.font;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.sparrow.nbt.Tag;
import org.incendo.cloud.suggestion.Suggestion;
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

    OffsetFont offsetFont();

    Map<Key, Emoji> emojis();

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

    Map<Key, BitmapImage> loadedBitmapImages();

    Map<Key, Image> loadedImages();

    Optional<BitmapImage> bitmapImageByCodepoint(Key font, int codepoint);

    default Optional<BitmapImage> bitmapImageByChars(Key font, char[] chars) {
        return bitmapImageByCodepoint(font, CharacterUtils.charsToCodePoint(chars));
    }

    Optional<BitmapImage> bitmapImageById(Key imageId);

    Optional<Image> imageById(Key id);

    Optional<Font> fontById(Key font);

    Collection<Suggestion> cachedImagesSuggestions();

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

    void refreshEmojiSuggestions(UUID uuid);

    @Deprecated
    default Map<String, ComponentProvider> matchTags(String text) {
        return CraftEngine.instance().networkManager().matchNetworkTags(text);
    }

    @Deprecated
    default Map<String, ComponentProvider> matchTags(Tag nbt) {
        return CraftEngine.instance().networkManager().matchNetworkTags(new StringValueOnlyTagVisitor().visit(nbt));
    }
}
