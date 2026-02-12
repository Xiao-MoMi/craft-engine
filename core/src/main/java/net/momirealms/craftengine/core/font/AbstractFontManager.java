package net.momirealms.craftengine.core.font;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.allocator.IdAllocator;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.config.IdSectionConfigParser;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import org.ahocorasick.trie.Token;
import org.ahocorasick.trie.Trie;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class AbstractFontManager implements FontManager {
    private final CraftEngine plugin;
    // namespace:font font
    private final Map<Key, Font> fonts = new HashMap<>();
    // namespace:id emoji
    private final Map<Key, Emoji> emojis = new HashMap<>();
    // namespace:id image
    private final Map<Key, BitmapImage> bitmapImages = new HashMap<>();
    private final Map<Key, Image> images = new HashMap<>();
    private final Map<String, Image> imagesByValue = new HashMap<>();
    private final Set<Integer> illegalChars = new HashSet<>();
    private final ImageParser imageParser;
    private final EmojiParser emojiParser;
    private OffsetFont offsetFont;

    protected Trie emojiKeywordTrie;
    protected Map<String, Emoji> emojiMapper;
    protected List<Emoji> emojiList;
    protected List<String> allEmojiSuggestions;
    // Cached command suggestions
    protected final List<Suggestion> cachedImagesSuggestions = new ArrayList<>();

    public AbstractFontManager(CraftEngine plugin) {
        this.plugin = plugin;
        this.imageParser = new ImageParser();
        this.emojiParser = new EmojiParser();
    }

    public ImageParser imageParser() {
        return imageParser;
    }

    public EmojiParser emojiParser() {
        return emojiParser;
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
        Optional.ofNullable(this.fonts.get(DEFAULT_FONT)).ifPresent(font -> this.illegalChars.addAll(font.codepointsInUse()));
        // global shift l10n image
        this.buildEmojiKeywordsTrie();
        this.emojiList = new ArrayList<>(this.emojis.values());
        this.allEmojiSuggestions = this.emojis.values().stream()
                .flatMap(emoji -> emoji.keywords().stream())
                .collect(Collectors.toList());
    }

    @Override
    public EmojiTextProcessResult replaceMiniMessageEmoji(@NotNull String miniMessage, Player player, int maxTimes) {
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
                    emoji.content(),
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
    public EmojiTextProcessResult replaceJsonEmoji(@NotNull String jsonText, Player player, int maxTimes) {
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
                    emoji.content(),
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
    public EmojiComponentProcessResult replaceComponentEmoji(@NotNull Component text, Player player, @NotNull String raw, int maxTimes) {
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
                    emoji.content(),
                    PlayerOptionalContext.of(player, ContextHolder.builder()
                            .withOptionalParameter(EmojiParameters.EMOJI, emoji.emojiImage())
                            .withParameter(EmojiParameters.KEYWORD, emoji.keywords().get(0))
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

    private Font getOrCreateFont(Key key) {
        return this.fonts.computeIfAbsent(key, Font::new);
    }

    public class EmojiParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"emojis", "emoji"};

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.EMOJI;
        }

        @Override
        public int count() {
            return AbstractFontManager.this.emojis.size();
        }

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (AbstractFontManager.this.emojis.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.emoji.duplicate");
            }
            String permission = (String) section.get("permission");
            Object keywordsRaw = section.get("keywords");
            if (keywordsRaw == null) {
                throw new LocalizedResourceConfigException("warning.config.emoji.missing_keywords");
            }
            List<String> keywords = MiscUtils.getAsStringList(keywordsRaw);
            if (keywords.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.emoji.missing_keywords");
            }
            Object rawContent = section.getOrDefault("content", "<white><arg:emoji></white>");
            String content;
            if (rawContent instanceof List<?> list) {
                content = list.stream().map(Object::toString).collect(Collectors.joining());
            } else {
                content = rawContent.toString();
            }
            String image = null;
            if (section.containsKey("image")) {
                String rawImage = section.get("image").toString();
                String[] split = rawImage.split(":");
                if (split.length == 2) {
                    Key imageId = new Key(split[0], split[1]);
                    Optional<Image> bitmapImage = imageById(imageId);
                    if (bitmapImage.isPresent() && bitmapImage.get() != DummyImage.INSTANCE) {
                        image = bitmapImage.get().miniMessageAt(0, 0);
                    } else {
                        throw new LocalizedResourceConfigException("warning.config.emoji.invalid_image", rawImage);
                    }
                } else if (split.length == 4) {
                    Key imageId = new Key(split[0], split[1]);
                    Optional<Image> bitmapImage = imageById(imageId);
                    if (bitmapImage.isPresent() && bitmapImage.get() != DummyImage.INSTANCE) {
                        try {
                            image = bitmapImage.get().miniMessageAt(Integer.parseInt(split[2]), Integer.parseInt(split[3]));
                        } catch (ArrayIndexOutOfBoundsException e) {
                            throw new LocalizedResourceConfigException("warning.config.emoji.invalid_image", rawImage);
                        }
                    } else {
                        throw new LocalizedResourceConfigException("warning.config.emoji.invalid_image", rawImage);
                    }
                } else {
                    throw new LocalizedResourceConfigException("warning.config.emoji.invalid_image", rawImage);
                }
            }
            Emoji emoji = new Emoji(content, permission, image, keywords);
            AbstractFontManager.this.emojis.put(id, emoji);
        }
    }

    public class ImageParser extends IdSectionConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"images", "image"};
        private final Map<Key, IdAllocator> idAllocators = new HashMap<>();

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.IMAGE;
        }

        @Override
        public int count() {
            return AbstractFontManager.this.bitmapImages.size();
        }

        @Override
        public void postProcess() {
            for (Map.Entry<Key, IdAllocator> entry : this.idAllocators.entrySet()) {
                entry.getValue().processPendingAllocations();
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

        public IdAllocator getOrCreateIdAllocator(Key key) {
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

        @Override
        public void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) {
            if (AbstractFontManager.this.bitmapImages.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.image.duplicate");
            }

            // 引用类型的
            Object ref = section.get("ref");
            boolean special = false;
            if (ref != null) {
                String rawRef = ref.toString();
                String[] param = rawRef.split(":", 4);
                int row;
                int col;
                Key refId;
                if (param.length == 4) {
                    row = Integer.parseInt(param[2]);
                    col = Integer.parseInt(param[3]);
                    refId = Key.of(param[0], param[1]);
                } else if (param.length == 3) {
                    row = Integer.parseInt(param[2]);
                    col = 0;
                    refId = Key.of(param[0], param[1]);
                } else {
                    row = ResourceConfigUtils.getAsInt(section.get("row"), "row");
                    col = ResourceConfigUtils.getAsInt(section.get("column"), "column");
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

            Object file = section.get("file");
            if (file == null) {
                throw new LocalizedResourceConfigException("warning.config.image.missing_file");
            }

            String resourceLocation = MiscUtils.make(CharacterUtils.replaceBackslashWithSlash(file.toString()), s -> s.endsWith(".png") ? s : s + ".png");
            if (!ResourceLocation.isValid(resourceLocation)) {
                throw new LocalizedResourceConfigException("warning.config.image.invalid_file_chars", resourceLocation);
            }
            String fontName = section.getOrDefault("font", pack.namespace()+ ":default").toString();
            if (!ResourceLocation.isValid(fontName)) {
                throw new LocalizedResourceConfigException("warning.config.image.invalid_font_chars", fontName);
            }

            Key fontId = Key.withDefaultNamespace(fontName, id.namespace());
            Font font = getOrCreateFont(fontId);

            IdAllocator allocator = getOrCreateIdAllocator(fontId);

            int rows;
            int columns;
            List<CompletableFuture<Integer>> futureCodepoints = new ArrayList<>();
            Object charsObj = ResourceConfigUtils.get(section, "chars", "char");
            // 自动分配
            if (charsObj == null) {
                Object grid = section.get("grid-size");
                if (grid != null) {
                    String gridString = grid.toString();
                    String[] split = gridString.split(",");
                    if (split.length != 2) {
                        throw new LocalizedResourceConfigException("warning.config.image.invalid_grid_size", gridString);
                    }
                    rows = Integer.parseInt(split[0]);
                    columns = Integer.parseInt(split[1]);
                    int chars = rows * columns;
                    if (chars <= 0) {
                        throw new LocalizedResourceConfigException("warning.config.image.invalid_grid_size", gridString);
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
            else if (charsObj instanceof List<?> list) {
                List<String> charsList = MiscUtils.getAsStringList(list);
                if (charsList.isEmpty() || charsList.getFirst().isEmpty()) {
                    throw new LocalizedResourceConfigException("warning.config.image.missing_char");
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
                        throw new LocalizedResourceConfigException("warning.config.image.invalid_codepoint_grid");
                    }
                }
                columns = tempColumns;
            }
            // 使用了具体的值
            else {
                if (charsObj instanceof Integer codepoint) {
                    futureCodepoints.add(allocator.assignFixedId(id.asString(), codepoint));
                    rows = 1;
                    columns = 1;
                } else {
                    String character = charsObj.toString();
                    if (character.isEmpty()) {
                        throw new LocalizedResourceConfigException("warning.config.image.missing_char");
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

            CompletableFutures.allOf(futureCodepoints).whenComplete((v, t) -> ResourceConfigUtils.runCatching(path, node, () -> {
                if (t != null) {
                    if (t instanceof CompletionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof IdAllocator.IdConflictException conflict) {
                            throw new LocalizedResourceConfigException("warning.config.image.codepoint.conflict",
                                    fontId.toString(),
                                    CharacterUtils.encodeCharsToUnicode(Character.toChars(conflict.id())),
                                    new String(Character.toChars(conflict.id())),
                                    conflict.previousOwner()
                            );
                        } else if (cause instanceof IdAllocator.IdExhaustedException) {
                            throw new LocalizedResourceConfigException("warning.config.image.codepoint.exhausted", fontId.asString());
                        }
                    }
                    throw new RuntimeException("Unknown error occurred", t);
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

                Object heightObj = section.get("height");
                if (heightObj == null) {
                    Key namespacedPath = Key.of(resourceLocation);
                    Path targetImagePath = pack.resourcePackFolder()
                            .resolve("assets")
                            .resolve(namespacedPath.namespace())
                            .resolve("textures")
                            .resolve(namespacedPath.value());
                    if (Files.exists(targetImagePath)) {
                        try (InputStream in = Files.newInputStream(targetImagePath)) {
                            BufferedImage image = ImageIO.read(in);
                            heightObj = image.getHeight() / codepointGrid.length;
                        } catch (IOException e) {
                            plugin.logger().warn("Failed to load image " + targetImagePath, e);
                            return;
                        }
                    } else {
                        throw new LocalizedResourceConfigException("warning.config.image.missing_height");
                    }
                }

                int height = ResourceConfigUtils.getAsInt(heightObj, "height");
                int ascent = ResourceConfigUtils.getAsInt(section.getOrDefault("ascent", height - 1), "ascent");
                if (height < ascent) {
                    throw new LocalizedResourceConfigException("warning.config.image.height_ascent_conflict", String.valueOf(height), String.valueOf(ascent));
                }

                BitmapImage bitmapImage = new BitmapImage(id, fontId, height, ascent, resourceLocation, codepointGrid);
                for (int[] y : codepointGrid) {
                    for (int x : y) {
                        font.addBitmapImage(x, bitmapImage);
                    }
                }

                AbstractFontManager.this.bitmapImages.put(id, bitmapImage);
                AbstractFontManager.this.images.put(id, bitmapImage);
                AbstractFontManager.this.imagesByValue.put(id.value(), bitmapImage);
                AbstractFontManager.this.cachedImagesSuggestions.add(Suggestion.suggestion(id.asString()));

            }, () -> GsonHelper.get().toJson(section)));
        }
    }
}
