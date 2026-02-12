package net.momirealms.craftengine.core.font;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.util.CharacterUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public final class OffsetFont {
    private final net.momirealms.craftengine.core.util.Key font;
    private final Key fontKey;

    private final String NEG_16;
    private final String NEG_24;
    private final String NEG_32;
    private final String NEG_48;
    private final String NEG_64;
    private final String NEG_128;
    private final String NEG_256;

    private final String POS_16;
    private final String POS_24;
    private final String POS_32;
    private final String POS_48;
    private final String POS_64;
    private final String POS_128;
    private final String POS_256;

    private final String[] negativeOffsets = new String[16];
    private final String[] positiveOffsets = new String[16];

    private final Cache<Integer, String> fastLookup = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(256)
            .build();

    public net.momirealms.craftengine.core.util.Key font() {
        return font;
    }

    @SuppressWarnings("all")
    public OffsetFont(Section section) {
        font = net.momirealms.craftengine.core.util.Key.of(section.getString("font", "minecraft:default"));
        fontKey = Key.key(font.namespace(), font.value());
        NEG_16 = convertIfUnicode(section.getString("-16", ""));
        NEG_24 = convertIfUnicode(section.getString("-24", ""));
        NEG_32 = convertIfUnicode(section.getString("-32", ""));
        NEG_48 = convertIfUnicode(section.getString("-48", ""));
        NEG_64 = convertIfUnicode(section.getString("-64", ""));
        NEG_128 = convertIfUnicode(section.getString("-128", ""));
        NEG_256 = convertIfUnicode(section.getString("-256", ""));
        POS_16 = convertIfUnicode(section.getString("16", ""));
        POS_24 = convertIfUnicode(section.getString("24", ""));
        POS_32 = convertIfUnicode(section.getString("32", ""));
        POS_48 = convertIfUnicode(section.getString("48", ""));
        POS_64 = convertIfUnicode(section.getString("64", ""));
        POS_128 = convertIfUnicode(section.getString("128", ""));
        POS_256 = convertIfUnicode(section.getString("256", ""));

        for (int i = 1; i <= 15; i++) {
            negativeOffsets[i] = convertIfUnicode(section.getString("-" + i, ""));
            positiveOffsets[i] = convertIfUnicode(section.getString(String.valueOf(i), ""));
        }
    }

    public Component createOffset(int offset) {
        if (offset == 0) return Component.empty();
        return Component.text(Objects.requireNonNull(this.fastLookup.get(offset, k -> k > 0 ? createPos(k) : createNeg(-k)))).font(this.fontKey);
    }

    public String createOffset(int offset, BiFunction<String, String, String> tagDecorator) {
        if (offset == 0) return "";
        return tagDecorator.apply(this.fastLookup.get(offset, k -> k > 0 ? createPos(k) : createNeg(-k)), this.font.asString());
    }

    @SuppressWarnings("DuplicatedCode")
    private String createPos(int offset) {
        StringBuilder stringBuilder = new StringBuilder();
        while (offset >= 256) {
            stringBuilder.append(POS_256);
            offset -= 256;
        }
        if (offset >= 128) {
            stringBuilder.append(POS_128);
            offset -= 128;
        }
        if (offset >= 64) {
            stringBuilder.append(POS_64);
            offset -= 64;
        }
        if (offset >= 48) {
            stringBuilder.append(POS_48);
            offset -= 48;
        }
        if (offset >= 32) {
            stringBuilder.append(POS_32);
            offset -= 32;
        }
        if (offset >= 24) {
            stringBuilder.append(POS_24);
            offset -= 24;
        }
        if (offset >= 16) {
            stringBuilder.append(POS_16);
            offset -= 16;
        }
        if (offset == 0) return stringBuilder.toString();
        stringBuilder.append(positiveOffsets[offset]);
        return stringBuilder.toString();
    }

    @SuppressWarnings("DuplicatedCode")
    private String createNeg(int offset) {
        StringBuilder stringBuilder = new StringBuilder();
        while (offset >= 256) {
            stringBuilder.append(NEG_256);
            offset -= 256;
        }
        if (offset >= 128) {
            stringBuilder.append(NEG_128);
            offset -= 128;
        }
        if (offset >= 64) {
            stringBuilder.append(NEG_64);
            offset -= 64;
        }
        if (offset >= 48) {
            stringBuilder.append(NEG_48);
            offset -= 48;
        }
        if (offset >= 32) {
            stringBuilder.append(NEG_32);
            offset -= 32;
        }
        if (offset >= 24) {
            stringBuilder.append(NEG_24);
            offset -= 24;
        }
        if (offset >= 16) {
            stringBuilder.append(NEG_16);
            offset -= 16;
        }
        if (offset == 0) return stringBuilder.toString();
        stringBuilder.append(negativeOffsets[offset]);
        return stringBuilder.toString();
    }

    private String convertIfUnicode(String s) {
        if (s.startsWith("\\u")) {
            return new String(CharacterUtils.decodeUnicodeToChars(font.asString()));
        } else {
            return s;
        }
    }
}
