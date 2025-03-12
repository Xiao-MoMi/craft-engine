package net.momirealms.craftengine.core.plugin.gui;

public interface Ingredient {
    Ingredient EMPTY = gui -> GuiElement.constant(null, (e, c) -> c.cancel());

    GuiElement element(Gui gui);

    static Ingredient simple(GuiElement element) {
        return new SimpleIngredient(element);
    }

    static Ingredient paged() {
        return new PagedIngredient();
    }

    class SimpleIngredient implements Ingredient {
        private final GuiElement element;

        public SimpleIngredient(GuiElement element) {
            this.element = element;
        }

        @Override
        public GuiElement element(Gui gui) {
            return element;
        }
    }

    class PagedIngredient implements Ingredient {
        private int order = 0;

        public PagedIngredient() {}

        @Override
        public GuiElement element(Gui gui) {
            return GuiElement.ordered(order++);
        }
    }
}
