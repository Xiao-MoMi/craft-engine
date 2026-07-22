package net.momirealms.craftengine.core.item.processor;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.text.minimessage.FormattedLine;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.ListTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WrittenBookContentProcessor implements SimpleNetworkItemProcessor {
    public static final ItemProcessorFactory<WrittenBookContentProcessor> FACTORY = new Factory();
    private final FilterableText title;
    private final String author;
    private final int generation;
    private final boolean resolved;
    private final List<FilterableComponent> pages;

    public WrittenBookContentProcessor(FilterableText title, String author, int generation, boolean resolved, List<FilterableComponent> pages) {
        this.title = title;
        this.author = author;
        this.generation = generation;
        this.resolved = resolved;
        this.pages = pages;
    }

    @Override
    public Item apply(Item item, ItemBuildContext context) {
        CompoundTag bookTag = new CompoundTag();
        bookTag.put("title", this.title.toTag(context));
        bookTag.putString("author", this.author);
        bookTag.putInt("generation", this.generation);
        bookTag.putBoolean("resolved", this.resolved);
        ListTag pagesTag = new ListTag();
        for (FilterableComponent page : this.pages) {
            pagesTag.add(page.toTag(context));
        }
        bookTag.put("pages", pagesTag);
        item.setSparrowTagComponent(DataComponentKeys.WRITTEN_BOOK_CONTENT, bookTag);
        return item;
    }

    @Override
    public Key componentType(Item item, ItemBuildContext context) {
        return DataComponentKeys.WRITTEN_BOOK_CONTENT;
    }

    public record FilterableText(FormattedLine raw, @Nullable FormattedLine filtered) {

        static FilterableText fromConfig(ConfigValue value) {
            if (value == null) {
                return new FilterableText(FormattedLine.create(""), null);
            }
            if (value.is(Map.class)) {
                ConfigSection section = value.getAsSection();
                return new FilterableText(
                        FormattedLine.create(AdventureHelper.legacyToMiniMessage(section.getString("raw", ""))),
                        section.containsKey("filtered") ? FormattedLine.create(AdventureHelper.legacyToMiniMessage(section.getNonNullString("filtered"))) : null
                );
            }
            return new FilterableText(FormattedLine.create(AdventureHelper.legacyToMiniMessage(value.getAsString())), null);
        }

        CompoundTag toTag(ItemBuildContext context) {
            CompoundTag tag = new CompoundTag();
            tag.putString("raw", AdventureHelper.plainTextContent(this.raw.parse(context)));
            if (this.filtered != null) {
                tag.putString("filtered", AdventureHelper.plainTextContent(this.filtered.parse(context)));
            }
            return tag;
        }
    }

    public record FilterableComponent(List<FormattedLine> raw, @Nullable List<FormattedLine> filtered) {

        static FilterableComponent fromConfig(ConfigValue value) {
            if (value.is(Map.class)) {
                ConfigSection section = value.getAsSection();
                return new FilterableComponent(
                        parseLines(section.getValue("raw")),
                        section.containsKey("filtered") ? parseLines(section.getValue("filtered")) : null
                );
            }
            return new FilterableComponent(parseLines(value), null);
        }

        private static List<FormattedLine> parseLines(ConfigValue value) {
            if (value == null) return List.of();
            List<String> lines = value.is(List.class) ? value.getAsStringList() : List.of(value.getAsString());
            return lines.stream()
                    .map(AdventureHelper::legacyToMiniMessage)
                    .map(FormattedLine::create)
                    .toList();
        }

        CompoundTag toTag(ItemBuildContext context) {
            CompoundTag tag = new CompoundTag();
            tag.put("raw", AdventureHelper.componentToNbt(toComponent(this.raw, context)));
            if (this.filtered != null) {
                tag.put("filtered", AdventureHelper.componentToNbt(toComponent(this.filtered, context)));
            }
            return tag;
        }

        private static Component toComponent(List<FormattedLine> lines, ItemBuildContext context) {
            if (lines.size() == 1) {
                return lines.getFirst().parse(context);
            }
            List<Component> children = new ArrayList<>(lines.size() * 2);
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) children.add(Component.newline());
                children.add(lines.get(i).parse(context));
            }
            return Component.empty().children(children);
        }
    }

    private static class Factory implements ItemProcessorFactory<WrittenBookContentProcessor> {

        @Override
        public WrittenBookContentProcessor create(ConfigValue value) {
            ConfigSection section = value.getAsSection();
            FilterableText title = FilterableText.fromConfig(section.getValue("title"));
            String author = section.getString("author", "");
            int generation = section.getInt("generation", 0);
            boolean resolved = section.getBoolean("resolved", false);
            List<FilterableComponent> pages = new ArrayList<>();
            ConfigValue pagesValue = section.getValue("pages");
            if (pagesValue != null) {
                pagesValue.forEach(v -> pages.add(FilterableComponent.fromConfig(v)));
            }
            return new WrittenBookContentProcessor(title, author, generation, resolved, pages);
        }
    }
}
