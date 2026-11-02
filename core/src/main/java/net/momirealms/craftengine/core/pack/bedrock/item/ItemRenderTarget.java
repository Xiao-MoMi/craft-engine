package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.bedrock.model.JavaModel;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResolver;
import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public sealed interface ItemRenderTarget {

    default boolean hasPlayerHead() {
        return this instanceof Model model && model.headSkin() != null
                || this instanceof Composite composite && composite.parts().stream().anyMatch(part -> part.target().hasPlayerHead());
    }

    record Model(Key model, List<Tint> tints, @Nullable Key headSkin) implements ItemRenderTarget {

        public Model(Key model, List<Tint> tints) {
            this(model, tints, null);
        }

        public Model {
            tints = List.copyOf(tints);
        }

        public JavaModel resolve(JavaModelResolver resolver) throws IOException {
            return this.headSkin == null ? resolver.resolve(this.model) : resolver.resolvePlayerHead(this.model, this.headSkin);
        }
    }

    record Special(Key base, SpecialModel model) implements ItemRenderTarget {
    }

    record Composite(List<ItemBindingRule> parts) implements ItemRenderTarget {

        public Composite {
            parts = List.copyOf(parts);
        }
    }

    enum Builtin implements ItemRenderTarget {
        EMPTY,
        MISSING,
        BUNDLE_SELECTED_ITEM
    }
}
