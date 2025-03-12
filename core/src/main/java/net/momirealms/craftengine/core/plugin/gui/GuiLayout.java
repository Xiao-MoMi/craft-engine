package net.momirealms.craftengine.core.plugin.gui;

import java.util.*;

public class GuiLayout {
    private final Map<Character, Ingredient> elements = new HashMap<>();
    private final int width;
    private final int height;
    private final char[][] layout;

    public GuiLayout(char[][] layout) {
        this.elements.put(' ', Ingredient.EMPTY);
        this.layout = layout;
        this.height = layout.length;
        if (height == 0) {
            throw new IllegalArgumentException("Layout must have at least one element");
        } else {
            this.width = layout[0].length;
            if (this.width == 0) {
                throw new IllegalArgumentException("Layout must have at least one element");
            }
        }
    }

    public GuiLayout(String... layout) {
        this(createLayout(layout));
    }

    private static char[][] createLayout(String... layout) {
        List<String> rows = new ArrayList<>();
        Collections.addAll(rows, layout);
        int height = rows.size();
        if (height == 0) {
            return new char[0][0];
        }
        int width = layout[0].length();
        char[][] array = new char[height][width];
        for (int i = 0; i < height; i++) {
            String row = rows.get(i);
            if (row.length() != width) {
                throw new IllegalArgumentException("All rows must have the same length as the first row (" + width + ")");
            }
            array[i] = row.toCharArray();
        }
        return array;
    }

    public int height() {
        return height;
    }

    public int width() {
        return width;
    }

    public GuiLayout addIngredient(char c, GuiElement supplier) {
        this.elements.put(c, Ingredient.simple(supplier));
        return this;
    }

    public GuiLayout addIngredient(char c, Ingredient ingredient) {
        this.elements.put(c, ingredient);
        return this;
    }

    public GuiElement[] createElements(Gui gui) {
        List<GuiElement> elementsList = new ArrayList<>();
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                char c = this.layout[i][j];
                Ingredient ingredient = this.elements.get(c);
                if (ingredient == null) {
                    throw new IllegalStateException("No ingredient registered for character: " + c);
                }
                GuiElement.AbstractGuiElement element = (GuiElement.AbstractGuiElement) ingredient.element(gui);
                element.setGui(gui);
                elementsList.add(element);
            }
        }
        GuiElement[] result = new GuiElement[elementsList.size()];
        return elementsList.toArray(result);
    }
}