package net.momirealms.craftengine.core.font;

import com.google.common.collect.ImmutableList;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.Identifier;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStages;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.util.*;
import org.ahocorasick.trie.Token;
import org.ahocorasick.trie.Trie;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractFontManager implements FontManager {
    private final CraftEngine plugin;
    // namespace:font font
    private final Map<Key, Font> fonts = new ConcurrentHashMap<>();
    // namespace:id emoji
    private final Map<Key, Emoji> emojis = new ConcurrentHashMap<>();
    // namespace:id image
    private final Map<Key, BitmapImage> bitmapImages = new ConcurrentHashMap<>();
    private final Map<Key, Image> images = new ConcurrentHashMap<>();
    private final Map<String, Image> imagesByValue = new ConcurrentHashMap<>();
    private final Set<Integer> illegalChars = new HashSet<>();
    private final ConfigParser imageParser;
    private final ConfigParser emojiParser;
    private OffsetFont offsetFont;

    protected Trie emojiKeywordTrie;
    protected Map<String, Emoji> emojiMapper;
    protected List<Emoji> emojiList;
    protected List<String> allEmojiSuggestions;
    // Cached command suggestions
    protected final List<Suggestion> cachedImagesSuggestions = Collections.synchronizedList(new ArrayList<>());

    public AbstractFontManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.imageParser = new ImageParser();
        this.emojiParser = new EmojiParser();
    }

    @Override
    public void load() {
        this.offsetFont = Optional.ofNullable(plugin.config().settings().getSection("image.offset-characters"))
                .filter(section -> section.getBoolean("enable", true))
                .map(OffsetFont::new)
                .orElse(null);
    }

    @Override
    public OffsetFont offsetFont() {
        return this.offsetFont;
    }

    @Override
    public Map<Key, BitmapImage> loadedBitmapImages() {
        return Collections.unmodifiableMap(this.bitmapImages);
    }

    @Override
    public Map<Key, Image> loadedImages() {
        return Collections.unmodifiableMap(this.images);
    }

    @Override
    public Map<Key, Emoji> emojis() {
        return Collections.unmodifiableMap(this.emojis);
    }

    @Override
    public void unload() {
        this.fonts.clear();
        this.bitmapImages.clear();
        this.images.clear();
        this.imagesByValue.clear();
        this.cachedImagesSuggestions.clear();
        this.illegalChars.clear();
        this.emojis.clear();
        this.emojiKeywordTrie = null;
        if (this.emojiMapper != null) {
            this.emojiMapper.clear();
        }
    }

    @Override
    public void disable() {
        this.unload();
    }

    @Override
    public ConfigParser[] parsers() {
        return new ConfigParser[] {this.imageParser, this.emojiParser};
    }

    @Override
    public void delayedLoad() {
        Player[] players = CraftEngine.instance().networkManager().onlineUsers();
        for (Player player : players) {
            this.removeEmojiSuggestions(player);
        }
        Optional.ofNullable(this.fonts.get(DEFAULT_FONT)).ifPresent(font -> this.illegalChars.addAll(font.codepointsInUse()));
        // global shift l10n image
        this.buildEmojiKeywordsTrie();
        this.emojiList = new ArrayList<>(this.emojis.values());
        this.allEmojiSuggestions = this.emojis.values().stream()
                .flatMap(emoji -> emoji.keywords().stream())
                .collect(ImmutableList.toImmutableList());
        for (Player player : players) {
            this.addEmojiSuggestions(player);
        }
    }

    @Override
    public EmojiTextProcessResult replaceMiniMessageEmoji(@NotNull String miniMessage, Player player, int maxTimes, @Nullable EmojiUseCase useCase) {
        if (this.emojiKeywordTrie == null || maxTimes <= 0) {
            return EmojiTextProcessResult.notReplaced(miniMessage);
        }
        Map<String, String> replacements = new HashMap<>();
        for (Token token : this.emojiKeywordTrie.tokenize(miniMessage)) {
            if (!token.isMatch())
                continue;
            String fragment = token.getFragment();
            if (replacements.containsKey(fragment))
                continue;
            Emoji emoji = this.emojiMapper.get(fragment);
            if (emoji == null || (player != null && emoji.permission() != null && !player.hasPermission(emoji.permission())))
                continue;
            Component content = AdventureHelper.miniMessage().deserialize(
                    emoji.content(useCase),
                    PlayerOptionalContext.of(player, ContextHolder.builder()
                            .withOptionalParameter(EmojiParameters.EMOJI, emoji.emojiImage())
                            .withParameter(EmojiParameters.KEYWORD, emoji.keywords().get(0))
                    ).tagResolvers()
            );
            replacements.put(fragment, AdventureHelper.componentToMiniMessage(content));
        }
        if (replacements.isEmpty()) return EmojiTextProcessResult.notReplaced(miniMessage);
        String regex = replacements.keySet().stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(miniMessage);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (matcher.find() && count < maxTimes) {
            String key = matcher.group();
            String replacement = replacements.get(key);
            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                count++;
            } else {
                // should not reach this
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return EmojiTextProcessResult.replaced(sb.toString());
    }

    @Override
    public EmojiTextProcessResult replaceJsonEmoji(@NotNull String jsonText, Player player, int maxTimes, @Nullable EmojiUseCase useCase) {
        if (this.emojiKeywordTrie == null) {
            return EmojiTextProcessResult.notReplaced(jsonText);
        }
        Map<String, Component> emojis = new HashMap<>();
        for (Token token : this.emojiKeywordTrie.tokenize(jsonText)) {
            if (!token.isMatch())
                continue;
            String fragment = token.getFragment();
            if (emojis.containsKey(fragment)) continue;
            Emoji emoji = this.emojiMapper.get(fragment);
            if (emoji == null || (player != null && emoji.permission() != null && !player.hasPermission(emoji.permission())))
                continue;
            emojis.put(fragment, AdventureHelper.miniMessage().deserialize(
                    emoji.content(useCase),
                    PlayerOptionalContext.of(player, ContextHolder.builder()
                            .withOptionalParameter(EmojiParameters.EMOJI, emoji.emojiImage())
                            .withParameter(EmojiParameters.KEYWORD, emoji.keywords().getFirst())
                    ).tagResolvers())
            );
            if (emojis.size() >= maxTimes) break;
        }
        if (emojis.isEmpty()) return EmojiTextProcessResult.notReplaced(jsonText);
        Component component = AdventureHelper.jsonToComponent(jsonText);
        String patternString = emojis.keySet().stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        component = component.replaceText(builder -> builder.times(maxTimes)
                .match(Pattern.compile(patternString))
                .replacement((result, b) -> emojis.get(result.group())));
        return EmojiTextProcessResult.replaced(AdventureHelper.componentToJson(component));
    }

    @Override
    public EmojiComponentProcessResult replaceComponentEmoji(@NotNull Component text, Player player, @NotNull String raw, int maxTimes, @Nullable EmojiUseCase useCase) {
        Map<String, Component> emojis = new HashMap<>();
        for (Token token : this.emojiKeywordTrie.tokenize(raw)) {
            if (!token.isMatch())
                continue;
            String fragment = token.getFragment();
            if (emojis.containsKey(fragment))
                continue;
            Emoji emoji = this.emojiMapper.get(token.getFragment());
            if (emoji == null || (player != null && emoji.permission() != null && !player.hasPermission(Objects.requireNonNull(emoji.permission()))))
                continue;
            emojis.put(fragment, AdventureHelper.miniMessage().deserialize(
                    emoji.content(useCase),
                    PlayerOptionalContext.of(player, ContextHolder.builder()
                            .withOptionalParameter(EmojiParameters.EMOJI, emoji.emojiImage())
                            .withParameter(EmojiParameters.KEYWORD, emoji.keywords().getFirst())
                    ).tagResolvers()
            ));
            if (emojis.size() >= maxTimes) break;
        }
        if (emojis.isEmpty()) return EmojiComponentProcessResult.failed();
        String patternString = emojis.keySet().stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        text = text.replaceText(builder -> builder.times(maxTimes)
                .match(Pattern.compile(patternString))
                .replacement((result, b) -> emojis.get(result.group())));
        return EmojiComponentProcessResult.success(text);
    }

    private void buildEmojiKeywordsTrie() {
        this.emojiMapper = new HashMap<>();
        for (Emoji emoji : this.emojis.values()) {
            for (String keyword : emoji.keywords()) {
                this.emojiMapper.put(keyword, emoji);
            }
        }
        this.emojiKeywordTrie = Trie.builder()
                .ignoreOverlaps()
                .addKeywords(this.emojiMapper.keySet())
                .build();
    }

    @Override
    public boolean isDefaultFontInUse() {
        return !this.illegalChars.isEmpty();
    }

    @Override
    public boolean isIllegalCodepoint(int codepoint) {
        return this.illegalChars.contains(codepoint);
    }

    @Override
    public Collection<Font> fonts() {
        return Collections.unmodifiableCollection(this.fonts.values());
    }

    @Override
    public Optional<BitmapImage> bitmapImageByCodepoint(Key font, int codepoint) {
        return fontById(font).map(f -> f.bitmapImageByCodepoint(codepoint));
    }

    @Override
    public Optional<BitmapImage> bitmapImageById(Key id) {
        return Optional.ofNullable(this.bitmapImages.get(id));
    }

    @Override
    public Optional<Image> imageById(Key id) {
        return Optional.ofNullable(this.images.get(id));
    }

    public Optional<Image> imageByIdValue(String value) {
        return Optional.ofNullable(this.imagesByValue.get(value));
    }

    @Override
    public int codepointByImageId(Key key, int x, int y) {
        Image image = this.images.get(key);
        if (image == null) return -1;
        return image.codepointAt(x, y);
    }

    @Override
    public String createOffsets(int offset, FontTagFormatter tagFormatter) {
        return Optional.ofNullable(this.offsetFont).map(it -> it.createOffset(offset, tagFormatter)).orElse("");
    }

    @Override
    public Optional<Font> fontById(Key id) {
        return Optional.ofNullable(this.fonts.get(id));
    }

    @Override
    public Collection<Suggestion> cachedImagesSuggestions() {
        return Collections.unmodifiableCollection(this.cachedImagesSuggestions);
    }

    @Override
    public void refreshEmojiSuggestions(@NotNull Player player) {
        this.removeEmojiSuggestions(player);
        this.addEmojiSuggestions(player);
    }

    @Override
    public List<String> getEmojiSuggestions(@NotNull Player player) {
        List<String> suggestions = new ArrayList<>();
        if (this.emojiList == null) return suggestions;
        for (Emoji emoji : this.emojiList) {
            if (!emoji.chatCompletion()) continue;
            String permission = emoji.permission();
            if (permission != null && !player.hasPermission(permission)) continue;
            suggestions.addAll(emoji.keywords());
        }
        return suggestions;
    }

    private synchronized Font getOrCreateFont(Key key) {
        return this.fonts.computeIfAbsent(key, Font::new);
    }

    private final class EmojiParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"emojis", "emoji"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return AbstractFontManager.this.emojis.size();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.EMOJI;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.IMAGE);
        }

        private static final String[] CONTENT = new String[] {"content", "format"};
        private static final String[] CHAT_COMPLETION = new String[] {"chat_completion", "chat-completion"};
        private static final String[] CONTENT_OVERRIDES = new String[] {"content_overrides", "content-overrides"};

        @Override
        public boolean async() {
            return Config.multiThreadedConfigLoad();
        }

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            String permission = section.getString("permission");
            List<String> keywords = section.getNonNullStringList("keywords");
            Object rawContent = section.getOrDefault(CONTENT, "<white><arg:emoji></white>");
            String content;
            if (rawContent instanceof List<?> list) {
                content = list.stream().map(Object::toString).collect(Collectors.joining());
            } else {
                content = rawContent.toString();
            }
            String image = null;
            // 其实 emoji 里的 image 并非刚需
            ConfigValue imageValue = section.getValue("image");
            if (imageValue != null) {
                ConfigValue[] split = imageValue.splitValues(":", 4);
                if (split.length == 2) {
                    Key imageId = Key.of(split[0].getAsString(), split[1].getAsString());
                    Optional<Image> bitmapImage = imageById(imageId);
                    if (bitmapImage.isPresent() && bitmapImage.get() != DummyImage.INSTANCE) {
                        image = bitmapImage.get().miniMessageAt(0, 0);
                    } else {
                        throw new KnownResourceException("resource.emoji.unknown_image", section.assemblePath("image"), imageValue.getAsString());
                    }
                } else if (split.length == 4) {
                    Key imageId = Key.of(split[0].getAsString(), split[1].getAsString());
                    Optional<Image> bitmapImage = imageById(imageId);
                    if (bitmapImage.isPresent() && bitmapImage.get() != DummyImage.INSTANCE) {
                        try {
                            image = bitmapImage.get().miniMessageAt(split[2].getAsInt(), split[3].getAsInt());
                        } catch (ArrayIndexOutOfBoundsException e) {
                            throw new KnownResourceException("resource.emoji.unknown_image", section.assemblePath("image"), imageValue.getAsString());
                        }
                    } else {
                        throw new KnownResourceException("resource.emoji.unknown_image", section.assemblePath("image"), imageValue.getAsString());
                    }
                }
                if (image == null) {
                    throw new KnownResourceException("resource.emoji.invalid_image_format", section.assemblePath("image"), imageValue.getAsString());
                }
            }
            boolean chatCompletion = section.getBoolean(CHAT_COMPLETION, true);
            Emoji emoji = new Emoji(content, permission, image, keywords, chatCompletion,
                    Optional.ofNullable(section.getSection(CONTENT_OVERRIDES)).map(it -> {
                        Map<String, String> overrides = new HashMap<>();
                        for (Map.Entry<String, Object> entry : it.values().entrySet()) {
                            overrides.put(entry.getKey(), entry.getValue().toString());
                        }
                        return overrides;
                    }).orElse(Collections.emptyMap())
            );
            AbstractFontManager.this.emojis.put(id, emoji);
        }
    }

    private final class ImageParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"images", "image"};
        private final Map<Key, IdAllocator> idAllocators = new ConcurrentHashMap<>();

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int count() {
            return AbstractFontManager.this.bitmapImages.size();
        }

        @Override
        public LoadingStage loadingStage() {
            return LoadingStages.IMAGE;
        }

        @Override
        public List<LoadingStage> dependencies() {
            return List.of(LoadingStages.TEMPLATE);
        }

        @Override
        public void postProcess() {
            for (Map.Entry<Key, IdAllocator> entry : this.idAllocators.entrySet()) {
                IdAllocator allocator = entry.getValue();
                allocator.processPendingAllocations();
                for (CompletableFuture<?> future : allocator.combinedFutures()) {
                    try {
                        future.join();
                    } catch (CompletionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof IdAllocator.IdExhaustedException || cause instanceof IdAllocator.IdConflictException) {
                            continue;
                        }
                        AbstractFontManager.this.plugin.logger().warn("Error while assigning codepoint for font " + entry.getKey().asString(), e);
                    }
                }
                try {
                    entry.getValue().saveToCache();
                } catch (IOException e) {
                    AbstractFontManager.this.plugin.logger().warn("Error while saving codepoint allocation for font " + entry.getKey().asString(), e);
                }
            }
        }

        @Override
        public void preProcess() {
            this.idAllocators.clear();
        }

        public synchronized IdAllocator getOrCreateIdAllocator(Key key) {
            return this.idAllocators.computeIfAbsent(key, k -> {
                IdAllocator newAllocator = new IdAllocator(plugin.dataFolderPath().resolve("cache").resolve("font").resolve(k.namespace()).resolve(k.value() + ".json"));
                newAllocator.reset(Config.codepointStartingValue(k), 1114111); // utf16
                try {
                    newAllocator.loadFromCache();
                } catch (IOException e) {
                    AbstractFontManager.this.plugin.logger().warn("Error while loading chars data from cache for font " + k.asString(), e);
                }
                return newAllocator;
            });
        }

        private static final String[] CHAR = new String[] {"char", "chars", "unicode"};
        private static final String[] HEIGHT = new String[] {"height", "scale", "scale_ratio"};
        private static final String[] ASCENT = new String[] {"ascent", "y_position"};
        private static final String[] GRID_SIZE = new String[] {"grid_size", "grid-size"};

        @Override
        public void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section) {
            // 引用类型的
            boolean special = false; // 只填了个id没填命名空间的傻缺
            String ref = section.getString("ref");
            if (ref != null) {
                String[] param = ref.split(":", 4);
                int row;
                int col = 0;
                Key refId;
                if (param.length >= 3) {
                    try {
                        row = Integer.parseInt(param[2]);
                    } catch (NumberFormatException e) {
                        throw new KnownResourceException(ConfigConstants.PARSE_INT_FAILED, section.assemblePath("ref"), param[2]);
                    }
                    if (param.length == 4) {
                        try {
                            col = Integer.parseInt(param[3]);
                        } catch (NumberFormatException e) {
                            throw new KnownResourceException(ConfigConstants.PARSE_INT_FAILED, section.assemblePath("ref"), param[3]);
                        }
                    }
                    refId = Key.of(param[0], param[1]);
                } else {
                    row = section.getInt("row");
                    col = section.getInt("col");
                    if (param.length == 1) {
                        refId = Key.of(param[0]);
                        special = true;
                    } else {
                        refId = Key.of(param[0], param[1]);
                    }
                }
                ReferenceImage referenceImage;
                if (special) {
                    referenceImage = new ReferenceImage(LazyReference.lazyReference(() -> {
                        Image image = AbstractFontManager.this.imagesByValue.get(refId.value());
                        if (image instanceof BitmapImage bitmapImage) {
                            return bitmapImage;
                        }
                        return DummyImage.INSTANCE;
                    }), row, col);
                } else {
                    referenceImage = new ReferenceImage(LazyReference.lazyReference(() -> {
                        Optional<BitmapImage> bitmapImage = bitmapImageById(refId);
                        if (bitmapImage.isPresent()) {
                            return bitmapImage.get();
                        }
                        return DummyImage.INSTANCE;
                    }), row, col);
                }

                AbstractFontManager.this.images.put(id, referenceImage);
                AbstractFontManager.this.imagesByValue.put(id.value(), referenceImage);
                AbstractFontManager.this.cachedImagesSuggestions.add(Suggestion.suggestion(id.asString()));
                return;
            }

            String file = section.getNonNullString("file");
            String identifier = MiscUtils.make(CharacterUtils.replaceBackslashWithSlash(file), s -> s.endsWith(".png") ? s : s + ".png");
            if (!Identifier.isValid(identifier)) {
                throw new KnownResourceException(ConfigConstants.PARSE_IDENTIFIER_FAILED, section.assemblePath("file"), file);
            }
            String fontName = section.getString("font", pack.namespace()+ ":default");
            if (!Identifier.isValid(fontName)) {
                throw new KnownResourceException(ConfigConstants.PARSE_IDENTIFIER_FAILED, section.assemblePath("font"), fontName);
            }

            Key fontId = Key.withDefaultNamespace(fontName, id.namespace());
            IdAllocator allocator = getOrCreateIdAllocator(fontId);

            int rows;
            int columns;
            List<CompletableFuture<Integer>> futureCodepoints = new ArrayList<>();
            ConfigValue charsValue = section.getValue(CHAR);
            // 没有设置 chars 自动分配
            if (charsValue == null) {
                ConfigValue gridSizeValue = section.getValue(GRID_SIZE);
                if (gridSizeValue != null) {
                    ConfigValue[] splitSize = gridSizeValue.splitValuesRestrict(",", 2);
                    rows = splitSize[0].getAsInt();
                    columns = splitSize[1].getAsInt();
                    int chars = rows * columns;
                    if (chars <= 0) {
                        throw new KnownResourceException("resource.image.invalid_grid_size", gridSizeValue.path(), gridSizeValue.getAsString());
                    }
                    for (int i = 0; i < rows; i++) {
                        for (int j = 0; j < columns; j++) {
                            futureCodepoints.add(allocator.requestAutoId(id.asString() + ":" + i + ":" + j));
                        }
                    }
                } else {
                    rows = 1;
                    columns = 1;
                    futureCodepoints.add(allocator.requestAutoId(id.asString()));
                }
            }
            // 使用了list
            else if (charsValue.is(List.class)) {
                List<String> charsList = charsValue.getAsStringList();
                // 阻止空列表和首个元素为空的类别
                if (charsList.isEmpty() || charsList.getFirst().isEmpty()) {
                    throw new KnownResourceException("resource.image.empty_chars", section.assembleExistingPath("chars", "char"));
                }
                int tempColumns = -1;
                rows = charsList.size();
                for (int i = 0; i < charsList.size(); i++) {
                    String charString = charsList.get(i);
                    int[] codepoints;
                    if (charString.startsWith("\\u")) {
                        codepoints = CharacterUtils.charsToCodePoints(CharacterUtils.decodeUnicodeToChars(charString));
                    } else {
                        codepoints = CharacterUtils.charsToCodePoints(charString.toCharArray());
                    }
                    for (int j = 0; j < codepoints.length; j++) {
                        if (codepoints[j] == 0) {
                            futureCodepoints.add(CompletableFuture.completedFuture(0));
                        } else {
                            futureCodepoints.add(allocator.assignFixedId(id.asString() + ":" + i + ":" + j, codepoints[j]));
                        }
                    }
                    if (tempColumns == -1) {
                        tempColumns = codepoints.length;
                    } else if (tempColumns != codepoints.length) {
                        throw new KnownResourceException("resource.image.invalid_chars_grid", section.assemblePath("chars"), String.valueOf(codepoints.length), String.valueOf(tempColumns));
                    }
                }
                columns = tempColumns;
            }
            // 使用了具体的值
            else {
                if (charsValue.is(Number.class)) {
                    futureCodepoints.add(allocator.assignFixedId(id.asString(), charsValue.getAsInt()));
                    rows = 1;
                    columns = 1;
                } else {
                    String character = charsValue.getAsString();
                    if (character.isEmpty()) {
                        throw new KnownResourceException("resource.image.empty_chars", section.assembleExistingPath("char", "chars"));
                    }
                    rows = 1;
                    int[] codepoints;
                    if (character.startsWith("\\u")) {
                        codepoints = CharacterUtils.charsToCodePoints(CharacterUtils.decodeUnicodeToChars(character));
                    } else {
                        codepoints = CharacterUtils.charsToCodePoints(character.toCharArray());
                    }
                    columns = codepoints.length;
                    for (int i = 0; i < codepoints.length; i++) {
                        if (codepoints[i] == 0) {
                            futureCodepoints.add(CompletableFuture.completedFuture(0));
                        } else {
                            futureCodepoints.add(allocator.assignFixedId(id.asString() + ":0:" + i, codepoints[i]));
                        }
                    }
                }
            }

            allocator.addCombinedFuture(CompletableFutures.allOf(futureCodepoints).whenCompleteAsync((v, t) -> {
                ResourceConfigUtils.runCatching(path, section.path(), () -> {
                    if (t != null) {
                        if (t instanceof CompletionException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof IdAllocator.IdConflictException conflict) {
                                error(new KnownResourceException(path, "resource.image.codepoint_conflict",
                                        section.path(),
                                        CharacterUtils.encodeCharsToUnicode(Character.toChars(conflict.id())),
                                        new String(Character.toChars(conflict.id())),
                                        fontId.asString(),
                                        conflict.previousOwner()
                                ));
                            } else if (cause instanceof IdAllocator.IdExhaustedException) {
                                error(new KnownResourceException(path, "resource.image.codepoint_exhausted", section.path(), fontId.asString()));
                            }
                        }
                        return;
                    }

                    int[][] codepointGrid = new int[rows][columns];

                    for (int i = 0; i < rows; i++) {
                        for (int j = 0; j < columns; j++) {
                            try {
                                int codepoint = futureCodepoints.get(i * columns + j).get();
                                codepointGrid[i][j] = codepoint;
                            } catch (InterruptedException | ExecutionException e) {
                                AbstractFontManager.this.plugin.logger().warn("Interrupted while allocating codepoint for image " + id.asString(), e);
                                return;
                            }
                        }
                    }

                    int height = section.getInt(HEIGHT, () -> {
                        Key namespacedPath = Key.of(identifier);
                        Path targetImagePath = pack.resourcePackFolder()
                                .resolve("assets")
                                .resolve(namespacedPath.namespace())
                                .resolve("textures")
                                .resolve(namespacedPath.value());
                        if (Files.exists(targetImagePath)) {
                            try (InputStream in = Files.newInputStream(targetImagePath)) {
                                BufferedImage image = ImageIO.read(in);
                                return image.getHeight() / codepointGrid.length;
                            } catch (IOException e) {
                                throw new RuntimeException("Could not read image " + targetImagePath, e);
                            }
                        }
                        // 会自动触发缺少参数错误
                        return section.getNonNullInt("height");
                    });

                    int ascent = section.getInt(ASCENT, height - 1);
                    if (height < ascent) {
                        throw new KnownResourceException("resource.image.height_ascent_conflict", section.path(), String.valueOf(height), String.valueOf(ascent));
                    }

                    BitmapImage bitmapImage = new BitmapImage(id, fontId, height, ascent, identifier, codepointGrid);
                    Font font = getOrCreateFont(fontId);
                    for (int[] y : codepointGrid) {
                        for (int x : y) {
                            font.addBitmapImage(x, bitmapImage);
                        }
                    }

                    AbstractFontManager.this.bitmapImages.put(id, bitmapImage);
                    AbstractFontManager.this.images.put(id, bitmapImage);
                    AbstractFontManager.this.imagesByValue.put(id.value(), bitmapImage);
                    AbstractFontManager.this.cachedImagesSuggestions.add(Suggestion.suggestion(id.asString()));

                }, super.errorHandler);
            }, AbstractFontManager.this.plugin.scheduler().async()));
        }
    }
}
