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

package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.util.AdventureHelper;

public class DisplayNameModifier<I> implements ItemModifier<I> {
    private final String argument;

    public DisplayNameModifier(String argument) {
        this.argument = ConfigManager.nonItalic() ? "<!i>" + argument : argument;
    }

    @Override
    public String name() {
        return "display-name";
    }

    @Override
    public void apply(Item<I> item, ItemBuildContext context) {
        item.displayName(AdventureHelper.componentToJson(AdventureHelper.miniMessage().deserialize(this.argument, context.tagResolvers())));
    }
}
