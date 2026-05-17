package net.momirealms.craftengine.core.item.recipe.transform;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

import java.util.List;

public final class KeepCustomData implements ItemTransformDataProcessor {
    public static final ItemTransformDataProcessor.Factory<KeepCustomData> FACTORY = new Factory();
    private final List<String[]> paths;

    public KeepCustomData(List<String[]> data) {
        this.paths = data;
    }

    @Override
    public void accept(Item item1, Item item2, Item item3) {
        for (String[] path : this.paths) {
            Object dataObj = item1.getTagAsJava((Object[]) path);
            if (dataObj != null) {
                item3.setTag(dataObj, (Object[]) path);
            }
        }
    }

    private static class Factory implements ItemTransformDataProcessor.Factory<KeepCustomData> {
        private static final String[] TAGS = new String[]{"tags", "paths"};

        @Override
        public KeepCustomData create(ConfigSection section) {
            return new KeepCustomData(section.getNonEmptyList(TAGS, v -> v.getAsString().split("\\.")));
        }
    }
}
