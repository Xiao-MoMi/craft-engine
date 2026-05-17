package net.momirealms.craftengine.core.font;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class Emoji {
    private final String content;
    private final String permission;
    private final String image;
    private final List<String> keywords;
    private final boolean chatCompletion;
    private final Map<String, String> contentOverrides;

    public Emoji(String content, String permission, String image, List<String> keywords, boolean chatCompletion, Map<String, String> contentOverrides) {
        this.content = content;
        this.image = image;
        this.permission = permission;
        this.keywords = keywords;
        this.chatCompletion = chatCompletion;
        this.contentOverrides = contentOverrides;
    }

    public String content(EmojiUseCase useCase) {
        if (useCase == null) return this.content;
        return this.contentOverrides.getOrDefault(useCase.id(), this.content);
    }

    @Nullable
    public String emojiImage() {
        return image;
    }

    @Nullable
    public String permission() {
        return permission;
    }

    public List<String> keywords() {
        return keywords;
    }

    public boolean chatCompletion() {
        return chatCompletion;
    }
}
