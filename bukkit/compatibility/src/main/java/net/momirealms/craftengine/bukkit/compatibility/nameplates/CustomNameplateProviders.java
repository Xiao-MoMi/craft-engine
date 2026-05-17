package net.momirealms.craftengine.bukkit.compatibility.nameplates;

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.momirealms.craftengine.core.plugin.compatibility.TagResolverProvider;
import net.momirealms.craftengine.core.plugin.context.Context;

public final class CustomNameplateProviders {

    public static class Background implements TagResolverProvider {
        @Override
        public String name() {
            return "background";
        }

        @Override
        public TagResolver getTagResolver(Context context) {
            return new BackgroundTag(context);
        }
    }

    public static class Nameplate implements TagResolverProvider {
        @Override
        public String name() {
            return "nameplate";
        }

        @Override
        public TagResolver getTagResolver(Context context) {
            return new NameplateTag(context);
        }
    }

    public static class Bubble implements TagResolverProvider {
        @Override
        public String name() {
            return "bubble";
        }

        @Override
        public TagResolver getTagResolver(Context context) {
            return new BubbleTag(context);
        }
    }
}
