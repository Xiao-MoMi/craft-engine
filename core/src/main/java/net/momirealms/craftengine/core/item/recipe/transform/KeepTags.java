package net.momirealms.craftengine.core.item.recipe.transform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.List;

public final class KeepTags implements ItemTransformDataProcessor {
    public static final ItemTransformDataProcessor.Factory<KeepTags> FACTORY = new Factory();
    private final List<String[]> tags;

    public KeepTags(List<String[]> tags) {
        this.tags = tags;
    }

    @Override
    public void accept(Item item1, Item item2, Item item3) {
        for (String[] tag : this.tags) {
            Object tagObj = item1.getTagAsJava((Object[]) tag);
            if (tagObj != null) {
                item3.setTag(tagObj, (Object[]) tag);
            }
        }
    }

    private static class Factory implements ItemTransformDataProcessor.Factory<KeepTags> {

        @Override
        public KeepTags create(ConfigSection section) {
            return new KeepTags(section.getNonEmptyList("tags", v -> v.getAsString().split("\\.")));
        }
    }
}