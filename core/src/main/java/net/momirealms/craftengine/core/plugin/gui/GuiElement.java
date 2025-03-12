/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.gui.category.ItemBrowserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface GuiElement {

    static GuiElement EMPTY = GuiElement.constant(null, (e, c) -> c.cancel());

    @Nullable
    Item<?> item();

    void handleClick(Click click);

    default void onTimer() {
    }

    static GuiElement dynamic(Function<DynamicGuiItemElement, Item<?>> itemSupplier, BiConsumer<DynamicGuiItemElement, Click> action) {
        return new DynamicGuiItemElement(itemSupplier, action);
    }

    static GuiElement paged(Function<PagedGuiElement, Item<?>> itemSupplier, boolean nextOrPrevious) {
        return new PagedGuiElement(itemSupplier, nextOrPrevious);
    }

    static GuiElement ordered(int order) {
        return new PageOrderedGuiElement(order);
    }

    static GuiElement constant(Item<?> item, BiConsumer<ConstantGuiElement, Click> action) {
        return new ConstantGuiElement(item, action);
    }

    static GuiElement recipeIngredient(List<Item<?>> ingredients, BiConsumer<RecipeIngredientGuiElement, Click> action) {
        return new RecipeIngredientGuiElement(ingredients, action);
    }

    abstract class AbstractGuiElement implements GuiElement {
        protected Gui gui;

        public void notifyItemUpdate() {
            gui().refresh();
        }

        public void setGui(Gui gui) {
            this.gui = gui;
        }

        public Gui gui() {
            return gui;
        }
    }

    class RecipeIngredientGuiElement extends AbstractGuiElement {
        private int ingredientIndex;
        private final List<Item<?>> ingredients;
        private final BiConsumer<RecipeIngredientGuiElement, Click> action;

        public RecipeIngredientGuiElement(List<Item<?>> ingredients, BiConsumer<RecipeIngredientGuiElement, Click> action) {
            this.ingredients = ingredients;
            this.ingredientIndex = 0;
            this.action = action;
        }

        @Override
        public @NotNull Item<?> item() {
            return this.ingredients.get(this.ingredientIndex);
        }

        @Override
        public void handleClick(Click click) {
            this.action.accept(this, click);
        }

        @Override
        public void onTimer() {
            int previous = this.ingredientIndex;
            increaseIndex();
            if (previous != ingredientIndex) {
                notifyItemUpdate();
            }
        }

        public void increaseIndex() {
            this.ingredientIndex++;
            if (this.ingredientIndex >= this.ingredients.size()) {
                this.ingredientIndex = 0;
            }
        }
    }

    class ConstantGuiElement extends AbstractGuiElement {
        private final BiConsumer<ConstantGuiElement, Click> action;
        private final Item<?> item;

        public ConstantGuiElement(Item<?> item, BiConsumer<ConstantGuiElement, Click> action) {
            this.item = item;
            this.action = action;
        }

        @Override
        public Item<?> item() {
            return item;
        }

        @Override
        public void handleClick(Click click) {
            this.action.accept(this, click);
        }
    }

    class PageOrderedGuiElement extends AbstractGuiElement {
        private final int index;

        public PageOrderedGuiElement(int index) {
            this.index = index;
        }

        @Override
        public PagedGui gui() {
            return (PagedGui) super.gui();
        }

        @Override
        public Item<?> item() {
            return gui().itemAt(index).item();
        }

        @Override
        public void handleClick(Click click) {
            this.gui().itemAt(order()).action().accept(this, click);
        }

        public int order() {
            return index;
        }
    }

    class PagedGuiElement extends AbstractGuiElement {
        private final Function<PagedGuiElement, Item<?>> itemSupplier;
        private final boolean nextOrPrevious;

        public PagedGuiElement(Function<PagedGuiElement, Item<?>> itemSupplier, boolean nextOrPrevious) {
            this.itemSupplier = itemSupplier;
            this.nextOrPrevious = nextOrPrevious;
        }

        @Override
        public Item<?> item() {
            return itemSupplier.apply(this);
        }

        @Override
        public void handleClick(Click click) {
            click.cancel();
            PagedGui pagedGui = gui();
            boolean changed = false;
            if (this.nextOrPrevious) {
                if (pagedGui.hasNextPage()) {
                    pagedGui.goNextPage();
                    changed = true;
                }
            } else {
                if (pagedGui.hasPreviousPage()) {
                    pagedGui.goPreviousPage();
                    changed = true;
                }
            }
            if (changed) {
                click.clicker().playSound(ItemBrowserManager.Constants.SOUND_CHANGE_PAGE, 0.25f, 1);
                notifyItemUpdate();
            }
        }

        @Override
        public PagedGui gui() {
            return (PagedGui) super.gui();
        }
    }

    class DynamicGuiItemElement extends AbstractGuiElement {
        private final BiConsumer<DynamicGuiItemElement, Click> action;
        private final Function<DynamicGuiItemElement, Item<?>> itemSupplier;

        public DynamicGuiItemElement(Function<DynamicGuiItemElement, Item<?>> itemSupplier, BiConsumer<DynamicGuiItemElement, Click> action) {
            this.itemSupplier = itemSupplier;
            this.action = action;
        }

        @Override
        public Item<?> item() {
            return itemSupplier.apply(this);
        }

        @Override
        public void handleClick(Click click) {
            this.action.accept(this, click);
        }
    }
}
