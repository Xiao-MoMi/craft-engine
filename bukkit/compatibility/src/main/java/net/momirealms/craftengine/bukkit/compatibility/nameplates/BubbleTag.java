package net.momirealms.craftengine.bukkit.compatibility.nameplates;

import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.customnameplates.api.CustomNameplatesAPI;
import net.momirealms.customnameplates.api.feature.bubble.Bubble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BubbleTag implements TagResolver {
    private final net.momirealms.craftengine.core.plugin.context.Context context;

    public BubbleTag(net.momirealms.craftengine.core.plugin.context.Context context) {
        this.context = context;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        String id = arguments.popOr("No bubble id provided").toString();
        Optional<Bubble> bubble = CustomNameplatesAPI.getInstance().getBubble(id);
        if (bubble.isEmpty()) {
            return null;
        }
        double left = arguments.popOr("No argument left provided").asDouble().orElseThrow(() -> ctx.newException("Invalid argument number", arguments));
        double right = arguments.popOr("No argument right provided").asDouble().orElseThrow(() -> ctx.newException("Invalid argument number", arguments));
        String content = arguments.popOr("No argument content provided").toString();
        String textWithImage = CustomNameplatesAPI.getInstance().createTextWithImage(AdventureHelper.strictMiniMessage().serialize(AdventureHelper.miniMessage().deserialize(content, this.context.tagResolvers())), bubble.get(), (float) left, (float) right);
        return Tag.selfClosingInserting(AdventureHelper.miniMessage().deserialize(textWithImage, this.context.tagResolvers()));
    }

    @Override
    public boolean has(@NotNull String name) {
        return "bubble".equals(name);
    }
}
