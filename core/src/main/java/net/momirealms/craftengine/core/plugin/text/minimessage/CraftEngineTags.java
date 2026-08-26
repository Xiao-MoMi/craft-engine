package net.momirealms.craftengine.core.plugin.text.minimessage;

import net.momirealms.sparrow.message.tag.resolver.TagResolver;
import net.momirealms.sparrow.message.tag.standard.*;

public final class CraftEngineTags {
    public static final TagResolver[] INTERNAL = new TagResolver[] {
            ShiftTag.INSTANCE,
            ImageTag.INSTANCE,
            I18NTag.INSTANCE,
            L10NTag.INSTANCE,
            ExpressionTag.INSTANCE,
            GlobalVariableTag.INSTANCE,
            VariableTag.INSTANCE,
            RandomTag.INSTANCE,
            NamedArgumentTag.INSTANCE,
            ViewerNamedArgumentTag.INSTANCE,
            PlaceholderTag.INSTANCE,
            ViewerPlaceholderTag.INSTANCE,
            RelationalPlaceholderTag.INSTANCE,
            AttributeValueTag.ATTACKER,
            AttributeValueTag.VICTIM,
    };
    public static final TagResolver[] SPECIAL_STANDARD = new TagResolver[] {
            HoverTag.RESOLVER,
            ClickTag.RESOLVER,
            SequentialHeadTag.RESOLVER
    };
    public static final TagResolver[] STANDARD = new TagResolver[] {
            HoverTag.RESOLVER,
            ClickTag.RESOLVER,
            ColorTagResolver.INSTANCE,
            KeybindTag.RESOLVER,
            TranslatableTag.RESOLVER,
            TranslatableFallbackTag.RESOLVER,
            InsertionTag.RESOLVER,
            FontTag.RESOLVER,
            DecorationTag.RESOLVER,
            GradientTag.RESOLVER,
            RainbowTag.RESOLVER,
            ResetTag.RESOLVER,
            NewlineTag.RESOLVER,
            TransitionTag.RESOLVER,
            SelectorTag.RESOLVER,
            ScoreTag.RESOLVER,
            NbtTag.RESOLVER,
            ShadowColorTag.RESOLVER,
            SpriteTag.RESOLVER,
            SequentialHeadTag.RESOLVER
    };

    private CraftEngineTags() {
    }
}
