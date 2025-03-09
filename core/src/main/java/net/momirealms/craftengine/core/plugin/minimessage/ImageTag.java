package net.momirealms.craftengine.core.plugin.minimessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.font.BitmapImage;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ImageTag implements TagResolver {
    public static final ImageTag INSTANCE = new ImageTag();

    public static ImageTag instance() {
        return INSTANCE;
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }
        String namespace = arguments.popOr("No argument namespace provided").toString();
        String id = arguments.popOr("No argument id provided").toString();
        Optional<BitmapImage> optional = CraftEngine.instance().imageManager().bitmapImageByImageId(Key.of(namespace, id));
        if (optional.isPresent()) {
            if (arguments.hasNext()) {
                int row = arguments.popOr("No argument row provided").asInt().orElseThrow(() -> ctx.newException("Invalid argument number", arguments));
                int column = arguments.popOr("No argument column provided").asInt().orElseThrow(() -> ctx.newException("Invalid argument number", arguments));
                return Tag.inserting(Component.empty().children(List.of(optional.get().componentAt(row,column))));
            } else {
                return Tag.inserting(Component.empty().children(List.of(optional.get().componentAt(0,0))));
            }
        } else {
            throw ctx.newException("Invalid image id", arguments);
        }
    }

    @Override
    public boolean has(@NotNull String name) {
        return "image".equals(name);
    }
}
