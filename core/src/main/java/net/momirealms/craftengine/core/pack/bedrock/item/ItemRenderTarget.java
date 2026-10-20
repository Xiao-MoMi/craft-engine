package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.definition.tint.Tint;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public sealed interface ItemRenderTarget {

    record Model(Key model, List<Tint> tints) implements ItemRenderTarget {

        public Model {
            tints = List.copyOf(tints);
        }
    }

    record Special(Key base, SpecialModel model) implements ItemRenderTarget {
    }

    enum Builtin implements ItemRenderTarget {
        EMPTY,
        MISSING,
        BUNDLE_SELECTED_ITEM
    }
}
