package net.momirealms.craftengine.core.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonElement;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.text.component.ComponentProvider;
import net.momirealms.craftengine.core.plugin.text.minimessage.CraftEngineTags;
import net.momirealms.sparrow.message.MiniMessage;
import net.momirealms.sparrow.message.tag.resolver.TagResolver;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.adventure.NBTComponentSerializer;
import net.momirealms.sparrow.nbt.adventure.NBTSerializerOptions;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class for handling Adventure components and related functionalities.
 */
public final class AdventureHelper {
    public static final String EMPTY_COMPONENT = componentToJson(Component.empty());
    private static final Cache<String, Pattern> PATTERN_CACHE = Caffeine.newBuilder()
            .maximumSize(4096)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    private final MiniMessage miniMessageForSerialize;
    private volatile MiniMessage miniMessage;
    private volatile MiniMessage customMiniMessage;
    private final GsonComponentSerializer gsonComponentSerializer;
    private final NBTComponentSerializer nbtComponentSerializer;
    private final LegacyComponentSerializer legacyComponentSerializer;
    private static final TextReplacementConfig REPLACE_LF = TextReplacementConfig.builder().matchLiteral("\n").replacement(Component.newline()).build();

    static {
        SparrowClass.of(SparrowClass.findNoRemap("net.kyori.adventure.text.TextComponentImpl")).getDeclaredSparrowField(FieldMatcher.named("WARN_WHEN_LEGACY_FORMATTING_DETECTED")).mh().set(null, false);
    }

