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

import java.util.function.Consumer;

public class BasicGuiImpl extends AbstractGui implements BasicGui {

    public BasicGuiImpl(GuiLayout layout, Consumer<Click> inventoryClickConsumer) {
        super(layout, inventoryClickConsumer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GuiLayout layout;
        private Consumer<Click> inventoryClickConsumer = Click::cancel;

        public Builder() {
        }

        public Builder layout(GuiLayout layout) {
            this.layout = layout;
            return this;
        }

        public Builder inventoryClickConsumer(Consumer<Click> inventoryClickConsumer) {
            this.inventoryClickConsumer = inventoryClickConsumer;
            return this;
        }

        public BasicGui build() {
            return new BasicGuiImpl(layout, inventoryClickConsumer);
        }
    }
}
