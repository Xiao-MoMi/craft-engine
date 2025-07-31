package net.momirealms.craftengine.core.util;

import com.google.gson.JsonElement;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentIteratorType;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;
import net.kyori.adventure.text.serializer.json.legacyimpl.NBTLegacyHoverEventSerializer;
import net.momirealms.sparrow.nbt.Tag;
import net.momirealms.sparrow.nbt.adventure.NBTComponentSerializer;
import net.momirealms.sparrow.nbt.adventure.NBTSerializerOptions;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helper class for handling Adventure components and related functionalities.
 */
public class AdventureHelper {
    public static final String EMPTY_COMPONENT = componentToJson(Component.empty());
    private final MiniMessage miniMessage;
    private final MiniMessage miniMessageStrict;
    private final MiniMessage miniMessageCustom;
    private final GsonComponentSerializer gsonComponentSerializer;
    private final NBTComponentSerializer nbtComponentSerializer;
    private static final TextReplacementConfig REPLACE_LF = TextReplacementConfig.builder().matchLiteral("\n").replacement(Component.newline()).build();
    /**
     * This iterator slices a component into individual parts that
     * <ul>
     *     <li>Can be used individually without style loss</li>
     *     <li>Can be concatenated to form the original component, given that children are dropped</li>
     * </ul>
     * Any {@link net.kyori.adventure.text.ComponentIteratorFlag}s are ignored.
     */
    private static final ComponentIteratorType SLICER = (component, deque, flags) -> {
        final List<Component> children = component.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            deque.addFirst(children.get(i).applyFallbackStyle(component.style()));
        }
    };

    private AdventureHelper() {
        this.miniMessage = MiniMessage.builder().build();
        this.miniMessageStrict = MiniMessage.builder().strict(true).build();
        this.miniMessageCustom = MiniMessage.builder().tags(TagResolver.empty()).build();
        GsonComponentSerializer.Builder builder = GsonComponentSerializer.builder();
        if (!VersionHelper.isOrAbove1_20_5()) {
            builder.legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.get());
            builder.editOptions((b) -> b.value(JSONOptions.EMIT_HOVER_SHOW_ENTITY_ID_AS_INT_ARRAY, false));
        }
        if (!VersionHelper.isOrAbove1_21_5()) {
            builder.editOptions((b) -> {
                b.value(JSONOptions.EMIT_CLICK_EVENT_TYPE, JSONOptions.ClickEventValueMode.CAMEL_CASE);
                b.value(JSONOptions.EMIT_HOVER_EVENT_TYPE, JSONOptions.HoverEventValueMode.CAMEL_CASE);
                b.value(JSONOptions.EMIT_HOVER_SHOW_ENTITY_KEY_AS_TYPE_AND_UUID_AS_ID, true);
            });
        }
        this.gsonComponentSerializer = builder.build();
        this.nbtComponentSerializer = NBTComponentSerializer.builder()
                .editItem(item -> {
                    if (VersionHelper.isOrAbove1_20_5()) {
                    }
                })
                .editOptions((b) -> {
                    if (!VersionHelper.isOrAbove1_21_5()) {
                        b.value(NBTSerializerOptions.EMIT_CLICK_EVENT_TYPE, false);
                        b.value(NBTSerializerOptions.EMIT_HOVER_EVENT_TYPE, false);
                    }
                })
                .build();
    }

    private static class SingletonHolder {
        private static final AdventureHelper INSTANCE = new AdventureHelper();
    }

    /**
     * Retrieves the singleton instance of AdventureHelper.
     *
     * @return the singleton instance
     */
    public static AdventureHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Retrieves the MiniMessage instance.
     *
     * @return the MiniMessage instance
     */
    public static MiniMessage miniMessage() {
        return getInstance().miniMessage;
    }

    public static MiniMessage customMiniMessage() {
        return getInstance().miniMessageCustom;
    }

    public static MiniMessage strictMiniMessage() {
        return getInstance().miniMessageStrict;
    }

    /**
     * Retrieves the GsonComponentSerializer instance.
     *
     * @return the GsonComponentSerializer instance
     */
    public static GsonComponentSerializer getGson() {
        return getInstance().gsonComponentSerializer;
    }

    /**
     * Retrieves the NBTComponentSerializer instance.
     *
     * @return the NBTComponentSerializer instance
     */
    public static NBTComponentSerializer getNBT() {
        return getInstance().nbtComponentSerializer;
    }

    /**
     * Sends a message to an audience.
     *
     * @param audience the audience to send the message to
     * @param message  the message component
     */
    public static void sendMessage(Audience audience, Component message) {
        audience.sendMessage(message);
    }

    /**
     * Plays a sound for an audience.
     *
     * @param audience the audience to play the sound for
     * @param sound    the sound to play
     */
    public static void playSound(Audience audience, Sound sound) {
        audience.playSound(sound);
    }

    /**
     * Surrounds text with a MiniMessage font tag.
     *
     * @param text the text to surround
     * @param font the font as a {@link Key}
     * @return the text surrounded by the MiniMessage font tag
     */
    public static String surroundWithMiniMessageFont(String text, Key font) {
        return "<font:" + font.asString() + ">" + text + "</font>";
    }

    /**
     * Surrounds text with a MiniMessage font tag.
     *
     * @param text the text to surround
     * @param font the font as a {@link String}
     * @return the text surrounded by the MiniMessage font tag
     */
    public static String surroundWithMiniMessageFont(String text, String font) {
        return "<font:" + font + ">" + text + "</font>";
    }

    /**
     * Converts a JSON string to a MiniMessage string.
     *
     * @param json the JSON string
     * @return the MiniMessage string representation
     */
    public static String jsonToMiniMessage(String json) {
        return getInstance().miniMessageStrict.serialize(getInstance().gsonComponentSerializer.deserialize(json));
    }

    public static String componentToMiniMessage(Component component) {
        return getInstance().miniMessageStrict.serialize(component);
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

    public static List<Component> splitLines(Component component) {
        List<Component> result = new ArrayList<>(1);
        Component line = Component.empty();
        for (Iterator<Component> it = component.replaceText(REPLACE_LF).iterator(SLICER); it.hasNext(); ) {
            Component child = it.next().children(Collections.emptyList());
            if (child instanceof TextComponent text && text.content().equals(Component.newline().content())) {
                result.add(line.compact());
                line = Component.empty();
            } else {
                line = line.append(child);
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

    public static Component replaceText(Component text, Map<String, Component> replacements) {
        String patternString = replacements.keySet().stream()
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        return text.replaceText(builder ->
                builder.match(Pattern.compile(patternString))
                .replacement((result, b) -> replacements.get(result.group())));
    }
}