    private AdventureHelper() {
        this.miniMessageForSerialize = MiniMessage.builder().strict(true).build();
        rebuildMiniMessages();
        GsonComponentSerializer.Builder gsonBuilder = GsonComponentSerializer.builder();
        if (!VersionHelper.isOrAbove1_20_5) {
            gsonBuilder.legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.get());
            gsonBuilder.editOptions((b) -> b.value(JSONOptions.EMIT_HOVER_SHOW_ENTITY_ID_AS_INT_ARRAY, false));
        }
        if (!VersionHelper.isOrAbove1_21_5) {
            gsonBuilder.editOptions((b) -> {
                b.value(JSONOptions.EMIT_CLICK_EVENT_TYPE, JSONOptions.ClickEventValueMode.CAMEL_CASE);
                b.value(JSONOptions.EMIT_HOVER_EVENT_TYPE, JSONOptions.HoverEventValueMode.CAMEL_CASE);
                b.value(JSONOptions.EMIT_HOVER_SHOW_ENTITY_KEY_AS_TYPE_AND_UUID_AS_ID, true);
            });
        }
        this.legacyComponentSerializer = LegacyComponentSerializer.builder().build();
        this.gsonComponentSerializer = gsonBuilder.build();
        this.nbtComponentSerializer = NBTComponentSerializer.builder()
                .editOptions((b) -> {
                    if (!VersionHelper.isOrAbove1_21_5) {
                        b.value(NBTSerializerOptions.MODERN_EVENT_TYPE, false);
                    }
                    if (!VersionHelper.isOrAbove1_20_5) {
                        b.value(NBTSerializerOptions.DATA_COMPONENT_RELEASE, false);
                    }
                    if (!VersionHelper.isOrAbove1_20_3) {
                        b.value(NBTSerializerOptions.INT_ARRAY_UUID, false);
                    }
                    b.value(NBTSerializerOptions.SERIALIZE_COMPONENT_TYPE, false);
                }).build();
    }

    private static class SingletonHolder {
        private static final AdventureHelper INSTANCE = new AdventureHelper();
    }

    public static AdventureHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static MiniMessage miniMessage() {
        return getInstance().miniMessage;
    }

    public static MiniMessage customMiniMessage() {
        return getInstance().customMiniMessage;
    }

    public static Component deserialize(String input, Context context) {
        return miniMessage().deserialize(input, context);
    }

    public static Component deserialize(String input, Context context, TagResolver... additional) {
        return miniMessage().deserialize(input, context, additional);
    }

    public static void refreshExternalTagResolvers() {
        getInstance().rebuildMiniMessages();
    }

    private void rebuildMiniMessages() {
        final TagResolver[] externals = externalTagResolvers();
        // standard tags + CraftEngine tags + external plugin tags
        this.miniMessage = MiniMessage.builder().tags(TagResolver.resolver(ArrayUtils.merge(ArrayUtils.merge(CraftEngineTags.INTERNAL, CraftEngineTags.STANDARD), externals))).build();
        // CraftEngine + external tags only, no standard formatting tags
        this.customMiniMessage = MiniMessage.builder().tags(TagResolver.resolver(ArrayUtils.merge(ArrayUtils.merge(CraftEngineTags.INTERNAL, externals), CraftEngineTags.SPECIAL_STANDARD))).build();
    }

    private static TagResolver[] externalTagResolvers() {
        try {
            CraftEngine engine = CraftEngine.instance();
            if (engine == null || engine.compatibilityManager() == null) {
                return new TagResolver[0];
            }
            TagResolver[] resolvers = engine.compatibilityManager().createExternalTagResolvers();
            return resolvers == null ? new TagResolver[0] : resolvers;
        } catch (Throwable ignored) {
            // too early in bootstrap — external tags will be compiled on the first registration
            return new TagResolver[0];
        }
    }

    public static LegacyComponentSerializer getLegacy() {
        return getInstance().legacyComponentSerializer;
    }

    public static String serializeMiniMessage(Component component) {
        return getInstance().miniMessageForSerialize.serialize(component);
    }

    public static GsonComponentSerializer getGson() {
        return getInstance().gsonComponentSerializer;
    }

    public static NBTComponentSerializer getNBT() {
        return getInstance().nbtComponentSerializer;
    }

    /**
     * Converts a JSON string to a MiniMessage string.
     *
     * @param json the JSON string
     * @return the MiniMessage string representation
     */
    public static String jsonToMiniMessage(String json) {
        return getInstance().miniMessageForSerialize.serialize(getInstance().gsonComponentSerializer.deserialize(json));
    }

    public static String componentToMiniMessage(Component component) {
        return getInstance().miniMessageForSerialize.serialize(component);
    }

    /**
     * Converts a JSON string to a Component.
     *
     * @param json the JSON string
     * @return the resulting Component
     */
    public static Component jsonToComponent(String json) {
        return getInstance().gsonComponentSerializer.deserialize(json);
    }

    public static Component jsonElementToComponent(JsonElement json) {
        return getInstance().gsonComponentSerializer.deserializeFromTree(json);
    }

    public static Component nbtToComponent(Tag tag) {
        return getInstance().nbtComponentSerializer.deserialize(tag);
    }

    public static Tag componentToNbt(Component component) {
        return getInstance().nbtComponentSerializer.serialize(component);
    }

    /**
     * Converts a Component to a JSON string.
     *
     * @param component the Component to convert
     * @return the JSON string representation
     */
    public static String componentToJson(Component component) {
        return getGson().serialize(component);
    }

    public static JsonElement componentToJsonElement(Component component) {
        return getGson().serializeToTree(component);
    }

    public static Tag componentToTag(Component component) {
        return getNBT().serialize(component);
    }

    public static Component tagToComponent(Tag tag) {
        return getNBT().deserialize(tag);
    }

    public static Component replaceShowItem(Component component, Function<HoverEvent.ShowItem, HoverEvent.ShowItem> replacer) {
        HoverEvent<?> hoverEvent = component.hoverEvent();
        if (hoverEvent != null && hoverEvent.action() == HoverEvent.Action.SHOW_ITEM) {
            Object showItem = hoverEvent.value();
            component = component.hoverEvent(HoverEvent.showItem(replacer.apply((HoverEvent.ShowItem) showItem)));
        }
        if (component instanceof TranslatableComponent translatableComponent) {
            List<TranslationArgument> newArgs = new ArrayList<>();
            for (TranslationArgument argument : translatableComponent.arguments()) {
                if (argument.value() instanceof Component argComponent) {
                    Component replaced = replaceShowItem(argComponent, replacer);
                    newArgs.add(TranslationArgument.component(replaced));
                } else {
                    newArgs.add(argument);
                }
            }
            component = translatableComponent.arguments(newArgs);
        }
        List<Component> newChildren = new ArrayList<>();
        for (Component child : component.children()) {
            newChildren.add(replaceShowItem(child, replacer));
        }
        return component.children(newChildren);
    }

    public static List<Component> splitLines(Component component) {
        List<Component> result = new ArrayList<>(4);
        Component line = Component.empty();
        Deque<Component> deque = new ArrayDeque<>();
        deque.addLast(component.replaceText(REPLACE_LF));
        while (!deque.isEmpty()) {
            Component current = deque.pollFirst();
            List<Component> children = current.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                Component child = children.get(i).applyFallbackStyle(current.style());
                deque.addFirst(child);
            }
            current = current.children(Collections.emptyList());
            if (current instanceof TextComponent text
                    && text.content().equals(Component.newline().content())) {
                result.add(line.compact());
                line = Component.empty();
            } else {
                line = line.append(current);
            }
        }
        if (Component.IS_NOT_EMPTY.test(line)) {
            result.add(line.compact());
        }
        return result;
    }

    /**
     * Checks if a character is a legacy color code.
     *
     * @param c the character to check
     * @return true if the character is a color code, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isLegacyColorCode(char c) {
        return c == '§' || c == '&';
    }

    public static boolean isHexColorCode(char c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f') ||
                (c >= 'A' && c <= 'F');
    }

    /**
     * Converts a legacy color code string to a MiniMessage string.
     *
     * @param legacy the legacy color code string
     * @return the MiniMessage string representation
     */
    public static String legacyToMiniMessage(String legacy) {
        StringBuilder stringBuilder = new StringBuilder();
        char[] chars = legacy.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (!isLegacyColorCode(chars[i])) {
                stringBuilder.append(chars[i]);
                continue;
            }
            if (i + 1 >= chars.length) {
                stringBuilder.append(chars[i]);
                continue;
            }
            switch (chars[i+1]) {
                case '0' -> stringBuilder.append("<black>");
                case '1' -> stringBuilder.append("<dark_blue>");
                case '2' -> stringBuilder.append("<dark_green>");
                case '3' -> stringBuilder.append("<dark_aqua>");
                case '4' -> stringBuilder.append("<dark_red>");
                case '5' -> stringBuilder.append("<dark_purple>");
                case '6' -> stringBuilder.append("<gold>");
                case '7' -> stringBuilder.append("<gray>");
                case '8' -> stringBuilder.append("<dark_gray>");
                case '9' -> stringBuilder.append("<blue>");
                case 'a' -> stringBuilder.append("<green>");
                case 'b' -> stringBuilder.append("<aqua>");
                case 'c' -> stringBuilder.append("<red>");
                case 'd' -> stringBuilder.append("<light_purple>");
                case 'e' -> stringBuilder.append("<yellow>");
                case 'f' -> stringBuilder.append("<white>");
                case 'r' -> stringBuilder.append("<reset><!i>");
                case 'l' -> stringBuilder.append("<b>");
                case 'm' -> stringBuilder.append("<st>");
                case 'o' -> stringBuilder.append("<i>");
                case 'n' -> stringBuilder.append("<u>");
                case 'k' -> stringBuilder.append("<obf>");
                case '#' -> {
                    if (i + 7 >= chars.length
                            || !isHexColorCode(chars[i+2])
                            || !isHexColorCode(chars[i+3])
                            || !isHexColorCode(chars[i+4])
                            || !isHexColorCode(chars[i+5])
                            || !isHexColorCode(chars[i+6])
                            || !isHexColorCode(chars[i+7])) {
                        stringBuilder.append(chars[i]);
                        continue;
                    }
                    stringBuilder
                            .append("<#")
                            .append(chars[i+2])
                            .append(chars[i+3])
                            .append(chars[i+4])
                            .append(chars[i+5])
                            .append(chars[i+6])
                            .append(chars[i+7])
                            .append(">");
                    i += 6;
                }
                case 'x' -> {
                    if (i + 13 >= chars.length
                            || !isLegacyColorCode(chars[i+2])
                            || !isLegacyColorCode(chars[i+4])
                            || !isLegacyColorCode(chars[i+6])
                            || !isLegacyColorCode(chars[i+8])
                            || !isLegacyColorCode(chars[i+10])
                            || !isLegacyColorCode(chars[i+12])) {
                        stringBuilder.append(chars[i]);
                        continue;
                    }
                    stringBuilder
                            .append("<#")
                            .append(chars[i+3])
                            .append(chars[i+5])
                            .append(chars[i+7])
                            .append(chars[i+9])
                            .append(chars[i+11])
                            .append(chars[i+13])
                            .append(">");
                    i += 12;
                }
                default -> {
                    stringBuilder.append(chars[i]);
                    continue;
                }
            }
            i++;
        }
        return stringBuilder.toString();
    }

    public static String plainTextContent(Component component) {
        StringBuilder sb = new StringBuilder();
        if (component instanceof TextComponent textComponent) {
            sb.append(textComponent.content());
        }
        for (Component child : component.children()) {
            sb.append(plainTextContent(child));
        }
        return sb.toString();
    }

    public static boolean isPureTextComponent(Component component) {
        if (!(component instanceof TextComponent textComponent)) {
            return false;
        }
        for (Component child : textComponent.children()) {
            if (!isPureTextComponent(child)) {
                return false;
            }
        }
        return true;
    }

    public static String resolvePlainStringTags(String raw, TagResolver... resolvers) {
        Component resultComponent = AdventureHelper.customMiniMessage().deserialize(raw, resolvers);
        return AdventureHelper.plainTextContent(resultComponent);
    }

    public static Component replaceText(Component text, Map<String, ComponentProvider> replacements, Context context) {
        int size = replacements.size();
        if (size == 0) return text;
        // 单个标签是最常见的情况(物品名/每一行lore), 同样必须走缓存, 否则每个物品的每一行都要重新编译正则
        String patternString = size == 1
                ? Pattern.quote(replacements.keySet().iterator().next())
                : replacements.keySet().stream()
                        .map(Pattern::quote)
                        .collect(Collectors.joining("|"));
        Pattern pattern = Objects.requireNonNull(PATTERN_CACHE.get(patternString, Pattern::compile));
        return replaceText(text, pattern, result ->
                Optional.ofNullable(replacements.get(result.group())).orElseThrow(() -> new IllegalStateException("Could not find tag '" + result.group() + "'")).apply(context)
        );
    }

    private static Component replaceText(Component text, Pattern pattern, Function<MatchResult, Component> replacement) {
        return FixedTextReplacementRenderer.INSTANCE.render(text, new FixedTextReplacementRenderer.State(pattern, replacement));
    }
}
