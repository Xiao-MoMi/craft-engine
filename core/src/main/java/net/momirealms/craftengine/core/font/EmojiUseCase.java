package net.momirealms.craftengine.core.font;

public final class EmojiUseCase {
    public static final EmojiUseCase CHAT = new EmojiUseCase("chat");
    public static final EmojiUseCase ANVIL = new EmojiUseCase("anvil");
    public static final EmojiUseCase SIGN = new EmojiUseCase("sign");
    public static final EmojiUseCase BOOK = new EmojiUseCase("book");
    public static final EmojiUseCase COMMAND = new EmojiUseCase("command");
    private final String id;

    public EmojiUseCase(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }
}
